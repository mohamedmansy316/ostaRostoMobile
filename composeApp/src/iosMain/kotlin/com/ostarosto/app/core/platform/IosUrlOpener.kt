package com.ostarosto.app.core.platform

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

class IosUrlOpener : UrlOpener {
    override fun open(url: String): Boolean {
        val nsUrl = NSURL.URLWithString(url) ?: return false
        val app = UIApplication.sharedApplication
        if (!app.canOpenURL(nsUrl)) return false
        app.openURL(nsUrl)
        return true
    }
}
