package com.example.alohi.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.alohi.MainActivity
import com.example.alohi.R
import com.example.alohi.data.local.TokenManager
import com.example.alohi.data.remote.ApiClient
import com.example.alohi.data.model.FcmTokenRequest
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AlohiFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed FCM token: $token")
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String) {
        val application = applicationContext as android.app.Application
        val tokenManager = TokenManager(application)
        ApiClient.init(tokenManager)
        val api = ApiClient.userApi

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (tokenManager.isLoggedIn.first()) {
                    val deviceId = tokenManager.deviceId.first() ?: java.util.UUID.randomUUID().toString()
                    val request = FcmTokenRequest(
                        token = token,
                        deviceId = deviceId, 
                        deviceType = "android"
                    )
                    api.registerFcmToken(request)
                    Log.d(TAG, "FCM token registered with server")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register FCM token", e)
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            val action = remoteMessage.data["action"]
            if (action == "incoming_call") {
                handleIncomingCallWakeUp(remoteMessage.data)
                return // Skip default message notification for calls
            }
            
            // Fallback for data-only push notifications
            if (remoteMessage.notification == null) {
                val title = remoteMessage.data["title"] ?: remoteMessage.data["senderName"] ?: "Tin nhắn mới"
                val body = remoteMessage.data["body"] ?: remoteMessage.data["content"] ?: "Bạn có tin nhắn mới"
                sendNotification(title, body, remoteMessage.data)
                return
            }
        }

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            sendNotification(it.title, it.body, remoteMessage.data)
        }
    }

    private fun sendNotification(title: String?, messageBody: String?, data: Map<String, String>? = null) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            
            // Put all FCM data payload into Intent extras so MainActivity can route
            data?.forEach { (key, value) ->
                putExtra(key, value)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0 /* Request code */, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
        )

        val channelId = "alohi_message_channel"
        val defaultSoundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title ?: "Thông báo mới")
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(defaultSoundUri)
            .setVibrate(longArrayOf(0, 250, 100, 250))
            .setDefaults(NotificationCompat.DEFAULT_LIGHTS)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = android.media.AudioAttributes.Builder()
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                .build()
            val channel = NotificationChannel(
                channelId,
                "Tin nhắn AloHi",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Thông báo tin nhắn mới"
                setSound(defaultSoundUri, audioAttributes)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 100, 250)
            }
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    private fun handleIncomingCallWakeUp(data: Map<String, String>) {
        val callId = data["callId"] ?: return
        val callerName = data["callerName"] ?: "Người gọi ẩn danh"
        val isVideo = data["type"] == "video"

        // Create an intent to launch MainActivity (will route to IncomingCallScreen)
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("action", "incoming_call")
            data.forEach { (key, value) -> putExtra(key, value) }
        }

        val pendingIntent = PendingIntent.getActivity(
            this, callId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "alohi_call_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Cuộc gọi đến", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Thông báo cuộc gọi đến"
                setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE), null)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val title = if (isVideo) "Cuộc gọi Video đến" else "Cuộc gọi thoại đến"
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText("$callerName đang gọi...")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(false)
            .setOngoing(true)
            .setFullScreenIntent(pendingIntent, true) // Wakes up the device!
            .build()

        notificationManager.notify(callId.hashCode(), notification)
    }

    companion object {
        private const val TAG = "AlohiFCM"
    }
}
