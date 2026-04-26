package com.vula.app.core.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.vula.app.core.util.Constants
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {

    // Use Hilt EntryPoint to get the same emulator-configured instances
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface FirebaseEntryPoint {
        fun firebaseAuth(): FirebaseAuth
        fun firebaseFirestore(): FirebaseFirestore
    }

    private val entryPoint: FirebaseEntryPoint by lazy {
        EntryPointAccessors.fromApplication(
            applicationContext,
            FirebaseEntryPoint::class.java
        )
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = entryPoint.firebaseAuth().currentUser?.uid
        if (uid != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    entryPoint.firebaseFirestore()
                        .collection(Constants.USERS_COLLECTION)
                        .document(uid)
                        .update("fcmToken", token)
                } catch (e: Exception) {
                    android.util.Log.e("FCM", "Failed to save token", e)
                }
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val title = remoteMessage.notification?.title ?: "Vula"
        val body = remoteMessage.notification?.body ?: "New message"
        showNotification(title, body)
    }

    private fun showNotification(title: String, message: String) {
        val channelId = "vula_notifications"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Vula Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
