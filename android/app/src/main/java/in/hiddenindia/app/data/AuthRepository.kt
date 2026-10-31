package `in`.hiddenindia.app.data

import android.content.Context
import `in`.hiddenindia.app.network.ApiClient
import `in`.hiddenindia.app.network.ApiResult
import `in`.hiddenindia.app.network.ApiService
import `in`.hiddenindia.app.network.AuthResponse
import `in`.hiddenindia.app.network.FavoritesResponse
import `in`.hiddenindia.app.network.ItinerariesResponse
import `in`.hiddenindia.app.network.LoginRequest
import `in`.hiddenindia.app.network.PasswordResetRequest
import `in`.hiddenindia.app.network.PasswordResetResponse
import `in`.hiddenindia.app.network.ProfileUpdateRequest
import `in`.hiddenindia.app.network.SignupRequest
import `in`.hiddenindia.app.network.SuccessResponse
import `in`.hiddenindia.app.network.TokenStorage
import `in`.hiddenindia.app.network.UserDto
import `in`.hiddenindia.app.network.UserEnvelopeResponse
import `in`.hiddenindia.app.network.safeApiCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Talks to the real backend's /api/auth/ endpoints (backend/server/authRoutes.ts).
 * No local/fake authentication, no simulated delays — every call here is a real
 * network request. Error classification is shared via network/ApiCallUtils.safeApiCall
 * (also used by AiRepository) so every repository fails the same way.
 */
import `in`.hiddenindia.app.ui.theme.DEFAULT_AVATAR_SVG
import `in`.hiddenindia.app.ui.theme.normalizeAvatarSvg

class AuthRepository(context: Context) {

    private val appContext = context.applicationContext
    private val api: ApiService = ApiClient.create(appContext)
    private val tokenStorage = TokenStorage(appContext)

    fun hasStoredToken(): Boolean = tokenStorage.hasToken()

    /** Clears the local session without calling the backend. Used after a 401 during session restore. */
    fun clearLocalSession() = tokenStorage.clearToken()

    fun getCachedUser(): UserDto? = tokenStorage.getCachedUser()
    fun getCustomAvatar(): String? = tokenStorage.getAvatar()
    fun saveCustomAvatar(avatar: String) = tokenStorage.saveAvatar(avatar)

    suspend fun signup(
        name: String,
        email: String,
        password: String,
        confirmPassword: String
    ): ApiResult<AuthResponse> = withContext(Dispatchers.IO) {
        val result = safeApiCall { api.signup(SignupRequest(name, email, password, confirmPassword)) }
        if (result is ApiResult.Success) {
            tokenStorage.saveToken(result.data.token)
            val user = result.data.user
            val effectiveAvatar = normalizeAvatarSvg(user.avatar)
            val updatedUser = user.copy(avatar = effectiveAvatar)
            tokenStorage.saveUser(updatedUser)
            tokenStorage.saveAvatar(effectiveAvatar)
        }
        result
    }

    suspend fun login(email: String, password: String): ApiResult<AuthResponse> = withContext(Dispatchers.IO) {
        val result = safeApiCall { api.login(LoginRequest(email, password)) }
        if (result is ApiResult.Success) {
            tokenStorage.saveToken(result.data.token)
            val user = result.data.user
            // If user has a locally preserved avatar from a previous device session, keep it;
            // otherwise normalize and default to 1st preset.
            val localAvatar = tokenStorage.getAvatar()
            val effectiveAvatar = when {
                !localAvatar.isNullOrBlank() -> localAvatar
                !user.avatar.isNullOrBlank() -> normalizeAvatarSvg(user.avatar)
                else -> DEFAULT_AVATAR_SVG
            }
            val updatedUser = user.copy(avatar = effectiveAvatar)
            tokenStorage.saveUser(updatedUser)
            tokenStorage.saveAvatar(effectiveAvatar)
        }
        result
    }

    /** Calls the backend logout endpoint, then always clears the local token regardless of the result. */
    suspend fun logout(): ApiResult<SuccessResponse> = withContext(Dispatchers.IO) {
        val result = safeApiCall { api.logout() }
        tokenStorage.clearToken()
        result
    }

    suspend fun getCurrentUser(): ApiResult<UserEnvelopeResponse> = withContext(Dispatchers.IO) {
        val result = safeApiCall { api.getMe() }
        if (result is ApiResult.Success) {
            val user = result.data.user
            val localAvatar = tokenStorage.getAvatar()
            val effectiveAvatar = when {
                !localAvatar.isNullOrBlank() -> localAvatar
                !user.avatar.isNullOrBlank() -> normalizeAvatarSvg(user.avatar)
                else -> DEFAULT_AVATAR_SVG
            }
            val updatedUser = user.copy(avatar = effectiveAvatar)
            tokenStorage.saveUser(updatedUser)
            tokenStorage.saveAvatar(effectiveAvatar)
            ApiResult.Success(UserEnvelopeResponse(updatedUser))
        } else {
            result
        }
    }

    suspend fun updateProfile(
        name: String? = null,
        avatar: String? = null,
        role: String? = null
    ): ApiResult<UserEnvelopeResponse> = withContext(Dispatchers.IO) {
        val avatarToSave = avatar?.let { normalizeAvatarSvg(it) }
        val result = safeApiCall { api.updateProfile(ProfileUpdateRequest(name, avatarToSave, role)) }
        if (result is ApiResult.Success) {
            val user = result.data.user
            val resolvedAvatar = avatarToSave ?: normalizeAvatarSvg(user.avatar)
            val updatedUser = user.copy(avatar = resolvedAvatar)
            tokenStorage.saveUser(updatedUser)
            tokenStorage.saveAvatar(resolvedAvatar)
            ApiResult.Success(UserEnvelopeResponse(updatedUser))
        } else {
            // Even if offline, save updated avatar and profile to local storage so user doesn't lose it!
            if (!avatarToSave.isNullOrBlank()) {
                tokenStorage.saveAvatar(avatarToSave)
            }
            val cached = tokenStorage.getCachedUser()
            if (cached != null) {
                val updatedCached = cached.copy(
                    name = name ?: cached.name,
                    role = role ?: cached.role,
                    avatar = avatarToSave ?: cached.avatar
                )
                tokenStorage.saveUser(updatedCached)
            }
            result
        }
    }

    suspend fun requestPasswordReset(
        email: String,
        newPassword: String? = null
    ): ApiResult<PasswordResetResponse> = withContext(Dispatchers.IO) {
        safeApiCall { api.requestPasswordReset(PasswordResetRequest(email, newPassword)) }
    }

    /** Toggles the favorite for [destinationId] on the authenticated account. */
    suspend fun toggleFavorite(destinationId: String): ApiResult<FavoritesResponse> = withContext(Dispatchers.IO) {
        safeApiCall { api.toggleFavorite(destinationId) }
    }

    /** Saves (or updates, if the id already exists) an itinerary on the authenticated account. */
    suspend fun saveItinerary(itinerary: GeneratedItinerary): ApiResult<ItinerariesResponse> = withContext(Dispatchers.IO) {
        safeApiCall { api.saveItinerary(itinerary) }
    }

    suspend fun deleteItinerary(itineraryId: String): ApiResult<ItinerariesResponse> = withContext(Dispatchers.IO) {
        safeApiCall { api.deleteItinerary(itineraryId) }
    }
}
