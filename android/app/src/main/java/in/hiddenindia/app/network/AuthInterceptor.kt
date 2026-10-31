package `in`.hiddenindia.app.network

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Attaches `Authorization: Bearer <token>` to every outgoing request when a
 * session token is present in secure storage. If there is no stored token,
 * the request is sent unmodified (e.g. signup/login themselves don't need
 * one). The token value is never logged here or anywhere else.
 */
class AuthInterceptor(private val tokenStorage: TokenStorage) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val token = tokenStorage.getToken()

        val request = if (!token.isNullOrBlank()) {
            original.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            original
        }

        return chain.proceed(request)
    }
}
