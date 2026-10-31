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

    suspend fun getNearestEmergencyFacilities(lat: Double, lng: Double): ApiResult<NearestEmergencyResponse> =
        withContext(Dispatchers.IO) {
            // Never send an invalid position (NaN, out of range, or the (0.0, 0.0) "null island"
            // default) to either lookup.
            if (!isValidCoordinate(lat, lng)) {
                Log.w("EmergencySOS", "SOS OSM: rejected invalid coordinates (not sent to any lookup)")
                return@withContext ApiResult.Failure(ApiError.Unexpected("Invalid coordinates for emergency lookup"))
            }
            // These exact values are what both the direct OSM lookup and the Render fallback receive.
            if (BuildConfig.DEBUG) {
                Log.d("EmergencySOS", "SOS OSM:\nusing latitude = ${rounded(lat)}")
                Log.d("EmergencySOS", "SOS OSM:\nusing longitude = ${rounded(lng)}")
            }
            // First attempt direct on-device OpenStreetMap Overpass lookup
            try {
                val directResult = directOsmService.findNearestEmergency(lat, lng)
                if (directResult != null && (directResult.hospital != null || directResult.police != null)) {
                    return@withContext ApiResult.Success(directResult)
                }
            } catch (e: Exception) {
                Log.w("EmergencySOS", "Direct OSM lookup exception: ${e.message}")
            }

            // Fallback to the existing Render backend endpoint
            if (BuildConfig.DEBUG) {
                Log.d("EmergencySOS", "SOS RENDER FALLBACK:\nusing latitude = ${rounded(lat)}")
                Log.d("EmergencySOS", "SOS RENDER FALLBACK:\nusing longitude = ${rounded(lng)}")
            }

            val result = safeApiCall { api.getNearestEmergencyFacilities(lat, lng) }
            when (result) {
                is ApiResult.Success -> {
                    val data = result.data
                    Log.d("EmergencySOS", "SOS API:\nHTTP status = 200")
                    Log.d("EmergencySOS", "SOS API:\nresponse body = {hospital=${data.hospital?.name ?: "null"}, police=${data.police?.name ?: "null"}}")
                    Log.d("EmergencySOS", "SOS:\nhospital count = ${data.hospitalCount}")
                    Log.d("EmergencySOS", "SOS:\npolice count = ${data.policeCount}")
                }
                is ApiResult.Failure -> {
                    val httpCode = (result.error as? ApiError.Http)?.code?.toString() ?: "failure"
                    Log.w("EmergencySOS", "SOS API:\nHTTP status = $httpCode")
                    Log.w("EmergencySOS", "SOS API request failed: ${result.error.message}")
                }
            }
            result
        }

    private fun isValidCoordinate(lat: Double, lng: Double): Boolean {
        if (lat.isNaN() || lng.isNaN() || lat.isInfinite() || lng.isInfinite()) return false
        if (lat < -90.0 || lat > 90.0 || lng < -180.0 || lng > 180.0) return false
        if (kotlin.math.abs(lat) < 1e-6 && kotlin.math.abs(lng) < 1e-6) return false
        return true
    }

    /** Logs are rounded to ~100 m so they don't record an exact position. */
    private fun rounded(value: Double): String = String.format(Locale.US, "%.3f", value)
}
