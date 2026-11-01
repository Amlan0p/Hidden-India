package `in`.hiddenindia.app.data

import android.content.Context
import android.util.Log
import `in`.hiddenindia.app.BuildConfig
import `in`.hiddenindia.app.network.ApiClient
import `in`.hiddenindia.app.network.ApiError
import `in`.hiddenindia.app.network.ApiResult
import `in`.hiddenindia.app.network.ApiService
import `in`.hiddenindia.app.network.NearestEmergencyResponse
import `in`.hiddenindia.app.network.safeApiCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Talks to GET /api/geo/nearest-emergency (backend/server.ts) — the real,
 * OpenStreetMap-backed nearest hospital/police lookup used by EmergencySOSScreen's
 * "Current Location (GPS)" mode. No auth required, same as AiRepository's endpoints.
 */
class GeoRepository(context: Context) {

    private val api: ApiService = ApiClient.create(context.applicationContext)
    private val directOsmService = DirectOsmEmergencyService()

    private val cache = java.util.concurrent.ConcurrentHashMap<String, NearestEmergencyResponse>()

    suspend fun getNearestEmergencyFacilities(lat: Double, lng: Double): ApiResult<NearestEmergencyResponse> =
        withContext(Dispatchers.IO) {
            if (!isValidCoordinate(lat, lng)) {
                Log.w("EmergencySOS", "SOS OSM: rejected invalid coordinates")
                return@withContext ApiResult.Failure(ApiError.Unexpected("Invalid coordinates for emergency lookup"))
            }

            val cacheKey = String.format(Locale.US, "%.2f,%.2f", lat, lng)
            val cached = cache[cacheKey]
            if (cached != null) {
                Log.d("EmergencySOS", "Returning cached emergency facilities for $cacheKey")
                return@withContext ApiResult.Success(cached)
            }

            // 1. Call the existing Render backend endpoint first: GET /api/geo/nearest-emergency
            // The backend uses Geoapify Places API as its primary provider (<200ms speed).
            val apiResult = safeApiCall { api.getNearestEmergencyFacilities(lat, lng) }
            if (apiResult is ApiResult.Success) {
                cache[cacheKey] = apiResult.data
                Log.d("EmergencySOS", "Backend Geoapify emergency lookup succeeded")
                return@withContext apiResult
            }

            // 2. Fallback to on-device direct Overpass lookup ONLY if backend server is unreachable
            Log.w("EmergencySOS", "Backend unavailable, falling back to direct on-device emergency lookup")
            try {
                val directResult = directOsmService.findNearestEmergency(lat, lng)
                if (directResult != null) {
                    cache[cacheKey] = directResult
                    return@withContext ApiResult.Success(directResult)
                }
            } catch (e: Exception) {
                Log.w("EmergencySOS", "Direct fallback exception: ${e.message}")
            }

            apiResult
        }

    private fun isValidCoordinate(lat: Double, lng: Double): Boolean {
        if (lat.isNaN() || lng.isNaN() || lat.isInfinite() || lng.isInfinite()) return false
        if (lat < -90.0 || lat > 90.0 || lng < -180.0 || lng > 180.0) return false
        if (kotlin.math.abs(lat) < 1e-6 && kotlin.math.abs(lng) < 1e-6) return false
        return true
    }

    private fun rounded(value: Double): String = String.format(Locale.US, "%.3f", value)
}
