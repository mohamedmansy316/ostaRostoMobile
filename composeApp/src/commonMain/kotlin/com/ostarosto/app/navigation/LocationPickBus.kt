package com.ostarosto.app.navigation

import com.ostarosto.app.domain.model.DeliveryAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Carries a confirmed map pin from [com.ostarosto.app.feature.location.LocationPickerScreen]
 * back to the checkout screen. Navigation-Compose here has no back-stack
 * result passing wired up anywhere, and `CartViewModel` isn't a shared
 * instance across routes (each destination gets its own via `koinViewModel()`)
 * — this one-shot bus mirrors [DeepLinkBus]'s existing pattern for signalling
 * into the graph from outside the current screen.
 */
object LocationPickBus {

    data class Result(val latitude: Double, val longitude: Double, val availability: DeliveryAvailability)

    private val _result = MutableStateFlow<Result?>(null)
    val result: StateFlow<Result?> = _result.asStateFlow()

    fun submit(latitude: Double, longitude: Double, availability: DeliveryAvailability) {
        _result.value = Result(latitude, longitude, availability)
    }

    /** Called once the checkout screen has applied the result. */
    fun consume() {
        _result.value = null
    }
}
