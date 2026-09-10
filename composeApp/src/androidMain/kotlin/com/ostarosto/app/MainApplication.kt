package com.ostarosto.app

import android.app.Application
import com.ostarosto.app.core.config.AppConfig
import com.ostarosto.app.di.initKoin
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.koin.android.ext.koin.androidContext

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (AppConfig.isDebug) Napier.base(DebugAntilog())
        initKoin {
            androidContext(this@MainApplication)
        }
    }
}
