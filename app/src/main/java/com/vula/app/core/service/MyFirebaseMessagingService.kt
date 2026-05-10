package com.vula.app.core.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * VulaPushService — placeholder for future push notification delivery.
 *
 * Firebase Cloud Messaging has been removed. Push notifications will be
 * delivered via the Go backend using a self-hosted solution (e.g. web-push
 * or a lightweight SSE/WebSocket keep-alive). This class remains as a
 * named anchor in the codebase for that future integration.
 *
 * In the meantime, in-app polling (3 s / 5 s intervals) in the
 * chat and request repositories keeps UI state fresh.
 */
class VulaPushService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Reserved for future push handling
        return START_NOT_STICKY
    }

    companion object {
        fun showNotification(context: Context, title: String, body: String) {
            val channelId = "vula_notifications"
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                nm.createNotificationChannel(
                    NotificationChannel(channelId, "Vula Notifications",
                        NotificationManager.IMPORTANCE_DEFAULT)
                )
            }

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .build()

            nm.notify(System.currentTimeMillis().toInt(), notification)
        }
    }
}
