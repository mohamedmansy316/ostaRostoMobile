package com.ostarosto.app.core.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.parameter
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.isSuccess
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull

/**
 * Wraps [HttpClient] and unpacks the Laravel `ApiResponse` envelope into
 * [ApiResult]. All repository calls go through here.
 */
class ApiClient(
    val http: HttpClient,
    private val json: Json = HttpClientFactory.json,
    /** Invoked whenever the server answers 401 — wired to clear the session. */
    private val onUnauthorized: () -> Unit = {},
) {

    suspend fun <T> get(
        path: String,
        serializer: KSerializer<T>,
        query: Map<String, Any?> = emptyMap(),
    ): ApiResult<T> = call(HttpMethod.Get, path, serializer, query = query)

    suspend fun <T> post(
        path: String,
        serializer: KSerializer<T>,
        body: Any? = null,
    ): ApiResult<T> = call(HttpMethod.Post, path, serializer, body = body)

    suspend fun <T> delete(
        path: String,
        serializer: KSerializer<T>,
        body: Any? = null,
    ): ApiResult<T> = call(HttpMethod.Delete, path, serializer, body = body)

    /** For endpoints where only success/failure matters. */
    suspend fun postUnit(path: String, body: Any? = null): ApiResult<Unit> =
        callUnit(HttpMethod.Post, path, body)

    suspend fun deleteUnit(path: String, body: Any? = null): ApiResult<Unit> =
        callUnit(HttpMethod.Delete, path, body)

    private sealed interface Exchange {
        data class Ok(val response: HttpResponse, val envelope: RawEnvelope) : Exchange
        data class Failed(val cause: Throwable) : Exchange
    }

    private suspend fun <T> call(
        method: HttpMethod,
        path: String,
        serializer: KSerializer<T>,
        body: Any? = null,
        query: Map<String, Any?> = emptyMap(),
    ): ApiResult<T> = when (val ex = exchange(method, path, body, query)) {
        is Exchange.Failed -> ApiResult.NetworkError(ex.cause)
        is Exchange.Ok -> {
            val (response, raw) = ex.response to ex.envelope
            when {
                !response.status.isSuccess() || !raw.success ->
                    httpError(response.status.value, raw)

                raw.data == null || raw.data is JsonNull ->
                    ApiResult.HttpError(response.status.value, "Empty response body")

                else -> runCatching { json.decodeFromJsonElement(serializer, raw.data) }.fold(
                    onSuccess = { ApiResult.Success(it) },
                    onFailure = { ApiResult.HttpError(response.status.value, "Malformed response: ${it.message}") },
                )
            }
        }
    }

    private suspend fun callUnit(method: HttpMethod, path: String, body: Any?): ApiResult<Unit> =
        when (val ex = exchange(method, path, body, emptyMap())) {
            is Exchange.Failed -> ApiResult.NetworkError(ex.cause)
            is Exchange.Ok ->
                if (ex.response.status.isSuccess() && ex.envelope.success) {
                    ApiResult.Success(Unit)
                } else {
                    httpError(ex.response.status.value, ex.envelope)
                }
        }

    private suspend fun exchange(
        method: HttpMethod,
        path: String,
        body: Any?,
        query: Map<String, Any?>,
    ): Exchange {
        val response: HttpResponse
        val text: String
        try {
            response = http.request(path) {
                this.method = method
                query.forEach { (key, value) -> if (value != null) parameter(key, value) }
                if (body != null) setBody(body)
            }
            // Reading the body can also fail (connection reset, read timeout,
            // charset) — keep it inside the same guard so it becomes a
            // NetworkError instead of an uncaught crash.
            text = response.bodyAsText()
        } catch (c: kotlinx.coroutines.CancellationException) {
            throw c
        } catch (t: Throwable) {
            return Exchange.Failed(t)
        }

        if (response.status.value == 401) onUnauthorized()

        val envelope = runCatching { json.decodeFromString(RawEnvelope.serializer(), text) }
            .getOrDefault(RawEnvelope(success = response.status.isSuccess()))
        return Exchange.Ok(response, envelope)
    }

    private fun httpError(status: Int, raw: RawEnvelope) = ApiResult.HttpError(
        status = status,
        message = raw.firstError() ?: "Request failed ($status)",
        fieldErrors = raw.errors ?: emptyMap(),
    )

    /** Decode a bare (non-enveloped) payload, e.g. the /ping route. */
    suspend inline fun <reified T> raw(path: String): T = http.request(path).body()
}
