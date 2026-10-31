package `in`.hiddenindia.app.network

import com.google.gson.Gson
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException

private val sharedGson = Gson()

/**
 * Runs a Retrofit call and converts it into an ApiResult, distinguishing
 * no-internet / timeout / HTTP error / malformed response instead of letting
 * exceptions or raw stack traces reach the UI. Shared by every repository
 * (AuthRepository, AiRepository, ...) so error handling stays consistent
 * and there is exactly one place that decides what counts as which failure.
 */
suspend fun <T> safeApiCall(block: suspend () -> Response<T>): ApiResult<T> {
    return try {
        val response = block()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                ApiResult.Success(body)
            } else {
                ApiResult.Failure(ApiError.Unexpected("Empty response from server."))
            }
        } else {
            val errorBodyString = response.errorBody()?.string()
            val parsedError = try {
                if (!errorBodyString.isNullOrBlank()) {
                    sharedGson.fromJson(errorBodyString, ErrorResponseBody::class.java)
                } else null
            } catch (e: Exception) {
                null
            }
            ApiResult.Failure(
                ApiError.Http(
                    code = response.code(),
                    notFound = parsedError?.notFound == true,
                    serverMessage = parsedError?.error
                )
            )
        }
    } catch (e: SocketTimeoutException) {
        ApiResult.Failure(ApiError.Timeout)
    } catch (e: IOException) {
        ApiResult.Failure(ApiError.NoInternet)
    } catch (e: Exception) {
        ApiResult.Failure(ApiError.Unexpected(e.message ?: "Unknown error"))
    }
}
