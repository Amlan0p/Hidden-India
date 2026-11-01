package `in`.hiddenindia.app.data

import android.content.Context
import `in`.hiddenindia.app.network.AiChatHistoryMessage
import `in`.hiddenindia.app.network.AiStatusResponse
import `in`.hiddenindia.app.network.ApiClient
import `in`.hiddenindia.app.network.ApiResult
import `in`.hiddenindia.app.network.ApiService
import `in`.hiddenindia.app.network.AskPlaceRequest
import `in`.hiddenindia.app.network.AskPlaceResponse
import `in`.hiddenindia.app.network.PlanTripRequest
import `in`.hiddenindia.app.network.PlanTripResponse
import `in`.hiddenindia.app.network.TranslateRequest
import `in`.hiddenindia.app.network.TranslateResponse
import `in`.hiddenindia.app.network.safeApiCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * AI Repository for Hidden India.
 * All AI capabilities (trip planner, Ask-the-Place, translator, AI status)
 * are served through the secure Render backend (/api/ai/ endpoints).
 * The backend manages Groq API credentials securely in its server environment;
 * no API keys or third-party AI secrets are embedded in the Android client.
 */
class AiRepository(context: Context) {

    private val api: ApiService = ApiClient.create(context.applicationContext)

    /**
     * Checks AI service health and availability from the backend.
     */
    suspend fun getStatus(): ApiResult<AiStatusResponse> = withContext(Dispatchers.IO) {
        safeApiCall { api.getAiStatus() }
    }

    /**
     * Ask-the-Place cultural travel queries and conversational guide.
     */
    suspend fun askPlace(
        destinationId: String?,
        question: String,
        chatHistory: List<AiChatHistoryMessage> = emptyList()
    ): ApiResult<AskPlaceResponse> = withContext(Dispatchers.IO) {
        safeApiCall {
            api.askPlace(
                AskPlaceRequest(
                    destinationId = destinationId,
                    question = question,
                    chatHistory = chatHistory
                )
            )
        }
    }

    /**
     * Multi-day personalized itinerary planner with grounded destination activities.
     */
    suspend fun planTrip(
        destinationId: String,
        days: Int,
        budgetStyle: String,
        interests: List<String>,
        travelStyle: String = "Authentic & Immersive"
    ): ApiResult<PlanTripResponse> = withContext(Dispatchers.IO) {
        safeApiCall {
            api.planTrip(
                PlanTripRequest(
                    destinationId = destinationId,
                    days = days,
                    budgetStyle = budgetStyle,
                    interests = interests,
                    travelStyle = travelStyle
                )
            )
        }
    }

    /**
     * Cultural linguist translator with phonetic transliteration and etiquette tips.
     */
    suspend fun translate(
        text: String,
        sourceLang: String = "English",
        targetLang: String = "Hindi"
    ): ApiResult<TranslateResponse> = withContext(Dispatchers.IO) {
        safeApiCall {
            api.translate(
                TranslateRequest(
                    text = text,
                    sourceLang = sourceLang,
                    targetLang = targetLang
                )
            )
        }
    }
}
