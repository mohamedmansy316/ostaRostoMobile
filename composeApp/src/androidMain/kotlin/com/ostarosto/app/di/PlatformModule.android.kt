package com.ostarosto.app.di

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.ostarosto.app.core.platform.AndroidUrlOpener
import com.ostarosto.app.core.platform.UrlOpener
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import io.github.aakira.napier.Napier
import org.koin.core.module.Module
import org.koin.dsl.module

private const val SECURE_PREFS = "osta_rosto_secure"

actual val platformModule: Module = module {
    single<UrlOpener> { AndroidUrlOpener(get<Context>()) }
    single<Settings> { SharedPreferencesSettings(securePrefs(get<Context>())) }
}

/**
 * EncryptedSharedPreferences, with a defensive fallback. On some OEM devices the
 * Keystore is broken, and after a cloud-backup restore the master key no longer
 * matches the ciphertext — both make `.create()` (or the first read) throw. Rather
 * than failing Koin init and crash-looping on launch, wipe the corrupt file and
 * retry once, then fall back to plain prefs so the app still starts (the user is
 * simply signed out).
 */
private fun securePrefs(context: Context): SharedPreferences {
    fun build(): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            SECURE_PREFS,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    return try {
        build()
    } catch (e: Throwable) {
        Napier.w(tag = "secure-prefs", message = "EncryptedSharedPreferences unavailable, recreating", throwable = e)
        context.deleteSharedPreferences(SECURE_PREFS)
        try {
            build()
        } catch (e2: Throwable) {
            Napier.e(tag = "secure-prefs", message = "Falling back to plain prefs", throwable = e2)
            context.getSharedPreferences("${SECURE_PREFS}_fallback", Context.MODE_PRIVATE)
        }
    }
}
