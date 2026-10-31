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
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.tan

/**
 * Port of web src/services/offlineTiles.ts. Before this, "Download Offline Bundle" on
 * Android only flipped a boolean flag (MainViewModel.toggleDownloadPack /
 * DestinationRepository) — no map tiles were ever actually fetched or cached, despite
 * OfflineVaultScreen's copy claiming "cached map tiles will load" in offline mode. This
 * downloads the real OSM raster tiles covering a destination's small osmBoundingBox
 * (~11km box, same zoom range as web) to local file storage, and MapViewer's WebView
 * serves them back via shouldInterceptRequest when offline — see MapViewer.kt.
 *
 * Same courtesy limits as the web version: small area, modest zoom range, low
 * concurrency, brief pauses between batches — see
 * https://operations.osmfoundation.org/policies/tiles/
 */
object OfflineTileCache {

    const val MIN_TILE_ZOOM = 11
    const val MAX_TILE_ZOOM = 14
    private const val CONCURRENCY = 4
    private const val BATCH_DELAY_MS = 100L
    private const val ESTIMATED_BYTES_PER_TILE = 18_000L

    private val subdomains = listOf("a", "b", "c")
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    data class TileCoord(val z: Int, val x: Int, val y: Int)

    private fun lonToTileX(lon: Double, z: Int): Int =
        Math.floor((lon + 180.0) / 360.0 * 2.0.pow(z)).toInt()

    private fun latToTileY(lat: Double, z: Int): Int {
        val latRad = Math.toRadians(lat)
        return Math.floor((1.0 - ln(tan(latRad) + 1.0 / Math.cos(latRad)) / Math.PI) / 2.0 * 2.0.pow(z)).toInt()
    }

    /** All tiles needed to cover a [minLat, minLng, maxLat, maxLng] box across a zoom range. */
    fun computeTileList(bbox: List<Double>, minZoom: Int = MIN_TILE_ZOOM, maxZoom: Int = MAX_TILE_ZOOM): List<TileCoord> {
        if (bbox.size < 4) return emptyList()
        val (minLat, minLng, maxLat, maxLng) = listOf(bbox[0], bbox[1], bbox[2], bbox[3])
        val tiles = mutableListOf<TileCoord>()
        for (z in minZoom..maxZoom) {
            val xMin = lonToTileX(minLng, z)
            val xMax = lonToTileX(maxLng, z)
            val yMin = latToTileY(maxLat, z)
            val yMax = latToTileY(minLat, z)
            for (x in min(xMin, xMax)..max(xMin, xMax)) {
                for (y in min(yMin, yMax)..max(yMin, yMax)) {
                    tiles.add(TileCoord(z, x, y))
                }
            }
        }
        return tiles
    }

    private fun tileDir(context: Context): File =
        File(context.filesDir, "offline_tiles").apply { if (!exists()) mkdirs() }

    fun tileFile(context: Context, z: Int, x: Int, y: Int): File =
        File(tileDir(context), "${z}_${x}_${y}.png")

    fun isTileCached(context: Context, z: Int, x: Int, y: Int): Boolean {
        val f = tileFile(context, z, x, y)
        return f.exists() && f.length() > 0
    }

    private fun tileNetworkUrl(z: Int, x: Int, y: Int): String {
        val s = subdomains[(x + y).mod(subdomains.size)]
        return "https://$s.tile.openstreetmap.org/$z/$x/$y.png"
    }

    private suspend fun fetchAndCacheTile(context: Context, tile: TileCoord): Boolean = withContext(Dispatchers.IO) {
        val dest = tileFile(context, tile.z, tile.x, tile.y)
        if (dest.exists() && dest.length() > 0) return@withContext true
        val temp = File(dest.parentFile, "${dest.name}.${System.currentTimeMillis()}.tmp")
        try {
            val request = Request.Builder()
                .url(tileNetworkUrl(tile.z, tile.x, tile.y))
                .header("User-Agent", "HiddenIndia-Android/1.0 (contact@hiddenindia.app)")
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
        } catch (e: Exception) {
            try { temp.delete() } catch (_: Exception) { }
            false
        }
    }

    /** Downloads all given tiles with limited concurrency, reporting progress as it goes. */
    suspend fun downloadTiles(
        context: Context,
        tiles: List<OfflineTileCache.TileCoord>,
        onProgress: ((done: Int, total: Int) -> Unit)? = null
    ): Pair<Int, Int> = withContext(Dispatchers.IO) {
        var succeeded = 0
        var failed = 0
        var done = 0
        tiles.chunked(CONCURRENCY).forEach { batch ->
            val results = batch.map { tile -> async { fetchAndCacheTile(context, tile) } }.awaitAll()
            results.forEach { if (it) succeeded++ else failed++ }
            done += batch.size
            onProgress?.invoke(done, tiles.size)
            if (done < tiles.size) delay(BATCH_DELAY_MS)
        }
        succeeded to failed
    }

    fun deleteCachedTiles(context: Context, tiles: List<TileCoord>) {
        tiles.forEach {
            try {
                val f = tileFile(context, it.z, it.x, it.y)
                if (f.exists()) f.delete()
            } catch (_: Exception) { }
        }
    }

    fun estimateTileDownloadBytes(tileCount: Int): Long = tileCount * ESTIMATED_BYTES_PER_TILE

    fun getTotalCachedTilesSize(context: Context): Long {
        val dir = tileDir(context)
        return dir.listFiles()?.filter { it.isFile && !it.name.endsWith(".tmp") }?.sumOf { it.length() } ?: 0L
    }
}
