package `in`.hiddenindia.app.network

import `in`.hiddenindia.app.data.GeneratedItinerary
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Endpoints mounted at /api/auth/ by backend/server.ts (`app.use('/api/auth', authRouter)`),
 * implemented in backend/server/authRoutes.ts. Only the auth/favorites/itinerary
 * endpoints are implemented here — AI, map, and offline-pack endpoints belong to
 * later stages.
 *
 * Every call returns a raw retrofit2.Response<T> (not a direct T) so callers can
 * distinguish HTTP success/failure and read the error body instead of Retrofit
 * throwing on non-2xx responses.
 */
interface ApiService {

    @POST("api/auth/signup")
    suspend fun signup(@Body body: SignupRequest): Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): Response<AuthResponse>

    @POST("api/auth/logout")
    suspend fun logout(): Response<SuccessResponse>

    @GET("api/auth/me")
    suspend fun getMe(): Response<UserEnvelopeResponse>

    @PATCH("api/auth/profile")
    suspend fun updateProfile(@Body body: ProfileUpdateRequest): Response<UserEnvelopeResponse>

    @POST("api/auth/password-reset")
    suspend fun requestPasswordReset(@Body body: PasswordResetRequest): Response<PasswordResetResponse>

    /**
     * TOGGLES the favorite for this destination on the authenticated account
     * (adds it if absent, removes it if present) — the backend has no
     * separate add/remove endpoint. Body is empty; nothing to send besides
     * the path parameter and the auth token.
     */
    @POST("api/auth/favorites/{destinationId}")
    suspend fun toggleFavorite(@Path("destinationId") destinationId: String): Response<FavoritesResponse>

    /** Body is the itinerary itself (must include a non-blank `id`, per authRoutes.ts). */
    @POST("api/auth/itineraries")
    suspend fun saveItinerary(@Body itinerary: GeneratedItinerary): Response<ItinerariesResponse>

    @DELETE("api/auth/itineraries/{itineraryId}")
    suspend fun deleteItinerary(@Path("itineraryId") itineraryId: String): Response<ItinerariesResponse>

    // ---- AI (backend/server.ts — none of these require authentication) ----

    @GET("api/ai/status")
    suspend fun getAiStatus(): Response<AiStatusResponse>

    @POST("api/ai/ask-place")
    suspend fun askPlace(@Body body: AskPlaceRequest): Response<AskPlaceResponse>

    @POST("api/ai/plan-trip")
    suspend fun planTrip(@Body body: PlanTripRequest): Response<PlanTripResponse>

    @POST("api/ai/translate")
    suspend fun translate(@Body body: TranslateRequest): Response<TranslateResponse>

    // Real nearest-hospital / nearest-police lookup for the SOS screen's "Current
    // Location (GPS)" mode (OpenStreetMap-backed — see server.ts). Returns nulls, never
    // an invented facility name, when nothing verified is found within range.
    @GET("api/geo/nearest-emergency")
    suspend fun getNearestEmergencyFacilities(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double
    ): Response<NearestEmergencyResponse>

    // Real, verified image lookup via Wikipedia's API (see server.ts) — used wherever a
    // place/dish in the destinations dataset has no hardcoded image, instead of falling
    // back to a generic/reused stock photo or the parent destination's cover image.
    @GET("api/images/resolve")
    suspend fun resolveImage(@Query("query") query: String, @Query("context") context: String?): Response<ImageResolveResponse>
}
