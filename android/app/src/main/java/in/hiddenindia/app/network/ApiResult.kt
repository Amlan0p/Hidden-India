package `in`.hiddenindia.app.network

/** Outcome of a single backend call, distinguishing success from every kind of failure. */
sealed class ApiResult<out T> {
    data class Success<out T>(val data: T) : ApiResult<T>()
    data class Failure(val error: ApiError) : ApiResult<Nothing>()
}

/**
 * A user-presentable classification of what went wrong. `message` is safe to
 * show directly in the UI — it is never a raw stack trace.
 */
sealed class ApiError(val message: String) {
    object NoInternet : ApiError("No internet connection. Please check your network and try again.")
    object Timeout : ApiError("The request timed out. Please try again.")

    /** A well-formed HTTP error response from the backend (4xx/5xx). */
    data class Http(val code: Int, val notFound: Boolean = false, val serverMessage: String?) :
        ApiError(serverMessage ?: defaultMessageFor(code))

    /** Anything else: malformed response, unexpected exception, etc. */
    data class Unexpected(val detail: String) : ApiError("Something went wrong. Please try again.")

    companion object {
        fun defaultMessageFor(code: Int): String = when (code) {
            400 -> "That request wasn't valid. Please check your input and try again."
            401 -> "You're not signed in, or your session has expired. Please log in again."
            403 -> "You don't have permission to do that."
            404 -> "We couldn't find what you were looking for."
            429 -> "AI service is temporarily busy. Please try again."
            in 500..599 -> "The server ran into a problem. Please try again in a moment."
            else -> "Something went wrong. Please try again."
        }
    }
}
