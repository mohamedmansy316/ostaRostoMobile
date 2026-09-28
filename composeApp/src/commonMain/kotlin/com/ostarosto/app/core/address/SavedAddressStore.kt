package com.ostarosto.app.core.address

import com.ostarosto.app.domain.model.SavedAddress
import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Named delivery locations ("Home", "Work", ...), persisted on-device only —
 * no backend sync. Reuses the same [Settings] instance as [com.ostarosto.app.core.auth.TokenStore];
 * the payload is a handful of small records, so a second Keychain/EncryptedSharedPreferences
 * instance would be pure plumbing for no real benefit.
 *
 * StateFlow-backed (like [com.ostarosto.app.core.state.SelectionStore]) since several
 * independent screens — the checkout address picker, the manage-addresses sheet and the
 * app-launch geofence prompt — all need to reactively observe the same list.
 */
class SavedAddressStore(private val settings: Settings) {

    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(SavedAddress.serializer())

    private val _addresses = MutableStateFlow(load())
    val addresses: StateFlow<List<SavedAddress>> = _addresses.asStateFlow()

    fun default(): SavedAddress? = _addresses.value.firstOrNull { it.isDefault }

    fun add(
        label: String,
        latitude: Double,
        longitude: Double,
        addressText: String?,
        makeDefault: Boolean = false,
    ): SavedAddress {
        val shouldBeDefault = makeDefault || _addresses.value.isEmpty()
        val address = SavedAddress(
            id = randomId(),
            label = label,
            latitude = latitude,
            longitude = longitude,
            addressText = addressText,
            isDefault = shouldBeDefault,
        )
        persist(
            _addresses.value.map { if (shouldBeDefault) it.copy(isDefault = false) else it } + address,
        )
        return address
    }

    fun update(id: String, label: String, latitude: Double, longitude: Double, addressText: String?) {
        persist(
            _addresses.value.map {
                if (it.id == id) it.copy(label = label, latitude = latitude, longitude = longitude, addressText = addressText) else it
            },
        )
    }

    fun delete(id: String) {
        val remaining = _addresses.value.filterNot { it.id == id }
        // Promote another address to default if the one deleted held that flag.
        val wasDefault = _addresses.value.any { it.id == id && it.isDefault }
        persist(
            if (wasDefault && remaining.isNotEmpty()) {
                remaining.mapIndexed { index, a -> a.copy(isDefault = index == 0) }
            } else {
                remaining
            },
        )
    }

    fun setDefault(id: String) {
        persist(_addresses.value.map { it.copy(isDefault = it.id == id) })
    }

    private fun load(): List<SavedAddress> {
        val raw: String? = settings[KEY]
        return raw?.let {
            runCatching { json.decodeFromString(serializer, it) }.getOrDefault(emptyList())
        } ?: emptyList()
    }

    private fun persist(list: List<SavedAddress>) {
        _addresses.value = list
        settings[KEY] = json.encodeToString(serializer, list)
    }

    private fun randomId(): String {
        val chars = "0123456789abcdef"
        return (1..20).map { chars.random() }.joinToString("")
    }

    private companion object {
        const val KEY = "saved_addresses_v1"
    }
}
