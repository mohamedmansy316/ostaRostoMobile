package com.ostarosto.app.core.network

import com.ostarosto.app.core.auth.TokenStore
import com.ostarosto.app.core.config.AppConfig
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Builds the shared Ktor client. The platform engine (OkHttp on Android,
 * Darwin on iOS) is auto-selected because exactly one is on the classpath.
 */
object HttpClientFactory {

    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
        encodeDefaults = true
    }

    fun create(tokenStore: TokenStore): HttpClient = HttpClient {
        expectSuccess = false

        install(ContentNegotiation) {
            json(json)
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 30_000
        }

        if (AppConfig.isDebug) {
            install(Logging) {
                level = LogLevel.INFO
                logger = object : io.ktor.client.plugins.logging.Logger {
                    override fun log(message: String) = Napier.d(tag = "http", message = message)
                }
            }
        }

        defaultRequest {
            url(AppConfig.baseUrl.removeSuffix("/") + "/")
            header(HttpHeaders.Accept, ContentType.Application.Json.toString())
            header(HttpHeaders.AcceptLanguage, "ar")
            contentType(ContentType.Application.Json)

            tokenStore.peek()?.let { token ->
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        }
    }
}
