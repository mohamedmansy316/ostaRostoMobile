package com.ostarosto.app

import androidx.compose.ui.window.ComposeUIViewController
import com.ostarosto.app.di.initKoin
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier

private val koinStarted: Unit by lazy {
    Napier.base(DebugAntilog())
    initKoin()
}

fun MainViewController() = ComposeUIViewController {
    koinStarted
    App()
}
