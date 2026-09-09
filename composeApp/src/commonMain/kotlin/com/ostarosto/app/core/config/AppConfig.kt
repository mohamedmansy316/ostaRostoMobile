package com.ostarosto.app.core.config

import com.ostarosto.app.config.BuildKonfig

/**
 * Runtime configuration. Values are injected at build time by BuildKonfig
 * (see composeApp/build.gradle.kts). Use `-Pbuildkonfig.flavor=prod` for
 * a production build.
 */
object AppConfig {
    val baseUrl: String = BuildKonfig.BASE_URL
    val environment: String = BuildKonfig.ENV
    val isDebug: Boolean = environment != "prod"

    /** Deep link scheme handled by the app: ostarosto://order/{id} */
    const val DEEP_LINK_SCHEME: String = "ostarosto"
}
