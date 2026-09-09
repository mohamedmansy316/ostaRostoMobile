package com.ostarosto.app.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Mirrors the Laravel `ApiResponse` envelope:
 * { "success": bool, "message": string?, "data": T?, "errors": {..}?, "meta": {..}? }
 */
@Serializable
data class ApiEnvelope<T>(
    val success: Boolean = false,
    val message: String? = null,
    val data: T? = null,
    val errors: Map<String, List<String>>? = null,
    val meta: PageMeta? = null,
)

@Serializable
data class PageMeta(
    @SerialName("current_page") val currentPage: Int = 1,
    @SerialName("per_page") val perPage: Int = 0,
    @SerialName("has_more") val hasMore: Boolean = false,
    val total: Int? = null,
)

/** Raw envelope used when we only need `success` / `message` / `errors`. */
@Serializable
data class RawEnvelope(
    val success: Boolean = false,
    val message: String? = null,
    val data: JsonElement? = null,
    val errors: Map<String, List<String>>? = null,
    val meta: PageMeta? = null,
)

fun RawEnvelope.firstError(): String? =
    errors?.values?.firstOrNull()?.firstOrNull() ?: message

val RawEnvelope.dataObject: JsonObject?
    get() = data as? JsonObject
