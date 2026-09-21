package com.ostarosto.app.core.platform

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import io.github.aakira.napier.Napier

/**
 * Opens payment/checkout links in a Chrome Custom Tab instead of handing off
 * to a separate browser app. A Custom Tab runs in our own task, so the
 * Activity (and the payment-waiting screen's state) survives the round trip —
 * plain ACTION_VIEW can launch a standalone browser app that displaces us and
 * lets the OS reclaim our Activity while it's backgrounded, stranding the
 * user with no way back to the "I already paid" screen.
 */
class AndroidUrlOpener(private val context: Context) : UrlOpener {
    override fun open(url: String): Boolean {
        val uri = Uri.parse(url)
        return try {
            val customTabsIntent = CustomTabsIntent.Builder().build()
            customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            customTabsIntent.launchUrl(context, uri)
            true
        } catch (e: ActivityNotFoundException) {
            Napier.w(tag = "url", message = "No Custom Tabs browser for $url, falling back", throwable = e)
            openInAnyBrowser(uri)
        } catch (e: SecurityException) {
            Napier.w(tag = "url", message = "Not allowed to open $url", throwable = e)
            false
        }
    }

    private fun openInAnyBrowser(uri: Uri): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            ContextCompat.startActivity(context, intent, null)
            true
        } catch (e: ActivityNotFoundException) {
            Napier.w(tag = "url", message = "No activity to open $uri", throwable = e)
            false
        }
    }
}
