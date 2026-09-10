package com.ostarosto.app.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Carries an inbound deep-link URI (e.g. `ostarosto://order/42`) from the
 * platform entry point to the Navigation-Compose graph. Covers both cold start
 * (Activity launched by the intent) and warm start (`onNewIntent`).
 */
object DeepLinkBus {

    private val _uri = MutableStateFlow<String?>(null)
    val uri: StateFlow<String?> = _uri.asStateFlow()

    fun submit(uri: String?) {
        if (!uri.isNullOrBlank()) _uri.value = uri
    }

    /** Called by the graph once the URI has been routed. */
    fun consume() {
        _uri.value = null
    }

    /** `ostarosto://order/{id}` -> id, or null if it is not an order link. */
    fun orderIdOf(uri: String): Long? {
        val marker = "order/"
        val i = uri.indexOf(marker)
        if (i < 0) return null
        return uri.substring(i + marker.length)
            .substringBefore('/')
            .substringBefore('?')
            .toLongOrNull()
    }
}
