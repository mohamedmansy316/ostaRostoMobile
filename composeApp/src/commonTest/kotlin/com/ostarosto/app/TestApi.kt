package com.ostarosto.app

import com.ostarosto.app.core.auth.SessionManager
import com.ostarosto.app.core.auth.TokenStore
import com.ostarosto.app.core.network.ApiClient
import com.ostarosto.app.core.network.HttpClientFactory
import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json

/** A canned response the mock engine should answer a matching request with. */
class MockRoute(
    val matcher: (HttpRequestData) -> Boolean,
    val status: HttpStatusCode,
    val body: String,
)

fun ok(match: (HttpRequestData) -> Boolean, body: String) = MockRoute(match, HttpStatusCode.OK, body)
fun created(match: (HttpRequestData) -> Boolean, body: String) = MockRoute(match, HttpStatusCode.Created, body)
fun status(code: Int, match: (HttpRequestData) -> Boolean, body: String) =
    MockRoute(match, HttpStatusCode.fromValue(code), body)

fun pathIs(path: String): (HttpRequestData) -> Boolean = { it.url.encodedPath.endsWith(path) }

class TestApiScope(
    val api: ApiClient,
    val session: SessionManager,
    val tokenStore: TokenStore,
    val requests: List<HttpRequestData>,
)

/**
 * Build an [ApiClient] backed by a Ktor [MockEngine] that replies from [routes].
 * Recorded requests are exposed on the returned scope.
 */
fun testApi(vararg routes: MockRoute): TestApiScope {
    val recorded = mutableListOf<HttpRequestData>()

    val engine = MockEngine { request ->
        recorded += request
        val route = routes.firstOrNull { it.matcher(request) }
            ?: error("No mock route for ${request.method.value} ${request.url.encodedPath}")
        respond(
            content = route.body,
            status = route.status,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
        )
    }

    val client = HttpClient(engine) {
        install(ContentNegotiation) { json(HttpClientFactory.json) }
        defaultRequest { url("http://localhost/api/v1/") }
    }

    val tokenStore = TokenStore(MapSettings())
    return TestApiScope(
        api = ApiClient(client),
        session = SessionManager(tokenStore),
        tokenStore = tokenStore,
        requests = recorded,
    )
}

fun HttpRequestData.bodyText(): String = (body as? TextContent)?.text ?: ""
