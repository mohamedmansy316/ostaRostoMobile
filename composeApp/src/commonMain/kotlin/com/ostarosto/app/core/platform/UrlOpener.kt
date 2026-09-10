package com.ostarosto.app.core.platform

/** Opens an external URL (Paymob web checkout) in the system browser / custom tab. */
interface UrlOpener {
    /** @return true if an app was launched to handle the URL, false otherwise. */
    fun open(url: String): Boolean
}
