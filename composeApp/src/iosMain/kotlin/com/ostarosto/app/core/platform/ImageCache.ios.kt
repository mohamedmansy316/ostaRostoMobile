package com.ostarosto.app.core.platform

import coil3.PlatformContext
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUserDomainMask

actual fun imageCacheDirectory(context: PlatformContext): Path {
    val cachesDir = (NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true)
        .firstOrNull() as? String)
        ?: NSTemporaryDirectory()
    return "$cachesDir/image_cache".toPath()
}
