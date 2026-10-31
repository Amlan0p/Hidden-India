package `in`.hiddenindia.app.network

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type

/**
 * Matches GET /api/geo/nearest-emergency in backend/server.ts — an OpenStreetMap
 * (Overpass API) lookup for the real nearest hospital/police station to a GPS
 * coordinate. Either field is null when nothing verified was found nearby; the backend
 * never invents a facility name.
 *
 * Uses NearestEmergencyDeserializer to seamlessly parse whether the backend returns
 * singular objects, lists/arrays, or null for hospital and police facilities.
 */
@JsonAdapter(NearestEmergencyDeserializer::class)
data class NearestEmergencyResponse(
    @SerializedName("hospital") val hospital: EmergencyFacility?,
    @SerializedName("police") val police: EmergencyFacility?,
    val hospitalList: List<EmergencyFacility> = if (hospital != null) listOf(hospital) else emptyList(),
    val policeList: List<EmergencyFacility> = if (police != null) listOf(police) else emptyList()
) {
    val hospitalCount: Int get() = hospitalList.size
    val policeCount: Int get() = policeList.size
}

class NearestEmergencyDeserializer : JsonDeserializer<NearestEmergencyResponse> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): NearestEmergencyResponse {
        if (!json.isJsonObject) {
            return NearestEmergencyResponse(hospital = null, police = null)
        }
        val obj = json.asJsonObject

        fun parseFacilities(element: JsonElement?): List<EmergencyFacility> {
            if (element == null || element.isJsonNull) return emptyList()
            return if (element.isJsonArray) {
                element.asJsonArray.mapNotNull { item ->
                    try {
                        context.deserialize<EmergencyFacility>(item, EmergencyFacility::class.java)
                    } catch (e: Exception) {
                        null
                    }
                }
            } else if (element.isJsonObject) {
                try {
                    val facility = context.deserialize<EmergencyFacility>(element, EmergencyFacility::class.java)
                    listOfNotNull(facility)
                } catch (e: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }
        }

        val hospitals = parseFacilities(obj.get("hospital"))
            .ifEmpty { parseFacilities(obj.get("hospitals")) }
        val policeStations = parseFacilities(obj.get("police"))
            .ifEmpty { parseFacilities(obj.get("policeStations")) }
            .ifEmpty { parseFacilities(obj.get("police_stations")) }

        return NearestEmergencyResponse(
            hospital = hospitals.firstOrNull(),
            police = policeStations.firstOrNull(),
            hospitalList = hospitals,
            policeList = policeStations
        )
    }
}

data class EmergencyFacility(
    @SerializedName("name") val name: String,
    @SerializedName("address") val address: String?,
    @SerializedName("phone") val phone: String?,
    @SerializedName("distanceKm") val distanceKm: Double,
    @SerializedName("lat") val lat: Double? = null,
    @SerializedName("lng") val lng: Double? = null
)

/**
 * Matches GET /api/images/resolve in backend/server.ts — a real Wikipedia API lookup.
 * `image` is null (never a guessed/fabricated URL) when nothing verified was found.
 */
data class ImageResolveResponse(
    @SerializedName("image") val image: String?,
    @SerializedName("sourceTitle") val sourceTitle: String?
)

