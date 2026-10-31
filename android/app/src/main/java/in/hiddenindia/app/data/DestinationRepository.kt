package `in`.hiddenindia.app.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DestinationRepository(private val context: Context) {
    private val gson = Gson()
    private val prefs: SharedPreferences =
        context.getSharedPreferences("hidden_india_prefs", Context.MODE_PRIVATE)

    private var cachedDestinations: List<Destination> = emptyList()
    private var cachedLanguages: List<SupportedLanguage> = emptyList()

    suspend fun loadDestinations(): List<Destination> = withContext(Dispatchers.IO) {
        if (cachedDestinations.isNotEmpty()) return@withContext cachedDestinations
        try {
            val json = context.assets.open("destinations.json").bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<Destination>>() {}.type
            val raw: List<Destination> = gson.fromJson(json, type) ?: emptyList()
            // Image URLs come ONLY from the verified image database (matched by record ID).
            // Image fields in the bundled JSON are ignored; empty/unverified records get no image.
            val list = ImageRepository.applyImages(context, raw)
            cachedDestinations = list
            list
        } catch (e: Exception) {
            Log.e("DestinationRepository", "Failed to load destinations.json: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun loadLanguages(): List<SupportedLanguage> = withContext(Dispatchers.IO) {
        if (cachedLanguages.isNotEmpty()) return@withContext cachedLanguages
        try {
            val json = context.assets.open("languages.json").bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<SupportedLanguage>>() {}.type
            val list: List<SupportedLanguage> = gson.fromJson(json, type) ?: emptyList()
            cachedLanguages = list
            list
        } catch (e: Exception) {
            Log.e("DestinationRepository", "Failed to load languages.json: ${e.message}", e)
            emptyList()
        }
    }

    // NOTE: favorites and saved itineraries used to be persisted here in
    // SharedPreferences. As of Stage 4 they are synced with the backend
    // account instead (see AuthRepository/AuthViewModel) and are no longer
    // stored locally as a source of truth. Offline pack downloads below are
    // unrelated (a different, still-local-only feature) and are unaffected.

    fun getDownloadedPackIds(): Set<String> {
        return prefs.getStringSet("downloaded_packs", emptySet()) ?: emptySet()
    }

    fun getDownloadTimestamp(destinationId: String): Long {
        return prefs.getLong("downloaded_pack_ts_$destinationId", 0L)
    }

    fun toggleDownloadPack(destinationId: String): Boolean {
        val current = getDownloadedPackIds().toMutableSet()
        val downloaded = if (current.contains(destinationId)) {
            current.remove(destinationId)
            prefs.edit().remove("downloaded_pack_ts_$destinationId").apply()
            false
        } else {
            current.add(destinationId)
            prefs.edit().putLong("downloaded_pack_ts_$destinationId", System.currentTimeMillis()).apply()
            true
        }
        prefs.edit().putStringSet("downloaded_packs", current).apply()
        return downloaded
    }

    // NOTE: itinerary generation and destination chat used to be answered here
    // with local template/keyword logic (generateLocalItinerary /
    // generateChatResponse). As of Stage 5 both are real backend AI calls
    // instead — see data/AiRepository.kt and MainViewModel's
    // generateItinerary()/sendChatMessage(), which call
    // POST /api/ai/plan-trip and POST /api/ai/ask-place. There is no local
    // fallback pretending to be AI.
}
