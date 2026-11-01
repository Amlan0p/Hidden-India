package `in`.hiddenindia.app.data

import android.util.Log
import `in`.hiddenindia.app.network.EmergencyFacility
import `in`.hiddenindia.app.network.NearestEmergencyResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Direct OpenStreetMap Overpass emergency facility lookup.
 * Queries public Overpass API endpoints directly on-device with multiple fallbacks
 * to reliably locate the nearest verified hospital and police station.
 */
class DirectOsmEmergencyService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(25, TimeUnit.SECONDS)
        .build()
) {

    private val endpoints = listOf(
        "https://overpass-api.de/api/interpreter",
        "https://lz4.overpass-api.de/api/interpreter",
        "https://overpass.kumi.systems/api/interpreter",
        "https://overpass.private.coffee/api/interpreter",
        "https://maps.mail.ru/osm/tools/overpass/api/interpreter"
    )

    suspend fun findNearestEmergency(userLat: Double, userLng: Double): NearestEmergencyResponse? =
        withContext(Dispatchers.IO) {
            var activeProvider: String? = null

            // Query hospitals and police stations simultaneously in parallel
            val (hospitalResult, policeResult) = coroutineScope {
                val hDeferred = async {
                    fetchAmenityWithFallback("hospital", userLat, userLng) { provider ->
                        if (activeProvider == null) activeProvider = provider
                    }
                }
                val pDeferred = async {
                    fetchAmenityWithFallback("police", userLat, userLng) { provider ->
                        if (activeProvider == null) activeProvider = provider
                    }
                }
                hDeferred.await() to pDeferred.await()
            }

            val hospitals = hospitalResult.first
            val policeStations = policeResult.first
            val providerUsed = activeProvider ?: hospitalResult.second ?: policeResult.second ?: endpoints.first()

            val selectedHospital = hospitals.minByOrNull { it.distanceKm }
            val selectedPolice = policeStations.minByOrNull { it.distanceKm }

            Log.d("EmergencySOS", "SOS DIRECT OSM:\nlatitude = $userLat")
            Log.d("EmergencySOS", "SOS DIRECT OSM:\nlongitude = $userLng")
            Log.d("EmergencySOS", "SOS DIRECT OSM:\nprovider = $providerUsed")
            Log.d("EmergencySOS", "SOS DIRECT OSM:\nhospital count = ${hospitals.size}")
            Log.d("EmergencySOS", "SOS DIRECT OSM:\npolice count = ${policeStations.size}")
            Log.d("EmergencySOS", "SOS DIRECT OSM:\nselected hospital = ${selectedHospital?.name ?: "null"}")
            Log.d("EmergencySOS", "SOS DIRECT OSM:\nselected police = ${selectedPolice?.name ?: "null"}")

            if (selectedHospital != null || selectedPolice != null) {
                NearestEmergencyResponse(
                    hospital = selectedHospital,
                    police = selectedPolice,
                    hospitalList = hospitals,
                    policeList = policeStations
                )
            } else {
                null
            }
        }

    private suspend fun fetchAmenityWithFallback(
        amenity: String,
        userLat: Double,
        userLng: Double,
        onProviderSuccess: (String) -> Unit
    ): Pair<List<EmergencyFacility>, String?> {
        // Initial search radius: 5 km (5000m)
        val initial = fetchAmenity(amenity, 5000, userLat, userLng, onProviderSuccess)
        if (initial.first.isNotEmpty()) {
            return initial
        }

        // Expand radius to 15 km if insufficient results in 5 km
        val secondary = fetchAmenity(amenity, 15000, userLat, userLng, onProviderSuccess)
        if (secondary.first.isNotEmpty()) {
            return secondary
        }

        // Fallback to 50 km for remote rural regions
        return fetchAmenity(amenity, 50000, userLat, userLng, onProviderSuccess)
    }

    private suspend fun fetchAmenity(
        amenity: String,
        radiusMeters: Int,
        userLat: Double,
        userLng: Double,
        onProviderSuccess: (String) -> Unit
    ): Pair<List<EmergencyFacility>, String?> {
        val query = """
            [out:json][timeout:15];
            nwr["amenity"="$amenity"](around:$radiusMeters,$userLat,$userLng);
            out center;
        """.trimIndent()

        val defaultPhone = if (amenity == "hospital") "108" else "112"

        for (endpoint in endpoints) {
            try {
                val url = endpoint.toHttpUrlOrNull()?.newBuilder()
                    ?.addQueryParameter("data", query)
                    ?.build() ?: continue

                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "HiddenIndiaAndroid/1.0 (contact@hiddenindia.in)")
                    .header("Accept", "*/*")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e("EmergencySOS", "SOS DIRECT OSM provider failed:\n$endpoint\nHTTP ${response.code}")
                        if (response.code == 504 || response.code == 429) {
                            delay(1500)
                        }
                        return@use
                    }
                    val bodyString = response.body?.string() ?: return@use
                    val json = JSONObject(bodyString)
                    val elements = json.optJSONArray("elements") ?: return@use

                    val facilities = mutableListOf<EmergencyFacility>()
                    for (i in 0 until elements.length()) {
                        val el = elements.optJSONObject(i) ?: continue
                        val tags = el.optJSONObject("tags") ?: continue
                        val name = tags.optString("name").trim()
                        if (name.isEmpty()) continue

                        val elLat: Double
                        val elLng: Double
                        if (el.has("center")) {
                            val center = el.getJSONObject("center")
                            elLat = center.optDouble("lat", Double.NaN)
                            elLng = center.optDouble("lon", Double.NaN)
                        } else {
                            elLat = el.optDouble("lat", Double.NaN)
                            elLng = el.optDouble("lon", Double.NaN)
                        }
                        if (elLat.isNaN() || elLng.isNaN()) continue

                        val distanceKm = calculateHaversineKm(userLat, userLng, elLat, elLng)

                        val fullAddr = tags.optString("addr:full").takeIf { it.isNotBlank() }
                        val street = tags.optString("addr:street").takeIf { it.isNotBlank() }
                        val city = tags.optString("addr:city").takeIf { it.isNotBlank() }
                        val address = fullAddr
                            ?: listOfNotNull(street, city).joinToString(", ").takeIf { it.isNotBlank() }
                            ?: "Address not available for this facility"

                        val phone = tags.optString("phone").takeIf { it.isNotBlank() }
                            ?: tags.optString("contact:phone").takeIf { it.isNotBlank() }
                            ?: defaultPhone

                        facilities.add(
                            EmergencyFacility(
                                name = name,
                                address = address,
                                phone = phone,
                                distanceKm = distanceKm,
                                lat = elLat,
                                lng = elLng
                            )
                        )
                    }

                    if (facilities.isNotEmpty()) {
                        onProviderSuccess(endpoint)
                        facilities.sortBy { it.distanceKm }
                        return Pair(facilities, endpoint)
                    }
                }
            } catch (e: Exception) {
                Log.e("EmergencySOS", "SOS DIRECT OSM provider failed:\n$endpoint\n${e.message ?: e.javaClass.simpleName}")
            }
            delay(1000)
        }

        return Pair(emptyList(), null)
    }

    private fun calculateHaversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Radius of the Earth in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        val dist = r * c
        return round(dist * 10.0) / 10.0
    }
}
