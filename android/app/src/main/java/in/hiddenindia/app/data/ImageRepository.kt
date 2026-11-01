package `in`.hiddenindia.app.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import android.util.LruCache
import coil.intercept.Interceptor
import coil.request.ErrorResult
import coil.request.ImageResult
import `in`.hiddenindia.app.BuildConfig
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * Centralized, read-only image repository.
 *
 *   hidden_india_image_database_final.sqlite (bundled asset)
 *        -> ImageRepository
 *        -> record-ID lookup (loc_0001, place_loc_0001_1, food_loc_0001_1, exp_loc_0001_1)
 *        -> verified image_url (full-resolution) and thumbnail_url (500px standard size)
 *        -> existing Compose UI (through the Destination model's image and thumbnail fields)
 *
 * The database is the authoritative source of image URLs. Records whose image_url is empty
 * or whose verification_status is not "verified" get NO image, and the UI displays a clean
 * themed placeholder.
 */
object ImageRepository {
    private const val TAG = "ImageRepository"
    private const val DB_ASSET = "databases/hidden_india_image_database_final.sqlite"
    private const val DB_FILE = "hidden_india_image_database.sqlite"
    private const val PREFS = "image_database_state"
    private const val PREFS_KEY_VERSION = "installed_version"

    /** Bumping version to 4 ensures the private SQLite database is refreshed with updated thumbnail_url records. */
    private const val IMAGE_DB_VERSION = 4

    const val STATUS_VERIFIED = "verified"

    data class ImageRecord(
        val recordId: String,
        val type: String,
        val name: String,
        val destinationId: String,
        val destination: String,
        val state: String,
        val imageUrl: String?,
        val thumbnailUrl: String?,
        val source: String?,
        val sourcePage: String?,
        val imageTitle: String?,
        val verificationStatus: String?,
        val confidence: Double,
        val reason: String?
    ) {
        /** True only for a verified record that actually carries an image URL. */
        val hasVerifiedImage: Boolean
            get() = verificationStatus == STATUS_VERIFIED && !imageUrl.isNullOrBlank()

        /** Full-size / detail image: always image_url (exact, untouched). */
        fun detailUrl(): String? = if (hasVerifiedImage) imageUrl else null

        /** Thumbnail for cards and list containers: thumbnail_url when present, otherwise derived 500px thumbnail or image_url. */
        fun listUrl(): String? {
            if (!hasVerifiedImage) return null
            val t = thumbnailUrl
            if (!t.isNullOrBlank() && (t.startsWith("https://") || t.startsWith("http://"))) {
                return t
            }
            return imageUrl?.let { makeWikimediaThumbnailUrl(it, 500) }
        }
    }

    @Volatile private var db: SQLiteDatabase? = null
    @Volatile private var openFailed = false
    private val recordCache = LruCache<String, ImageRecord>(4096)
    private val missing = ConcurrentHashMap.newKeySet<String>()
    private val loggedFailures = ConcurrentHashMap.newKeySet<String>()

    /** Cache permanent failures (HTTP 404/410) with an expiration timestamp (10 minutes TTL). */
    private val permanentFailures = ConcurrentHashMap<String, Long>()
    private const val FAILURE_CACHE_TTL_MS = 10 * 60 * 1000L

    // ------------------------------------------------------------------------------------
    // Database access
    // ------------------------------------------------------------------------------------

    @Synchronized
    private fun database(context: Context): SQLiteDatabase? {
        db?.let { if (it.isOpen) return it }
        if (openFailed) return null
        return try {
            val appContext = context.applicationContext
            val target = appContext.getDatabasePath(DB_FILE)
            val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            if (!target.exists() || prefs.getInt(PREFS_KEY_VERSION, 0) != IMAGE_DB_VERSION) {
                target.parentFile?.mkdirs()
                val tmp = File(target.parentFile, "$DB_FILE.tmp")
                appContext.assets.open(DB_ASSET).use { input ->
                    FileOutputStream(tmp).use { output -> input.copyTo(output) }
                }
                if (target.exists()) target.delete()
                if (!tmp.renameTo(target)) throw IllegalStateException("could not install image database")
                prefs.edit().putInt(PREFS_KEY_VERSION, IMAGE_DB_VERSION).apply()
            }
            val opened = SQLiteDatabase.openDatabase(target.path, null, SQLiteDatabase.OPEN_READONLY)
            db = opened
            opened
        } catch (e: Exception) {
            openFailed = true
            Log.e(TAG, "Image database unavailable; all images fall back to placeholders: ${e.message}", e)
            null
        }
    }

    private const val COLUMNS =
        "record_id, type, name, destination_id, destination, state, image_url, thumbnail_url, " +
            "source, source_page, image_title, verification_status, confidence, reason"

    private fun android.database.Cursor.toRecord() = ImageRecord(
        recordId = getString(0).orEmpty(),
        type = getString(1).orEmpty(),
        name = getString(2).orEmpty(),
        destinationId = getString(3).orEmpty(),
        destination = getString(4).orEmpty(),
        state = getString(5).orEmpty(),
        imageUrl = getString(6),
        thumbnailUrl = getString(7),
        source = getString(8),
        sourcePage = getString(9),
        imageTitle = getString(10),
        verificationStatus = getString(11),
        confidence = if (isNull(12)) 0.0 else getDouble(12),
        reason = getString(13)
    )

    /** Indexed lookup (primary key) of a single record. Cached. */
    fun getRecord(context: Context, recordId: String): ImageRecord? {
        if (recordId.isBlank()) return null
        recordCache.get(recordId)?.let { return it }
        if (missing.contains(recordId)) return null
        val database = database(context) ?: return null
        database.rawQuery("SELECT $COLUMNS FROM image_records WHERE record_id = ?", arrayOf(recordId)).use { c ->
            if (c.moveToFirst()) {
                val r = c.toRecord()
                recordCache.put(recordId, r)
                return r
            }
        }
        missing.add(recordId)
        return null
    }

    /** Verified full-size image URL for a record ID, or null (placeholder). */
    fun imageUrlFor(context: Context, recordId: String): String? = getRecord(context, recordId)?.detailUrl()

    /** Verified thumbnail image URL for a record ID (or falls back to detail URL). */
    fun listUrlFor(context: Context, recordId: String): String? = getRecord(context, recordId)?.listUrl()

    // ------------------------------------------------------------------------------------
    // Applying the database to the app's destination data
    // ------------------------------------------------------------------------------------

    data class ApplyReport(
        var records: Int = 0,
        var withImage: Int = 0,
        var placeholders: Int = 0,
        var mismatches: Int = 0,
        var unmatched: Int = 0
    )

    private class Slim(
        val type: String,
        val name: String,
        val destinationId: String,
        val state: String,
        val imageUrl: String,
        val thumbnailUrl: String
    )

    /**
     * Constructs a compliant Wikimedia Commons thumbnail URL (500px bucket).
     */
    fun makeWikimediaThumbnailUrl(url: String, widthPx: Int = 500): String {
        if (!url.startsWith("https://upload.wikimedia.org/wikipedia/commons/")) return url
        val marker = "/wikipedia/commons/"
        val idx = url.indexOf(marker)
        if (idx == -1) return url
        val path = url.substring(idx + marker.length)
        if (path.startsWith("thumb/")) return url
        val filename = path.substringAfterLast('/')
        return "https://upload.wikimedia.org/wikipedia/commons/thumb/$path/${widthPx}px-$filename"
    }

    /**
     * Returns the same destinations with verified image_url and thumbnail_url set from the database.
     * Record-ID matching is strictly preserved: a record receives an image only if the database
     * row has the exact same record ID, type, name, destination, and state.
     */
    fun applyImages(context: Context, destinations: List<Destination>, report: ApplyReport = ApplyReport()): List<Destination> {
        val database = database(context)
        val verified = HashMap<String, Slim>(2700)
        if (database != null) {
            database.rawQuery(
                "SELECT record_id, type, name, destination_id, state, image_url, thumbnail_url FROM image_records " +
                    "WHERE verification_status = 'verified' AND image_url IS NOT NULL AND image_url <> ''",
                null
            ).use { c ->
                while (c.moveToNext()) {
                    val rid = c.getString(0).orEmpty()
                    val type = c.getString(1).orEmpty()
                    val name = c.getString(2).orEmpty()
                    val destId = c.getString(3).orEmpty()
                    val state = c.getString(4).orEmpty()
                    val imgUrl = c.getString(5).orEmpty()
                    val thumbUrl = c.getString(6).orEmpty().ifBlank { makeWikimediaThumbnailUrl(imgUrl, 500) }
                    verified[rid] = Slim(type, name, destId, state, imgUrl, thumbUrl)
                }
            }
        }

        fun pick(id: String?, type: String, name: String, destId: String, state: String): Pair<String, String> {
            report.records++
            val s = if (id.isNullOrBlank()) null else verified[id]
            if (s == null) {
                report.placeholders++
                logRecord(id, type, name, "", "no verified image -> placeholder")
                return Pair("", "")
            }
            if (s.type != type || s.name != name || s.destinationId != destId || s.state != state) {
                report.mismatches++
                report.placeholders++
                Log.w(TAG, "[IMAGE_DB] id=$id does not match the app record ($type/$name); image NOT applied")
                return Pair("", "")
            }
            report.withImage++
            logRecord(id, type, name, s.imageUrl, "verified")
            return Pair(s.imageUrl, s.thumbnailUrl)
        }

        val result = destinations.map { d ->
            val (cover, coverThumb) = pick(d.id, "destination_cover", d.name, d.id, d.state)
            d.copy(
                coverImage = cover,
                thumbnailUrl = coverThumb.ifBlank { null },
                places = d.places.map { p ->
                    val (pImg, pThumb) = pick(p.id, "place", p.name, d.id, d.state)
                    p.copy(
                        image = pImg,
                        thumbnailUrl = pThumb.ifBlank { null }
                    )
                },
                localFoods = d.localFoods.map { f ->
                    val (fImg, fThumb) = pick(f.id, "food", f.name, d.id, d.state)
                    f.copy(
                        image = fImg.ifBlank { null },
                        thumbnailUrl = fThumb.ifBlank { null }
                    )
                },
                localExperiences = d.localExperiences.map { e ->
                    val (eImg, eThumb) = pick(e.id, "experience", e.title, d.id, d.state)
                    e.copy(
                        image = eImg.ifBlank { null },
                        thumbnailUrl = eThumb.ifBlank { null }
                    )
                }
            )
        }
        report.unmatched = (verified.size - report.withImage).coerceAtLeast(0)
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "[IMAGE_DB] applied: records=${report.records}, withImage=${report.withImage}, " +
                "placeholders=${report.placeholders}, mismatches=${report.mismatches}, " +
                "verifiedRowsNotMatchedToAppRecord=${report.unmatched}, dbAvailable=${database != null}")
        }
        return result
    }

    private fun logRecord(id: String?, type: String, name: String, url: String, status: String) {
        if (BuildConfig.DEBUG) Log.v(TAG, "[IMAGE_DB]\nid=$id\ntype=$type\nname=$name\nimage_url=$url\nstatus=$status")
    }

    // ------------------------------------------------------------------------------------
    // Failure handling with graceful classification and TTL expiration
    // ------------------------------------------------------------------------------------

    /**
     * Mark a failure. Only permanent errors (404 Not Found, 410 Gone) are cached
     * with an expiration timestamp so they do not block retry after network recovery.
     */
    fun markFailure(url: String, statusCode: Int, errorMessage: String? = null) {
        if (statusCode == 404 || statusCode == 410) {
            permanentFailures[url] = System.currentTimeMillis()
        }
        ImageDiagnostics.recordFailure(url, statusCode, errorMessage)
    }

    /**
     * Check if a URL has a genuine, unexpired permanent failure (HTTP 404/410).
     */
    fun isPermanentlyBroken(url: String): Boolean {
        val failedAt = permanentFailures[url] ?: return false
        if (System.currentTimeMillis() - failedAt > FAILURE_CACHE_TTL_MS) {
            permanentFailures.remove(url)
            return false
        }
        return true
    }

    fun clearFailure(url: String) {
        permanentFailures.remove(url)
    }

    /** Logs the failed URL once, with the record ID(s) that use it (debug builds). */
    fun reportLoadFailure(context: Context, url: String) {
        if (!loggedFailures.add(url)) return
        if (!BuildConfig.DEBUG) return
        val database = database(context) ?: return
        database.rawQuery("SELECT record_id, type, name FROM image_records WHERE image_url = ? OR thumbnail_url = ? LIMIT 5", arrayOf(url, url)).use { c ->
            while (c.moveToNext()) {
                Log.w(TAG, "[IMAGE_DB] image failed to load: id=${c.getString(0)} type=${c.getString(1)} name=${c.getString(2)} url=$url")
            }
        }
    }
}

/**
 * Coil interceptor: checks if a URL has an active permanent failure (404/410).
 * Temporary network errors (timeouts, 429 rate limits, 5xx server issues) are NEVER blocked.
 */
class BrokenImageInterceptor : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val data = chain.request.data
        if (data is String && ImageRepository.isPermanentlyBroken(data)) {
            return ErrorResult(null, chain.request, IllegalStateException("image permanently missing (404/410): $data"))
        }
        return chain.proceed(chain.request)
    }
}
