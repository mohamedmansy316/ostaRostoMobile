package com.ostarosto.app.di

import com.ostarosto.app.core.platform.IosUrlOpener
import com.ostarosto.app.core.platform.UrlOpener
import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.KeychainSettings
import com.russhwolf.settings.Settings
import org.koin.core.module.Module
import org.koin.dsl.module

@OptIn(ExperimentalSettingsImplementation::class)
actual val platformModule: Module = module {
    single<UrlOpener> { IosUrlOpener() }
    single<Settings> { KeychainSettings(service = "com.ostarosto.app.secure") }
}
