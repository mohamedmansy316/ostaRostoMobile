package com.ostarosto.app.core.platform

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Static hop between [com.ostarosto.app.MainActivity] (the only place that can call
 * `registerForActivityResult`) and [AndroidLocationProvider] (which lives in `core/platform`
 * and has no Activity reference) — same one-shot-bus shape as
 * [com.ostarosto.app.navigation.DeepLinkBus] / [com.ostarosto.app.navigation.LocationPickBus].
 */
object LocationPermissionBridge {

    private var launcher: ((Array<String>) -> Unit)? = null
    private val _result = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    val result: SharedFlow<Boolean> = _result

    /** Called once from `MainActivity.onCreate` with its permission launcher. */
    fun attach(launch: (Array<String>) -> Unit) {
        launcher = launch
    }

    fun request(permissions: Array<String>) {
        val launch = launcher
        if (launch != null) launch(permissions) else _result.tryEmit(false)
    }

    /** Called from `MainActivity`'s `ActivityResultCallback`. */
    fun onResult(granted: Boolean) {
        _result.tryEmit(granted)
    }
}
