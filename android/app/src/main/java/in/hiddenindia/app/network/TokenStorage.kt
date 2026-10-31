package `in`.hiddenindia.app.network

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Persists the backend session token securely on-device.
 *
 * Uses EncryptedSharedPreferences (Jetpack Security) rather than plain
 * SharedPreferences, satisfying the requirement to never store the auth
 * token unencrypted. Reads/writes are synchronous and cheap, which matters
 * because AuthInterceptor needs to read the token synchronously on OkHttp's
 * background thread for every request.
 *
 * IMPORTANT: never log the token. getToken()/saveToken() intentionally do
 * not log their values anywhere, and no calling code should either.
 */
class TokenStorage(context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context.applicationContext,
            "hidden_india_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun clearToken() {
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_USER_NAME)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_USER_ROLE)
            .remove(KEY_USER_AVATAR)
            .apply()
    }

    fun hasToken(): Boolean = !getToken().isNullOrBlank()

    fun saveUser(user: UserDto) {
        val editor = prefs.edit()
            .putString(KEY_USER_ID, user.id)
            .putString(KEY_USER_NAME, user.name)
            .putString(KEY_USER_EMAIL, user.email)
            .putString(KEY_USER_ROLE, user.role ?: "")
        if (!user.avatar.isNullOrBlank()) {
            editor.putString(KEY_USER_AVATAR, user.avatar)
        }
        editor.apply()
    }

    fun saveAvatar(avatar: String) {
        prefs.edit().putString(KEY_USER_AVATAR, avatar).apply()
    }

    fun getAvatar(): String? = prefs.getString(KEY_USER_AVATAR, null)

    fun getCachedUser(): UserDto? {
        val id = prefs.getString(KEY_USER_ID, null) ?: return null
        val name = prefs.getString(KEY_USER_NAME, "Explorer") ?: "Explorer"
        val email = prefs.getString(KEY_USER_EMAIL, "") ?: ""
        val role = prefs.getString(KEY_USER_ROLE, null)
        val avatar = prefs.getString(KEY_USER_AVATAR, null)
        return UserDto(
            id = id,
            name = name,
            email = email,
            avatar = avatar,
            role = role
        )
    }

    companion object {
        private const val KEY_TOKEN = "session_token"
        private const val KEY_USER_ID = "cached_user_id"
        private const val KEY_USER_NAME = "cached_user_name"
        private const val KEY_USER_EMAIL = "cached_user_email"
        private const val KEY_USER_ROLE = "cached_user_role"
        private const val KEY_USER_AVATAR = "cached_user_avatar"
    }
}
