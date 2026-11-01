package `in`.hiddenindia.app

import android.app.Application
import coil.Coil
import coil.EventListener
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.network.HttpException
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import `in`.hiddenindia.app.data.BrokenImageInterceptor
import `in`.hiddenindia.app.data.ImageDiagnostics
import `in`.hiddenindia.app.data.ImageRepository
import `in`.hiddenindia.app.data.OfflineImageInterceptor
import okhttp3.Dispatcher
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Global application class configuring Coil 2.7.0 image loading pipeline for Hidden India:
 * - 250 MB persistent disk cache + memory cache with stable original URL keys
 * - Backend image proxy interceptor with seamless Wikimedia direct fallback
 * - Transient failure retry with exponential backoff (handles 429 rate limits, 5xx server issues, timeouts)
 * - Offline image interception for downloaded packs
 * - Real-time non-intrusive diagnostics tracking
 */
class HiddenIndiaApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        Coil.setImageLoader(this)
    }

    override fun newImageLoader(): ImageLoader {
        val dispatcher = Dispatcher().apply {
            maxRequests = 32
            maxRequestsPerHost = 10
        }

        val okHttpClient = OkHttpClient.Builder()
            .dispatcher(dispatcher)
            // 1. Wikimedia-compliant User-Agent and image format negotiation
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val requestWithHeaders = originalRequest.newBuilder()
                    .header("User-Agent", "HiddenIndiaAndroid/2.0 (https://hiddenindia.in; contact@hiddenindia.in) Coil/2.7.0")
                    .header("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                    .build()
                chain.proceed(requestWithHeaders)
            }
            // 2. Intelligent Backend Image Proxy Interceptor with direct fallback
            .addInterceptor(ImageProxyInterceptor(BuildConfig.API_BASE_URL))
            // 3. Transient error retry interceptor (backoff on 429/timeouts)
            .addInterceptor(TransientRetryInterceptor())
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .crossfade(true)
            .respectCacheHeaders(false) // aggressively cache Wikimedia thumbnails on disk to avoid re-downloading
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("coil_disk_cache"))
                    .maxSizeBytes(250L * 1024 * 1024) // 250 MB persistent cache
                    .build()
            }
            .eventListener(object : EventListener {
                override fun onSuccess(request: ImageRequest, result: SuccessResult) {
                    val url = request.data as? String ?: return
                    ImageDiagnostics.recordSuccess(url, result.dataSource)
                }

                override fun onError(request: ImageRequest, result: ErrorResult) {
                    val url = request.data as? String ?: return
                    val throwable = result.throwable
                    val statusCode = if (throwable is HttpException) throwable.response.code else -1
                    val errorMsg = throwable.message

                    // Record failure in diagnostics and classify permanency
                    ImageRepository.markFailure(url, statusCode, errorMsg)
                    ImageRepository.reportLoadFailure(this@HiddenIndiaApplication, url)
                }
            })
            .components {
                add(BrokenImageInterceptor())
                add(OfflineImageInterceptor(this@HiddenIndiaApplication))
                add(SvgDecoder.Factory())
            }
            .build()
    }
}

/**
 * Routes eligible Wikimedia and Unsplash image requests through the deployed backend image proxy.
 * Keeps the original image URL as the stable Coil cache key. If the proxy is sleeping or unavailable,
 * falls back seamlessly to direct Wikimedia download.
 */
class ImageProxyInterceptor(private val baseUrl: String) : Interceptor {
    companion object {
        private val ALLOWED_PROXY_HOSTS = setOf("upload.wikimedia.org", "images.unsplash.com")
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val originalUrl = originalRequest.url.toString()
        val host = originalRequest.url.host

        // Only proxy supported image hosts and prevent double-proxying
        if (host in ALLOWED_PROXY_HOSTS && !originalUrl.contains("/api/image-proxy") && baseUrl.isNotBlank()) {
            val encodedTarget = try {
                URLEncoder.encode(originalUrl, "UTF-8")
            } catch (e: Exception) {
                null
            }

            if (encodedTarget != null) {
                val proxyUrl = "${baseUrl.trimEnd('/')}/api/image-proxy?url=$encodedTarget"
                val proxyRequest = originalRequest.newBuilder()
                    .url(proxyUrl)
                    .header("X-Target-Image-Host", host)
                    .build()

                try {
                    val proxyResponse = chain.proceed(proxyRequest)
                    if (proxyResponse.isSuccessful) {
                        ImageDiagnostics.recordProxySuccess(originalUrl)
                        return proxyResponse
                    } else {
                        ImageDiagnostics.recordProxyFallback(originalUrl, proxyResponse.code, "Proxy returned ${proxyResponse.code}")
                        proxyResponse.close()
                    }
                } catch (e: Exception) {
                    ImageDiagnostics.recordProxyFallback(originalUrl, -1, e.message)
                }
            }
        }

        // Direct fetch fallback
        return chain.proceed(originalRequest)
    }
}

/**
 * Handles transient network failures (timeouts, 429 rate limiting, 5xx gateway errors)
 * using controlled exponential backoff without infinite loops.
 */
class TransientRetryInterceptor : Interceptor {
    companion object {
        private const val MAX_RETRIES = 2
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response: Response? = null
        var lastException: IOException? = null
        val url = request.url.toString()

        for (attempt in 0..MAX_RETRIES) {
            try {
                if (response != null) {
                    response.close()
                }
                response = chain.proceed(request)

                // If rate limited (429) or temporary gateway error (502/503/504), back off and retry
                if ((response.code == 429 || response.code in 502..504) && attempt < MAX_RETRIES) {
                    val backoffMs = 500L * (attempt + 1)
                    Thread.sleep(backoffMs)
                    ImageDiagnostics.recordRetry(url, attempt + 1, false)
                    continue
                }

                if (attempt > 0 && response.isSuccessful) {
                    ImageDiagnostics.recordRetry(url, attempt, true)
                }
                return response
            } catch (e: SocketTimeoutException) {
                lastException = e
                if (attempt < MAX_RETRIES) {
                    Thread.sleep(400L * (attempt + 1))
                    ImageDiagnostics.recordRetry(url, attempt + 1, false)
                    continue
                }
            } catch (e: IOException) {
                lastException = e
                if (attempt < MAX_RETRIES && !chain.call().isCanceled()) {
                    Thread.sleep(400L * (attempt + 1))
                    ImageDiagnostics.recordRetry(url, attempt + 1, false)
                    continue
                }
            }
        }

        if (response != null) return response
        throw lastException ?: IOException("Failed to load image after $MAX_RETRIES retries: $url")
    }
}
