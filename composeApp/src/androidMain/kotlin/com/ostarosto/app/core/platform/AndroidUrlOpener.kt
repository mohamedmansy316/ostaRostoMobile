package com.ostarosto.app.core.platform

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import io.github.aakira.napier.Napier

class AndroidUrlOpener(private val context: Context) : UrlOpener {
    override fun open(url: String): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            Napier.w(tag = "url", message = "No activity to open $url", throwable = e)
            false
        } catch (e: SecurityException) {
            Napier.w(tag = "url", message = "Not allowed to open $url", throwable = e)
            false
        }
    }
}
