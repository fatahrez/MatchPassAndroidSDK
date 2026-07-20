package africa.matchpass.sdk.internal

import retrofit2.HttpException
import java.io.IOException

/**
 * Plain-language message for an HTTP status code — never echoes the raw response body or
 * reason phrase back to the user (e.g. "HTTP 400 Bad Request"), which is jargon they can't
 * act on.
 */
internal fun httpStatusToFriendlyMessage(code: Int): String = when (code) {
    400 -> "There was a problem with your request. Please check your details and try again."
    401, 403 -> "Your session isn't valid. Please log in again."
    404 -> "We couldn't find what you were looking for."
    408 -> "This is taking longer than expected. Please check your connection and try again."
    429 -> "Too many attempts. Please wait a moment and try again."
    in 500..599 -> "Something went wrong on our end. Please try again in a moment."
    else -> "Something went wrong. Please try again."
}

internal fun networkFailureMessage(): String =
    "Network error. Please check your connection and try again."

/** Maps any throwable from a Retrofit call to a message safe to show a user directly. */
internal fun Throwable.toFriendlyMessage(): String = when (this) {
    is HttpException -> httpStatusToFriendlyMessage(code())
    is IOException -> networkFailureMessage()
    else -> "Something went wrong. Please try again."
}
