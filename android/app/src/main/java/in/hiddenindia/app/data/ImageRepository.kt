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
 *        -> verified image_url
 *        -> existing Compose UI (through the Destination model's image fields)
 *
 * The database is the only source of image URLs. Records whose image_url is empty or whose
 * verification_status is not "verified" get NO image, and the UI shows its existing themed
 * placeholder. Nothing is searched on the web and nothing is substituted. URLs are used exactly
 * as stored (never rewritten, rebuilt or re-hosted).
 *
 * The database holds URLs and metadata only (no image files). It is opened read-only from a
 * private copy of the asset; the copy is refreshed when [IMAGE_DB_VERSION] changes.
 */
object ImageRepository {
    private const val TAG = "ImageRepository"
    private const val DB_ASSET = "databases/hidden_india_image_database_final.sqlite"
    private const val DB_FILE = "hidden_india_image_database.sqlite"
    private const val PREFS = "image_database_state"
    private const val PREFS_KEY_VERSION = "installed_version"

    /** Bump when the bundled .sqlite asset is replaced; the private copy is then refreshed. */
    private const val IMAGE_DB_VERSION = 3

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

        /** Thumbnail for lists: thumbnail_url when present and valid, otherwise image_url. */
        fun listUrl(): String? {
            if (!hasVerifiedImage) return null
            val t = thumbnailUrl
            return if (!t.isNullOrBlank() && (t.startsWith("https://") || t.startsWith("http://"))) t else imageUrl
        }
    }

    @Volatile private var db: SQLiteDatabase? = null
    @Volatile private var openFailed = false
    private val recordCache = LruCache<String, ImageRecord>(4096)
    private val missing = ConcurrentHashMap.newKeySet<String>()
    private val loggedFailures = ConcurrentHashMap.newKeySet<String>()
    private val brokenUrls = ConcurrentHashMap.newKeySet<String>()

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

    /** Verified image URL for a record ID, or null (placeholder). */
    fun imageUrlFor(context: Context, recordId: String): String? = getRecord(context, recordId)?.detailUrl()

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

    private class Slim(val type: String, val name: String, val destinationId: String, val state: String, val url: String)

    /**
     * Returns the same destinations with ONLY the image fields set from the database.
     * Whatever image URLs the bundled JSON happens to contain are ignored. A record gets an image
     * only if the database row has the SAME record ID, the same type, the same name and the same
     * destination (so an image can never be attached to a different record).
     */
    fun applyImages(context: Context, destinations: List<Destination>, report: ApplyReport = ApplyReport()): List<Destination> {
        val database = database(context)
        val verified = HashMap<String, Slim>(1024)
        if (database != null) {
            // Only rows that actually carry a verified URL are read (all 2,617 in the current database).
            database.rawQuery(
                "SELECT record_id, type, name, destination_id, state, image_url FROM image_records " +
                    "WHERE verification_status = 'verified' AND image_url IS NOT NULL AND image_url <> ''",
                null
            ).use { c ->
                while (c.moveToNext()) {
                    verified[c.getString(0)] = Slim(c.getString(1).orEmpty(), c.getString(2).orEmpty(),
                        c.getString(3).orEmpty(), c.getString(4).orEmpty(), c.getString(5).orEmpty())
                }
            }
        }

        fun pick(id: String?, type: String, name: String, destId: String, state: String): String {
            report.records++
            val s = if (id.isNullOrBlank()) null else verified[id]
            if (s == null) {
                report.placeholders++
                logRecord(id, type, name, "", "no verified image -> placeholder")
                return ""
            }
            if (s.type != type || s.name != name || s.destinationId != destId || s.state != state) {
                report.mismatches++; report.placeholders++
                Log.w(TAG, "[IMAGE_DB] id=$id does not match the app record ($type/$name); image NOT applied")
                return ""
            }
            report.withImage++
            logRecord(id, type, name, s.url, "verified")
            return s.url
        }

        val result = destinations.map { d ->
            d.copy(
                coverImage = pick(d.id, "destination_cover", d.name, d.id, d.state),
                places = d.places.map { p -> p.copy(image = pick(p.id, "place", p.name, d.id, d.state)) },
                localFoods = d.localFoods.map { f -> f.copy(image = pick(f.id, "food", f.name, d.id, d.state)) },
                localExperiences = d.localExperiences.map { e -> e.copy(image = pick(e.id, "experience", e.title, d.id, d.state)) }
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
    // Failure handling (no crash, no retry loop, record IDs logged)
    // ------------------------------------------------------------------------------------

    /** Remember that a URL permanently failed (HTTP error) so it is not requested again this session. */
    fun markBroken(url: String) { brokenUrls.add(url) }

    fun isBroken(url: String): Boolean = brokenUrls.contains(url)

    /** Logs the failed URL once, with the record ID(s) that use it (debug builds). */
    fun reportLoadFailure(context: Context, url: String) {
        if (!loggedFailures.add(url)) return
        if (!BuildConfig.DEBUG) return
        val database = database(context) ?: return
        database.rawQuery("SELECT record_id, type, name FROM image_records WHERE image_url = ? LIMIT 5", arrayOf(url)).use { c ->
            while (c.moveToNext()) {
                Log.w(TAG, "[IMAGE_DB] image failed to load: id=${c.getString(0)} type=${c.getString(1)} name=${c.getString(2)} url=$url")
            }
        }
    }
}

/**
 * Coil interceptor: a URL that already failed with an HTTP error this session is not requested
 * again, so the UI settles on the placeholder instead of retrying while scrolling.
 */
class BrokenImageInterceptor : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val data = chain.request.data
        if (data is String && ImageRepository.isBroken(data)) {
            return ErrorResult(null, chain.request, IllegalStateException("image previously failed: $data"))
        }
        return chain.proceed(chain.request)
    }
}
