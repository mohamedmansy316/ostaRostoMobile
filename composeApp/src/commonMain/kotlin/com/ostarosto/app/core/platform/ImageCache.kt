package com.ostarosto.app.core.platform

import coil3.PlatformContext
import okio.Path

/**
 * App-private, writable directory Coil uses to persist already-downloaded images to disk.
 *
 * Coil's own default (`FileSystem.SYSTEM_TEMPORARY_DIRECTORY`) resolves to `java.io.tmpdir` on
 * Android, which usually isn't writable by the app — so images would silently never make it to
 * disk and get re-downloaded on every cold start. Pointing it at the platform's real cache
 * directory instead makes the cache actually persist.
 */
expect fun imageCacheDirectory(context: PlatformContext): Path
