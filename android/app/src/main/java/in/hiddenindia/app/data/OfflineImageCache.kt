package `in`.hiddenindia.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Manages caching and offline persistence of destination imagery (cover images,
 * places of interest, authentic local food dishes, and local experiences).
 *
 * When an offline destination bundle is downloaded via toggleDownloadPack, all images
 * for that destination are fetched via OkHttpClient with a Wikimedia-compliant User-Agent
 * and persisted to the app's internal storage (`context.filesDir/offline_images/`).
 *
 * The Coil ImageLoader interceptor (see OfflineImageInterceptor) and MapViewer's WebViewClient
 * automatically detect and serve these offline files without touching the network, ensuring
 * that the app looks rich and fully visual even in zero-network simulation / offline mode.
 */
object OfflineImageCache {

    private const val CONCURRENCY = 3
    private const val BATCH_DELAY_MS = 80L

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    fun imageDir(context: Context): File =
        File(context.filesDir, "offline_images").apply { if (!exists()) mkdirs() }

    private fun urlToFilename(url: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(url.toByteArray(Charsets.UTF_8))
        val hex = digest.joinToString("") { "%02x".format(it) }
        val ext = url.substringBefore("?").substringAfterLast(".", "").lowercase()
        val safeExt = if (ext in listOf("jpg", "jpeg", "png", "webp", "gif", "svg")) ".$ext" else ".jpg"
        return "$hex$safeExt"
    }

    fun getImageFile(context: Context, url: String): File =
        File(imageDir(context), urlToFilename(url))

    fun isImageCached(context: Context, url: String): Boolean {
        if (url.isBlank() || (!url.startsWith("http://") && !url.startsWith("https://"))) return false
        val file = getImageFile(context, url)
        return file.exists() && file.length() > 0
    }

    /**
     * Extracts all unique image URLs belonging to a destination (cover, places, foods, experiences).
     */
    fun getAllImageUrlsForDestination(destination: Destination): List<String> {
        val urls = linkedSetOf<String>()
        if (destination.coverImage.isNotBlank() && destination.coverImage.startsWith("http")) {
            urls.add(destination.coverImage)
        }
        destination.thumbnailUrl?.takeIf { it.isNotBlank() && it.startsWith("http") }?.let { urls.add(it) }

        destination.places.forEach { place ->
            if (place.image.isNotBlank() && place.image.startsWith("http")) {
                urls.add(place.image)
            }
            place.thumbnailUrl?.takeIf { it.isNotBlank() && it.startsWith("http") }?.let { urls.add(it) }
        }
        destination.localFoods.forEach { food ->
            val img = food.image
            if (!img.isNullOrBlank() && img.startsWith("http")) {
                urls.add(img)
            }
            food.thumbnailUrl?.takeIf { it.isNotBlank() && it.startsWith("http") }?.let { urls.add(it) }
        }
        destination.localExperiences.forEach { exp ->
            val img = exp.image
            if (!img.isNullOrBlank() && img.startsWith("http")) {
                urls.add(img)
            }
            exp.thumbnailUrl?.takeIf { it.isNotBlank() && it.startsWith("http") }?.let { urls.add(it) }
        }
        return urls.toList()
    }

    /**
     * Downloads an individual image to disk atomically via a temporary file.
     */
    suspend fun fetchAndCacheImage(context: Context, url: String): Boolean = withContext(Dispatchers.IO) {
        if (url.isBlank() || (!url.startsWith("http://") && !url.startsWith("https://"))) return@withContext false
        val dest = getImageFile(context, url)
        if (dest.exists() && dest.length() > 0) return@withContext true

        val temp = File(dest.parentFile, "${dest.name}.${System.currentTimeMillis()}.tmp")
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "HiddenIndiaAndroid/1.0 (https://hiddenindia.in; contact@hiddenindia.in)")
                .header("Accept", "image/webp,image/png,image/jpeg,image/*;q=0.8")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext false
                val body = response.body ?: return@withContext false
                body.byteStream().use { input ->
                    temp.outputStream().use { output -> input.copyTo(output) }
                }
            }

            if (temp.exists() && temp.length() > 0) {
                if (dest.exists()) dest.delete()
                val renamed = temp.renameTo(dest)
                if (!renamed) {
                    temp.copyTo(dest, overwrite = true)
                    temp.delete()
                }
                true
            } else {
                temp.delete()
                false
            }
        } catch (_: Exception) {
            try { temp.delete() } catch (_: Exception) { }
            false
        }
    }

    /** Record IDs of this destination's items that carry no stored image URL. */
    private fun recordIdsWithoutStoredImage(destination: Destination): List<String> {
        val ids = mutableListOf<String>()
        if (destination.coverImage.isBlank()) ids.add(destination.id)
        destination.places.forEach { if (it.image.isBlank()) ids.add(it.id) }
        destination.localFoods.forEach { f -> val id = f.id; if (f.image.isNullOrBlank() && !id.isNullOrBlank()) ids.add(id) }
        destination.localExperiences.forEach { if (it.image.isNullOrBlank()) ids.add(it.id) }
        return ids
    }

    /**
     * Stored image URLs plus, for any record without one, the verified URL from the image
     * database looked up by the EXACT record ID (null = no verified image = nothing to download).
     */
    fun getAllImageUrlsForDestination(context: Context, destination: Destination): List<String> {
        val urls = getAllImageUrlsForDestination(destination).toMutableList()
        for (id in recordIdsWithoutStoredImage(destination)) {
            val url = ImageRepository.imageUrlFor(context, id)
            if (!url.isNullOrBlank() && url.startsWith("http") && !urls.contains(url)) urls.add(url)
        }
        return urls
    }

    /**
     * Downloads all verified images for a destination, reporting progress. Only URLs from the
     * image database are downloaded; records without a verified image have none.
     */
    suspend fun downloadImagesForDestination(
        context: Context,
        destination: Destination,
        onProgress: ((done: Int, total: Int) -> Unit)? = null
    ): Pair<Int, Int> = withContext(Dispatchers.IO) {
        val urls = getAllImageUrlsForDestination(context, destination)

        var succeeded = 0
        var failed = 0
        var done = 0

        urls.chunked(CONCURRENCY).forEach { batch ->
            val results = batch.map { url -> async { fetchAndCacheImage(context, url) } }.awaitAll()
            results.forEach { if (it) succeeded++ else failed++ }
            done += batch.size
            onProgress?.invoke(done, urls.size)
            if (done < urls.size) delay(BATCH_DELAY_MS)
        }

        succeeded to failed
    }

    /**
     * Deletes cached images for this destination that are not in use by other downloaded destinations.
     */
    fun deleteCachedImagesForDestination(
        context: Context,
        destination: Destination,
        remainingDownloadedDestinations: List<Destination>
    ) {
        val thisUrls = getAllImageUrlsForDestination(context, destination).toSet()
        val otherUrls = remainingDownloadedDestinations.flatMap { getAllImageUrlsForDestination(context, it) }.toSet()

        for (url in thisUrls) {
            if (!otherUrls.contains(url)) {
                try {
                    val file = getImageFile(context, url)
                    if (file.exists()) file.delete()
                } catch (_: Exception) { }
            }
        }
    }

    /**
     * Calculates the total storage size of all cached offline images in bytes.
     */
    fun getTotalCachedImagesSize(context: Context): Long {
        val dir = imageDir(context)
        return dir.listFiles()?.filter { it.isFile && !it.name.endsWith(".tmp") }?.sumOf { it.length() } ?: 0L
    }
}
