package com.ostarosto.app.core.auth

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set

/**
 * Persists the Sanctum bearer token in platform secure storage
 * (EncryptedSharedPreferences on Android, Keychain on iOS — the concrete
 * [Settings] is provided by the platform Koin module).
 *
 * [peek] is a synchronous in-memory read used by the Ktor `defaultRequest`
 * block; [save] / [clear] update both memory and disk.
 */
class TokenStore(private val settings: Settings) {

    private var cached: String? = settings[KEY]

    fun peek(): String? = cached

    fun save(token: String) {
        cached = token
        settings[KEY] = token
    }

    fun clear() {
        cached = null
        settings.remove(KEY)
    }

    val hasToken: Boolean get() = !cached.isNullOrBlank()

    private companion object {
        const val KEY = "sanctum_token"
    }
}
