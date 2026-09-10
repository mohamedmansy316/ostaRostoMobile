package com.ostarosto.app.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.ostarosto.app.MainActivity
import com.ostarosto.app.R
import com.ostarosto.app.data.repository.DeviceRepository
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/**
 * Receives FCM messages. `data.type == "order_status"` deep-links to the order
 * detail screen via `ostarosto://order/{id}`.
 */
class OstaFirebaseMessagingService : FirebaseMessagingService() {

    private val deviceRepository: DeviceRepository by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Firebase is only wired once google-services.json ships (see README). */
    private val firebaseReady: Boolean
        get() = FirebaseApp.getApps(applicationContext).isNotEmpty()

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onNewToken(token: String) {
        if (!firebaseReady) return
        Napier.d(tag = "fcm", message = "new token")
        scope.launch {
            runCatching { deviceRepository.register(token, platform = "android", appVersion = appVersion()) }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        if (!firebaseReady) return
        val data = message.data
        val title = message.notification?.title ?: data["title"] ?: "أسطى روستو"
        val body = message.notification?.body ?: data["body"].orEmpty()
        val orderId = data["order_id"]
        val orderIdLong = orderId?.toLongOrNull()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (data["type"] == "order_status" && orderIdLong != null) {
                action = Intent.ACTION_VIEW
                setData(Uri.parse("ostarosto://order/$orderIdLong"))
            }
        }

        // Stable per-order id so a later update replaces the same notification
        // instead of colliding by String.hashCode().
        val notifId = orderIdLong?.let { (it % Int.MAX_VALUE).toInt() } ?: NOTIF_ID

        val pending = PendingIntent.getActivity(
            this,
            notifId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        ensureChannel()

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        getSystemService(NotificationManager::class.java)
            .notify(notifId, notification)
    }

    private fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.order_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun appVersion(): String =
        runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull() ?: "1.0.0"

    private companion object {
        const val CHANNEL_ID = "order_status"
        const val NOTIF_ID = 1001
    }
}
