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
 * Talks to the real backend's /api/ai/ endpoints (backend/server.ts).
 * No local/fake AI, no keyword matching, no simulated delays — every call
 * here is a real network request to Groq/Gemini via the backend. None of
 * these endpoints require authentication (confirmed from the backend
 * source — no requireAuth middleware on any /api/ai/ route), so this
 * repository doesn't touch TokenStorage directly; it reuses the same
 * shared ApiClient/AuthInterceptor as AuthRepository (which will still
 * attach a Bearer token if the user happens to be logged in — harmless,
 * the backend simply ignores it here).
 */
class AiRepository(context: Context) {

    private val api: ApiService = ApiClient.create(context.applicationContext)

    suspend fun getStatus(): ApiResult<AiStatusResponse> = withContext(Dispatchers.IO) {
        safeApiCall { api.getAiStatus() }
    }

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

    suspend fun translate(
        text: String,
        sourceLang: String = "English",
        targetLang: String = "Hindi"
    ): ApiResult<TranslateResponse> = withContext(Dispatchers.IO) {
        safeApiCall { api.translate(TranslateRequest(text = text, sourceLang = sourceLang, targetLang = targetLang)) }
    }
}
