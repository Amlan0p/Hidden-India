package `in`.hiddenindia.app.network

import com.google.gson.annotations.SerializedName

/**
 * Request/response models below are matched field-for-field against the
 * actual backend implementation in backend/server.ts (GET /api/ai/status,
 * POST /api/ai/plan-trip, POST /api/ai/ask-place, POST /api/ai/translate).
 * None of these endpoints require authentication on the backend (no
 * requireAuth middleware) — the AuthInterceptor still attaches a Bearer
 * token when one exists, which the backend simply ignores for these routes.
 */

// ---- GET /api/ai/status ----

data class AiStatusResponse(
    @SerializedName("configured") val configured: Boolean,
    @SerializedName("groqConfigured") val groqConfigured: Boolean,
    @SerializedName("geminiConfigured") val geminiConfigured: Boolean,
    @SerializedName("model") val model: String,
    @SerializedName("provider") val provider: String,
    @SerializedName("message") val message: String
)

// ---- POST /api/ai/ask-place ----

/** sender must be exactly "user" or "ai" to match backend/server.ts's chatHistory handling. */
data class AiChatHistoryMessage(
    @SerializedName("sender") val sender: String,
    @SerializedName("text") val text: String
)

data class AskPlaceRequest(
    @SerializedName("destinationId") val destinationId: String? = null,
    @SerializedName("question") val question: String,
    @SerializedName("chatHistory") val chatHistory: List<AiChatHistoryMessage> = emptyList()
)

data class AskPlaceResponse(
    @SerializedName("answer") val answer: String,
    @SerializedName("destinationId") val destinationId: String? = null,
    @SerializedName("destinationName") val destinationName: String? = null,
    @SerializedName("provider") val provider: String? = null,
    @SerializedName("model") val model: String? = null
)

// ---- POST /api/ai/plan-trip ----

data class PlanTripRequest(
    @SerializedName("destinationId") val destinationId: String,
    @SerializedName("days") val days: Int,
    @SerializedName("budgetStyle") val budgetStyle: String,
    @SerializedName("interests") val interests: List<String> = emptyList(),
    @SerializedName("travelStyle") val travelStyle: String = "Authentic & Immersive"
)

data class AiItineraryItem(
    @SerializedName("id") val id: String? = null,
    @SerializedName("time") val time: String? = null,
    @SerializedName("placeName") val placeName: String? = null,
    @SerializedName("activity") val activity: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("estimatedCost") val estimatedCost: String? = null,
    @SerializedName("duration") val duration: String? = null,
    @SerializedName("travelDistance") val travelDistance: String? = null,
    @SerializedName("category") val category: String? = null
)

data class AiDayPlan(
    @SerializedName("dayNumber") val dayNumber: Int,
    @SerializedName("title") val title: String? = null,
    @SerializedName("theme") val theme: String? = null,
    @SerializedName("items") val items: List<AiItineraryItem> = emptyList()
)

/**
 * Raw shape of backend/server.ts's POST /api/ai/plan-trip response — this is
 * intentionally NOT the same as the app's own GeneratedItinerary UI model
 * (field names differ: totalEstimatedCost vs totalCost, items vs activities,
 * time vs timeSlot, estimatedCost vs cost, etc). See
 * data/AiMappers.kt's toGeneratedItinerary() for the explicit mapping, which
 * keeps the existing itinerary UI/Stage 4 save-to-account flow unchanged.
 */
data class PlanTripResponse(
    @SerializedName("id") val id: String,
    @SerializedName("destinationId") val destinationId: String,
    @SerializedName("destinationName") val destinationName: String,
    @SerializedName("state") val state: String,
    @SerializedName("days") val days: Int,
    @SerializedName("budgetStyle") val budgetStyle: String,
    @SerializedName("interests") val interests: List<String> = emptyList(),
    @SerializedName("travelStyle") val travelStyle: String? = null,
    @SerializedName("totalEstimatedCost") val totalEstimatedCost: String,
    @SerializedName("curatedDate") val curatedDate: String? = null,
    @SerializedName("summary") val summary: String,
    @SerializedName("dayPlans") val dayPlans: List<AiDayPlan> = emptyList(),
    @SerializedName("provider") val provider: String? = null,
    @SerializedName("model") val model: String? = null
)

// ---- POST /api/ai/translate ----

data class TranslateRequest(
    @SerializedName("text") val text: String,
    @SerializedName("sourceLang") val sourceLang: String = "English",
    @SerializedName("targetLang") val targetLang: String = "Hindi",
    @SerializedName("context") val context: String? = null
)

data class TranslateResponse(
    @SerializedName("translatedText") val translatedText: String,
    @SerializedName("phonetic") val phonetic: String,
    @SerializedName("literalMeaning") val literalMeaning: String,
    @SerializedName("culturalTip") val culturalTip: String,
    @SerializedName("sourceLang") val sourceLang: String,
    @SerializedName("targetLang") val targetLang: String
)
