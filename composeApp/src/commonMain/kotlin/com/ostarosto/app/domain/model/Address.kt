package com.ostarosto.app.domain.model

import kotlinx.serialization.Serializable

/**
 * A customer-named delivery location (e.g. "Home", "Work"), persisted locally
 * on-device only — see [com.ostarosto.app.core.address.SavedAddressStore].
 */
@Serializable
data class SavedAddress(
    val id: String,
    val label: String,
    val latitude: Double,
    val longitude: Double,
    val addressText: String? = null,
    val isDefault: Boolean = false,
)
