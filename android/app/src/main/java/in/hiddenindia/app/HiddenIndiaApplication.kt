package `in`.hiddenindia.app

import android.app.Application
import coil.Coil
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import coil.disk.DiskCache
import coil.EventListener
import coil.network.HttpException
import coil.request.ErrorResult
import coil.request.ImageRequest
import `in`.hiddenindia.app.data.BrokenImageInterceptor
import `in`.hiddenindia.app.data.ImageRepository
import `in`.hiddenindia.app.data.OfflineImageInterceptor
import okhttp3.OkHttpClient

/**
 * Registers coil-svg's decoder app-wide so AsyncImage/UserAvatar can render the
 * AVATAR_PRESETS inline SVGs (ProfileScreen) the same way the web app's <img src="data:
 * image/svg+xml..."> does, configures a compliant User-Agent required by Wikimedia's
 * User-Agent robot policy (https://w.wiki/4wJS) to prevent HTTP 403 Forbidden errors,
 * and attaches OfflineImageInterceptor to seamlessly serve cached images when offline.
 */
class HiddenIndiaApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        Coil.setImageLoader(this)
    }

    override fun newImageLoader(): ImageLoader {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val requestWithHeaders = originalRequest.newBuilder()
                    .header("User-Agent", "HiddenIndiaAndroid/1.0 (https://hiddenindia.in; contact@hiddenindia.in) Coil/2.7.0")
                    .header("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                    .build()
                chain.proceed(requestWithHeaders)
            }
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .crossfade(true)
            .respectCacheHeaders(false) // aggressively cache Wikimedia thumbnails to avoid 429 rate limits
            .memoryCache {
                coil.memory.MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("coil_disk_cache"))
                    .maxSizeBytes(250L * 1024 * 1024) // 250 MB
                    .build()
            }
            .eventListener(object : EventListener {
                override fun onError(request: ImageRequest, result: ErrorResult) {
                    val url = request.data as? String ?: return
                    // Log the failing record ID(s); an HTTP error is permanent for this session.
                    // (Offline/network errors are NOT marked, so images load again when back online.)
                    ImageRepository.reportLoadFailure(this@HiddenIndiaApplication, url)
                    if (result.throwable is HttpException) ImageRepository.markBroken(url)
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
