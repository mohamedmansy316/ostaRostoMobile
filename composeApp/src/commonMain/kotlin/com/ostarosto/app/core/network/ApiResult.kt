package com.ostarosto.app.core.network

/**
 * Outcome of an API call. Repositories return this; ViewModels map it to UI state.
 */
sealed interface ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>

    /** Server responded with a non-2xx status or `success:false`. */
    data class HttpError(
        val status: Int,
        val message: String,
        val fieldErrors: Map<String, List<String>> = emptyMap(),
    ) : ApiResult<Nothing>

    /** Could not reach the server (no connectivity, timeout, TLS, …). */
    data class NetworkError(val cause: Throwable) : ApiResult<Nothing>
}

inline fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Success -> ApiResult.Success(transform(value))
    is ApiResult.HttpError -> this
    is ApiResult.NetworkError -> this
}

inline fun <T> ApiResult<T>.onSuccess(block: (T) -> Unit): ApiResult<T> {
    if (this is ApiResult.Success) block(value)
    return this
}

inline fun <T> ApiResult<T>.onFailure(block: (String) -> Unit): ApiResult<T> {
    when (this) {
        is ApiResult.HttpError -> block(message)
        is ApiResult.NetworkError -> block(cause.message ?: "Network error")
        is ApiResult.Success -> Unit
    }
    return this
}

fun <T> ApiResult<T>.getOrNull(): T? = (this as? ApiResult.Success)?.value

/** Returns the value or throws with the failure detail. Handy in tests. */
fun <T> ApiResult<T>.unwrap(): T = when (this) {
    is ApiResult.Success -> value
    is ApiResult.HttpError -> error("expected success, got HTTP $status: $message")
    is ApiResult.NetworkError -> error("expected success, got network error: ${cause.message}")
}
