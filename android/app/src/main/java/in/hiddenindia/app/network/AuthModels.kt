package `in`.hiddenindia.app.network

import com.google.gson.annotations.SerializedName
import `in`.hiddenindia.app.data.GeneratedItinerary

/**
 * Request/response models below are matched field-for-field against the
 * actual backend implementation in backend/server/authRoutes.ts and
 * backend/server/db.ts (toPublicUser / PublicUser). Do not add or rename
 * fields without checking the backend source first.
 */

// ---- Requests ----

data class SignupRequest(
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("confirmPassword") val confirmPassword: String
)

data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class ProfileUpdateRequest(
    @SerializedName("name") val name: String? = null,
    @SerializedName("avatar") val avatar: String? = null,
    @SerializedName("role") val role: String? = null
)

data class PasswordResetRequest(
    @SerializedName("email") val email: String,
    @SerializedName("newPassword") val newPassword: String? = null
)

// ---- Responses ----

/**
 * Matches PublicUser from backend/server/db.ts:
 *   { id, name, email, avatar, role, favoriteDestinationIds, savedItineraries }
 *
 * savedItineraries is stored server-side as whatever JSON payload was POSTed
 * to /api/auth/itineraries and echoed back unchanged (see db.ts's
 * getItineraries/upsertItinerary), which is always a GeneratedItinerary as
 * produced by DestinationRepository — see network/ItineraryModels usage in
 * AuthRepository.saveItinerary().
 */
data class UserDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("avatar") val avatar: String? = null,
    @SerializedName("role") val role: String? = null,
    @SerializedName("favoriteDestinationIds") val favoriteDestinationIds: List<String> = emptyList(),
    @SerializedName("savedItineraries") val savedItineraries: List<GeneratedItinerary> = emptyList()
)

/**
 * Response of POST /api/auth/favorites/{destinationId} (backend/server/authRoutes.ts).
 * This endpoint TOGGLES the favorite (add if absent, remove if present) — the
 * backend has no separate add/remove or GET/list endpoint, so this response's
 * full, current list is also how the app discovers the up-to-date favorites.
 */
data class FavoritesResponse(
    @SerializedName("favoriteDestinationIds") val favoriteDestinationIds: List<String> = emptyList()
)

/**
 * Response of POST /api/auth/itineraries and DELETE /api/auth/itineraries/{itineraryId}
 * (backend/server/authRoutes.ts): the full, current list of the account's saved
 * itineraries after the change.
 */
data class ItinerariesResponse(
    @SerializedName("savedItineraries") val savedItineraries: List<GeneratedItinerary> = emptyList()
)

/** Response of POST /api/auth/signup and POST /api/auth/login: {success, token, user} */
data class AuthResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("token") val token: String,
    @SerializedName("user") val user: UserDto
)

/** Response of GET /api/auth/me and PATCH /api/auth/profile: {user} */
data class UserEnvelopeResponse(
    @SerializedName("user") val user: UserDto
)

/** Response of POST /api/auth/logout: {success} */
data class SuccessResponse(
    @SerializedName("success") val success: Boolean
)

/** Response of POST /api/auth/password-reset on success: {success, message} */
data class PasswordResetResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String? = null
)

/**
 * Shape of error bodies returned by the backend, e.g.
 * { "error": "Incorrect password...", "notFound": true }
 * `notFound` is only present on POST /api/auth/login's 404 case.
 * `details` is only present on POST /api/ai/ask-place's 500 case.
 */
data class ErrorResponseBody(
    @SerializedName("error") val error: String? = null,
    @SerializedName("notFound") val notFound: Boolean? = null,
    @SerializedName("details") val details: String? = null
)
