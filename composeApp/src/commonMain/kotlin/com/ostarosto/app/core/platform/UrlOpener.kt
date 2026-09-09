package com.ostarosto.app.core.platform

/** Opens an external URL (Paymob web checkout) in the system browser / custom tab. */
interface UrlOpener {
    fun open(url: String)
}
