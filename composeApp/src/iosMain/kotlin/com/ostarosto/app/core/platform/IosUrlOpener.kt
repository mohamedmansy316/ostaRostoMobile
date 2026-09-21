package com.ostarosto.app.core.platform

import platform.Foundation.NSURL
import platform.SafariServices.SFSafariViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController

/**
 * Opens payment/checkout links in an in-app Safari view controller instead of
 * switching to the standalone Safari app. Presenting it modally keeps the
 * user inside our app the whole time, so returning to the payment-waiting
 * screen (and its "I already paid" check) is a simple dismiss rather than an
 * app switch that can leave the user stranded outside the app.
 */
class IosUrlOpener : UrlOpener {
    override fun open(url: String): Boolean {
        val nsUrl = NSURL.URLWithString(url) ?: return false
        val scheme = nsUrl.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") return false

        val presenter = topViewController() ?: return false
        val safariVc = SFSafariViewController(nsUrl)
        presenter.presentViewController(safariVc, animated = true, completion = null)
        return true
    }

    private fun topViewController(): UIViewController? {
        @Suppress("DEPRECATION")
        val keyWindow = UIApplication.sharedApplication.keyWindow

        var top = keyWindow?.rootViewController ?: return null
        while (true) {
            top = top.presentedViewController ?: break
        }
        return top
    }
}
