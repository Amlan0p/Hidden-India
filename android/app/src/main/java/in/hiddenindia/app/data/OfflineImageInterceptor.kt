package `in`.hiddenindia.app.data

import android.content.Context
import coil.intercept.Interceptor
import coil.request.ImageResult

/**
 * Coil interceptor that checks if an image URL has been downloaded as part of
 * an offline destination bundle (via OfflineImageCache).
 *
 * If the image is present in offline storage, this intercepts the request and points
 * it directly to the local File, bypassing network calls completely. This allows all
 * Coil AsyncImage instances across the entire application to seamlessly render offline
 * images without any screen-level code modification.
 */
class OfflineImageInterceptor(private val context: Context) : Interceptor {

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val request = chain.request
        val data = request.data

        if (data is String && (data.startsWith("http://") || data.startsWith("https://"))) {
            val cachedFile = OfflineImageCache.getImageFile(context, data)
            if (cachedFile.exists() && cachedFile.length() > 0) {
                val offlineRequest = request.newBuilder()
                    .data(cachedFile)
                    .build()
                return chain.proceed(offlineRequest)
            }
        }

        return chain.proceed(request)
    }
}
