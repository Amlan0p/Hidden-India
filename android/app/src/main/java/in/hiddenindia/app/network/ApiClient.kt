package `in`.hiddenindia.app.network

import android.content.Context
import `in`.hiddenindia.app.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Builds the single Retrofit/OkHttp instance the app uses to reach the
 * Hidden India backend. Base URL comes from BuildConfig.API_BASE_URL,
 * configured in app/build.gradle (see the comment there) — this is the
 * ONE place that value is set.
 */
object ApiClient {

    @Volatile
    private var apiService: ApiService? = null

    /**
     * Returns the single shared ApiService instance for the whole app,
     * building it once on first call and reusing it after. Multiple
     * repositories (AuthRepository etc.) all call this rather than each
     * building their own OkHttpClient/Retrofit.
     */
    fun create(context: Context): ApiService {
        return apiService ?: synchronized(this) {
            apiService ?: build(context.applicationContext).also { apiService = it }
        }
    }

    private fun build(context: Context): ApiService {
        val tokenStorage = TokenStorage(context)

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
            redactHeader("Authorization") // never print the session token, even in debug logs
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenStorage))
            .addInterceptor(loggingInterceptor)
            // AI endpoints (plan-trip/ask-place/translate) can legitimately take
            // longer than a typical REST call while Groq/Gemini generate a
            // response, so the shared client's read/write/call timeouts are
            // sized for that rather than for the much-faster auth/account
            // calls, which comfortably finish well within these ceilings.
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(75, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(ApiService::class.java)
    }
}
