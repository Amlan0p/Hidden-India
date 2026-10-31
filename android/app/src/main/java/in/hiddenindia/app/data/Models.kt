package `in`.hiddenindia.app.data

import com.google.gson.annotations.SerializedName

data class Coordinates(
    @SerializedName("lat") val lat: Double = 0.0,
    @SerializedName("lng") val lng: Double = 0.0
)

data class Place(
    @SerializedName("id") val id: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("category") val category: String = "Heritage",
    @SerializedName("markerCategory") val markerCategory: String? = null,
    @SerializedName("description") val description: String = "",
    @SerializedName("coordinates") val coordinates: Coordinates? = null,
    @SerializedName("image") val image: String = "",
    @SerializedName("estimatedCost") val estimatedCost: String = "",
    @SerializedName("bestTimeOfDay") val bestTimeOfDay: String = "",
    @SerializedName("insiderTip") val insiderTip: String = "",
    @SerializedName("isSecretGem") val isSecretGem: Boolean = false,
    @SerializedName("coordinateStatus") val coordinateStatus: String? = "verified"
)

data class LocalFood(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String = "",
    @SerializedName("description") val description: String = "",
    @SerializedName("whereToTry") val whereToTry: String = "",
    @SerializedName("priceRange") val priceRange: String = "",
    @SerializedName("vegetarian") val vegetarian: Boolean = true,
    @SerializedName("image") val image: String? = null,
    @SerializedName("coordinates") val coordinates: Coordinates? = null
)

data class LocalExperience(
    @SerializedName("id") val id: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("description") val description: String = "",
    @SerializedName("communityHost") val communityHost: String = "",
    @SerializedName("duration") val duration: String = "",
    @SerializedName("cost") val cost: String = "",
    @SerializedName("impactScore") val impactScore: Int = 5,
    @SerializedName("category") val category: String = "",
    @SerializedName("image") val image: String? = null,
    @SerializedName("coordinates") val coordinates: Coordinates? = null
)

data class LocalPhrase(
    @SerializedName("id") val id: String = "",
    @SerializedName("english") val english: String = "",
    @SerializedName("localText") val localText: String = "",
    @SerializedName("phonetic") val phonetic: String = "",
    @SerializedName("category") val category: String = "Greetings",
    @SerializedName("language") val language: String = ""
)

data class Hotel(
    @SerializedName("name") val name: String = "",
    @SerializedName("address") val address: String = "",
    @SerializedName("phone") val phone: String = "",
    @SerializedName("email") val email: String = ""
)

data class EmergencyPoint(
    @SerializedName("id") val id: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("type") val type: String = "Hospital",
    @SerializedName("phone") val phone: String = "",
    @SerializedName("address") val address: String = ""
)

data class EmergencyInfo(
    @SerializedName("hospitalName") val hospitalName: String = "",
    @SerializedName("hospitalAddress") val hospitalAddress: String = "",
    @SerializedName("hospitalNumber") val hospitalNumber: String = "108",
    @SerializedName("policeStationName") val policeStationName: String = "",
    @SerializedName("policeAddress") val policeAddress: String = "",
    @SerializedName("policeNumber") val policeNumber: String = "112",
    @SerializedName("touristHelpline") val touristHelpline: String = "1363",
    @SerializedName("ambulanceNumber") val ambulanceNumber: String = "102",
    @SerializedName("womenHelpline") val womenHelpline: String = "1091",
    @SerializedName("emergencyPoints") val emergencyPoints: List<EmergencyPoint> = emptyList()
)

data class Destination(
    @SerializedName("id") val id: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("tagline") val tagline: String = "",
    @SerializedName("state") val state: String = "",
    @SerializedName("region") val region: String = "",
    @SerializedName("category") val category: String = "Heritage",
    @SerializedName("secondaryCategories") val secondaryCategories: List<String> = emptyList(),
    @SerializedName("coverImage") val coverImage: String = "",
    @SerializedName("description") val description: String = "",
    @SerializedName("longDescription") val longDescription: String = "",
    @SerializedName("loreTitle") val loreTitle: String? = null,
    @SerializedName("ancientLegend") val ancientLegend: String? = null,
    @SerializedName("historicalChronicle") val historicalChronicle: String? = null,
    @SerializedName("folkloreTales") val folkloreTales: List<String> = emptyList(),
    @SerializedName("coordinates") val coordinates: Coordinates = Coordinates(),
    @SerializedName("bestTimeToVisit") val bestTimeToVisit: String = "October to March",
    @SerializedName("estimatedBudgetPerDay") val estimatedBudgetPerDay: String = "₹1,500 - ₹3,000",
    @SerializedName("downloadSizeMB") val downloadSizeMB: Double = 12.0,
    // [minLat, minLng, maxLat, maxLng] — used by OfflineTileCache to know which map
    // tiles cover this destination when its offline pack is downloaded.
    @SerializedName("osmBoundingBox") val osmBoundingBox: List<Double>? = null,
    @SerializedName("places") val places: List<Place> = emptyList(),
    @SerializedName("localFoods") val localFoods: List<LocalFood> = emptyList(),
    @SerializedName("localExperiences") val localExperiences: List<LocalExperience> = emptyList(),
    // 5 verified hotels per destination (name/address/phone/email) — see the Hotels tab
    // in DestinationDetailScreen.kt.
    @SerializedName("hotels") val hotels: List<Hotel> = emptyList(),
    @SerializedName("phrases") val phrases: List<LocalPhrase> = emptyList(),
    @SerializedName("emergencyInfo") val emergencyInfo: EmergencyInfo = EmergencyInfo(),
    @SerializedName("culturalGuidelines") val culturalGuidelines: List<String> = emptyList(),
    @SerializedName("responsibleTips") val responsibleTips: List<String> = emptyList()
) {
    val formattedDownloadSizeMB: String
        get() = if (downloadSizeMB % 1.0 == 0.0) downloadSizeMB.toInt().toString() else String.format(java.util.Locale.US, "%.1f", downloadSizeMB)
}

data class SupportedLanguage(
    @SerializedName("id") val id: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("nativeName") val nativeName: String = "",
    @SerializedName("speechCode") val speechCode: String = "en-IN",
    @SerializedName("culturalTip") val culturalTip: String = ""
)

data class ItineraryActivity(
    val timeSlot: String,
    val placeName: String,
    val activity: String,
    val description: String,
    val cost: String,
    val insiderTip: String
)

data class DayItinerary(
    val dayNumber: Int,
    val theme: String,
    val activities: List<ItineraryActivity>
)

data class GeneratedItinerary(
    val id: String,
    val destinationId: String,
    val destinationName: String,
    val state: String,
    val days: Int,
    val requestedDays: Int = days,
    val budgetStyle: String,
    val interests: List<String>,
    val totalCost: String,
    val summary: String,
    val dayPlans: List<DayItinerary>,
    val createdAt: Long = System.currentTimeMillis()
)

data class ChatMessage(
    val id: String,
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class MessageSender {
    USER, AI, SYSTEM
}
