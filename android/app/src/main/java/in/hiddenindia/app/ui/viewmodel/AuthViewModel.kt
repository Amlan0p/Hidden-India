package `in`.hiddenindia.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import `in`.hiddenindia.app.data.AuthRepository
import `in`.hiddenindia.app.data.GeneratedItinerary
import `in`.hiddenindia.app.network.ApiError
import `in`.hiddenindia.app.network.ApiResult
import `in`.hiddenindia.app.network.UserDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Overall app-wide authentication state, driving which nav graph is shown. */
sealed class AuthState {
    /** Checking secure storage / verifying a stored token with the backend. */
    object Loading : AuthState()
    data class Authenticated(val user: UserDto) : AuthState()
    object Unauthenticated : AuthState()

    /**
     * A token exists locally but we couldn't verify it (no internet, timeout,
     * or an unexpected server error) — NOT the same as an invalid/expired
     * token (that maps to Unauthenticated after a 401). Lets the user retry
     * without silently discarding a possibly-still-valid session.
     */
    data class SessionCheckFailed(val error: ApiError) : AuthState()
}

/** State of an in-progress login or signup form submission. */
sealed class AuthFormState {
    object Idle : AuthFormState()
    object Loading : AuthFormState()
    data class Error(val message: String, val notFound: Boolean = false) : AuthFormState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AuthRepository(application)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState

    private val _loginFormState = MutableStateFlow<AuthFormState>(AuthFormState.Idle)
    val loginFormState: StateFlow<AuthFormState> = _loginFormState

    private val _signupFormState = MutableStateFlow<AuthFormState>(AuthFormState.Idle)
    val signupFormState: StateFlow<AuthFormState> = _signupFormState

    // ---- Account-synced favorites & itineraries (Stage 4) ----
    // Single source of truth is the UserDto embedded in AuthState.Authenticated,
    // which is exactly what the backend returned from /me, /login, /signup, the
    // favorites toggle, or an itinerary save/delete. There is no separate local
    // cache to keep in sync or to leak between accounts: logging out simply
    // switches authState to Unauthenticated, and logging in as someone else
    // replaces it with that account's fresh UserDto.

    val favoriteIds: StateFlow<Set<String>> = authState
        .map { (it as? AuthState.Authenticated)?.user?.favoriteDestinationIds?.toSet() ?: emptySet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    /** Exposed for chrome that needs the logged-in user's name/avatar (HiddenIndiaTopBar, ProfileScreen). */
    val currentUser: StateFlow<UserDto?> = authState
        .map { (it as? AuthState.Authenticated)?.user }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val savedItineraries: StateFlow<List<GeneratedItinerary>> = authState
        .map { (it as? AuthState.Authenticated)?.user?.savedItineraries ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Destination IDs currently mid-toggle, so the UI can show a per-item spinner. */
    private val _pendingFavoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val pendingFavoriteIds: StateFlow<Set<String>> = _pendingFavoriteIds

    private val _favoriteError = MutableStateFlow<String?>(null)
    val favoriteError: StateFlow<String?> = _favoriteError

    private val _itineraryActionState = MutableStateFlow<AuthFormState>(AuthFormState.Idle)
    val itineraryActionState: StateFlow<AuthFormState> = _itineraryActionState

    private val _profileActionState = MutableStateFlow<AuthFormState>(AuthFormState.Idle)
    val profileActionState: StateFlow<AuthFormState> = _profileActionState

    /** Itinerary IDs currently mid-delete, so the UI can show a per-item spinner. */
    private val _pendingDeleteItineraryIds = MutableStateFlow<Set<String>>(emptySet())
    val pendingDeleteItineraryIds: StateFlow<Set<String>> = _pendingDeleteItineraryIds

    init {
        restoreSession()
    }

    /** Step 1: no token → Unauthenticated. Step 2: token present → restore cached user immediately & verify via /api/auth/me. */
    fun restoreSession() {
        if (!repository.hasStoredToken()) {
            _authState.value = AuthState.Unauthenticated
            return
        }

        val cachedUser = repository.getCachedUser()
        if (cachedUser != null) {
            _authState.value = AuthState.Authenticated(cachedUser)
        } else {
            _authState.value = AuthState.Loading
        }

        viewModelScope.launch {
            when (val result = repository.getCurrentUser()) {
                is ApiResult.Success -> {
                    _authState.value = AuthState.Authenticated(result.data.user)
                }
                is ApiResult.Failure -> {
                    val error = result.error
                    if (error is ApiError.Http && error.code == 401) {
                        // Stored token is no longer valid on the backend.
                        repository.clearLocalSession()
                        _authState.value = AuthState.Unauthenticated
                    } else {
                        // Network hiccup or server error — don't throw away a
                        // possibly-valid session; use cached profile if available.
                        if (cachedUser != null) {
                            _authState.value = AuthState.Authenticated(cachedUser)
                        } else {
                            _authState.value = AuthState.SessionCheckFailed(error)
                        }
                    }
                }
            }
        }
    }

    fun login(email: String, password: String) {
        _loginFormState.value = AuthFormState.Loading
        viewModelScope.launch {
            when (val result = repository.login(email, password)) {
                is ApiResult.Success -> {
                    _loginFormState.value = AuthFormState.Idle
                    _authState.value = AuthState.Authenticated(result.data.user)
                }
                is ApiResult.Failure -> {
                    val notFound = (result.error as? `in`.hiddenindia.app.network.ApiError.Http)?.notFound == true
                    _loginFormState.value = AuthFormState.Error(result.error.message, notFound = notFound)
                }
            }
        }
    }

    private val _forgotPasswordState = MutableStateFlow<AuthFormState>(AuthFormState.Idle)
    val forgotPasswordState: StateFlow<AuthFormState> = _forgotPasswordState
    private val _forgotPasswordMessage = MutableStateFlow<String?>(null)
    val forgotPasswordMessage: StateFlow<String?> = _forgotPasswordMessage

    /**
     * Port of web AuthContext.resetPassword — calls the existing PATCH-style reset
     * endpoint (ApiService.requestPasswordReset / AuthRepository.requestPasswordReset
     * already existed; only the ViewModel call and UI were missing).
     */
    fun resetPassword(email: String, newPassword: String?) {
        _forgotPasswordState.value = AuthFormState.Loading
        _forgotPasswordMessage.value = null
        viewModelScope.launch {
            when (val result = repository.requestPasswordReset(email, newPassword)) {
                is ApiResult.Success -> {
                    _forgotPasswordState.value = AuthFormState.Idle
                    _forgotPasswordMessage.value = result.data.message ?: "Password reset link sent to your email."
                }
                is ApiResult.Failure -> {
                    _forgotPasswordState.value = AuthFormState.Error(result.error.message)
                }
            }
        }
    }

    fun clearForgotPasswordStatus() {
        _forgotPasswordState.value = AuthFormState.Idle
        _forgotPasswordMessage.value = null
    }

    fun signup(name: String, email: String, password: String, confirmPassword: String) {
        _signupFormState.value = AuthFormState.Loading
        viewModelScope.launch {
            when (val result = repository.signup(name, email, password, confirmPassword)) {
                is ApiResult.Success -> {
                    _signupFormState.value = AuthFormState.Idle
                    val userWithDefaultAvatar = result.data.user
                    _authState.value = AuthState.Authenticated(userWithDefaultAvatar)
                    // Persist the 1st avatar on the backend as well
                    launch {
                        repository.updateProfile(
                            name = userWithDefaultAvatar.name,
                            avatar = userWithDefaultAvatar.avatar,
                            role = userWithDefaultAvatar.role ?: "Cultural Connoisseur"
                        )
                    }
                }
                is ApiResult.Failure -> {
                    _signupFormState.value = AuthFormState.Error(result.error.message)
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout() // local token is cleared by the repository regardless of the network outcome
            _authState.value = AuthState.Unauthenticated
        }
    }

    fun clearLoginError() {
        _loginFormState.value = AuthFormState.Idle
    }

    fun clearSignupError() {
        _signupFormState.value = AuthFormState.Idle
    }

    /** Toggles the favorite for [destinationId]. Requires an authenticated session. */
    fun toggleFavorite(destinationId: String) {
        val user = (authState.value as? AuthState.Authenticated)?.user
        if (user == null) {
            _favoriteError.value = "Please log in to save favorites."
            return
        }
        if (_pendingFavoriteIds.value.contains(destinationId)) return // already in flight

        _pendingFavoriteIds.value = _pendingFavoriteIds.value + destinationId
        viewModelScope.launch {
            when (val result = repository.toggleFavorite(destinationId)) {
                is ApiResult.Success -> {
                    _favoriteError.value = null
                    val current = (authState.value as? AuthState.Authenticated)?.user
                    if (current != null) {
                        _authState.value = AuthState.Authenticated(
                            current.copy(favoriteDestinationIds = result.data.favoriteDestinationIds)
                        )
                    }
                }
                is ApiResult.Failure -> {
                    _favoriteError.value = result.error.message
                    handleIfUnauthorized(result.error)
                }
            }
            _pendingFavoriteIds.value = _pendingFavoriteIds.value - destinationId
        }
    }

    /** Saves a generated itinerary to the authenticated account. Requires an authenticated session. */
    fun saveItineraryToAccount(itinerary: GeneratedItinerary) {
        val user = (authState.value as? AuthState.Authenticated)?.user
        if (user == null) {
            _itineraryActionState.value = AuthFormState.Error("Please log in to save itineraries.")
            return
        }

        _itineraryActionState.value = AuthFormState.Loading
        viewModelScope.launch {
            when (val result = repository.saveItinerary(itinerary)) {
                is ApiResult.Success -> {
                    _itineraryActionState.value = AuthFormState.Idle
                    val current = (authState.value as? AuthState.Authenticated)?.user
                    if (current != null) {
                        _authState.value = AuthState.Authenticated(
                            current.copy(savedItineraries = result.data.savedItineraries)
                        )
                    }
                }
                is ApiResult.Failure -> {
                    _itineraryActionState.value = AuthFormState.Error(result.error.message)
                    handleIfUnauthorized(result.error)
                }
            }
        }
    }

    /**
     * Port of web AuthContext.updateProfile — calls the existing PATCH /api/auth/profile
     * endpoint (ApiService.updateProfile / AuthRepository.updateProfile were already wired
     * up to the backend; only the ViewModel-level call and UI were missing).
     */
    fun updateProfile(name: String, role: String, avatar: String) {
        _profileActionState.value = AuthFormState.Loading
        viewModelScope.launch {
            when (val result = repository.updateProfile(name, avatar, role)) {
                is ApiResult.Success -> {
                    _profileActionState.value = AuthFormState.Idle
                    _authState.value = AuthState.Authenticated(result.data.user)
                }
                is ApiResult.Failure -> {
                    _profileActionState.value = AuthFormState.Error(result.error.message)
                    handleIfUnauthorized(result.error)
                }
            }
        }
    }

    fun deleteItineraryFromAccount(itineraryId: String) {
        val user = (authState.value as? AuthState.Authenticated)?.user
        if (user == null) {
            _itineraryActionState.value = AuthFormState.Error("Please log in to manage your itineraries.")
            return
        }
        if (_pendingDeleteItineraryIds.value.contains(itineraryId)) return // already in flight

        _pendingDeleteItineraryIds.value = _pendingDeleteItineraryIds.value + itineraryId
        viewModelScope.launch {
            when (val result = repository.deleteItinerary(itineraryId)) {
                is ApiResult.Success -> {
                    val current = (authState.value as? AuthState.Authenticated)?.user
                    if (current != null) {
                        _authState.value = AuthState.Authenticated(
                            current.copy(savedItineraries = result.data.savedItineraries)
                        )
                    }
                }
                is ApiResult.Failure -> {
                    _itineraryActionState.value = AuthFormState.Error(result.error.message)
                    handleIfUnauthorized(result.error)
                }
            }
            _pendingDeleteItineraryIds.value = _pendingDeleteItineraryIds.value - itineraryId
        }
    }

    /** On a 401 (expired/invalid session), clear the token and drop to Unauthenticated — no retry loop. */
    private fun handleIfUnauthorized(error: ApiError) {
        if (error is ApiError.Http && error.code == 401) {
            repository.clearLocalSession()
            _authState.value = AuthState.Unauthenticated
        }
    }
}
