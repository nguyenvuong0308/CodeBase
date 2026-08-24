package com.codebasetemplate.required.firebase

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.codebasetemplate.R
import com.codebasetemplate.features.feature_splash.ui.SplashActivity
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        const val FCM_CHANNEL_ID = "fcm_channel_id"
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun handleIntent(intent: Intent) {
        try {
            super.handleIntent(intent)
        } catch (se: SecurityException) {
            Log.e("FCM", "SecurityException when Firebase handles notification intent", se)
            Firebase.analytics.logEvent("FCM_AUTO_NOTIFY_SECURITY", Bundle().apply {
                putString("model", Build.MODEL)
            })
        }
    }

    override fun onNewToken(token: String) {
        // Gửi token lên server của bạn (để nhắm đẩy theo user)
        Log.e("FCM", "New token: $token")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.e("FCM", "onMessageReceived: $remoteMessage")
        createNotificationChannelIfNeed()
        // 1) Notification message (tự hiển thị khi app background)
        remoteMessage.notification?.let {
            showLocalNotification(it.title, it.body, it.imageUrl)
        }

        // 2) Data message (bạn tự xử lý)
        val data = remoteMessage.data // Map<String, String, String>
        if (data.isNotEmpty()) {
            showLocalNotification(data["title"], data["body"], null)
        }
    }

    private fun showLocalNotification(title: String?, body: String?, imageUrl: Uri?) {
        val intent = Intent(this, SplashActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(this, FCM_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title ?: getString(R.string.app_name))
            .setContentText(body ?: "")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        fun createNotification() {
            val notification = builder.build()
            val id = (System.currentTimeMillis() and 0x7FFFFFFF).toInt()
            safeNotify(id, notification)
        }

        if (imageUrl != null) {
            Glide.with(this)
                .asBitmap()
                .load(imageUrl)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        builder.setStyle(
                            NotificationCompat.BigPictureStyle()
                                .bigPicture(resource)
                                .bigLargeIcon(resource)
                        )

                        createNotification()
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                        createNotification()
                    }
                })
        } else {
            createNotification()
        }
    }

    private fun createNotificationChannelIfNeed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(FCM_CHANNEL_ID, getString(R.string.app_name), NotificationManager.IMPORTANCE_HIGH)
            channel.setBypassDnd(true)
            channel.enableLights(true)
            channel.lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun Context.canPostNotifications(): Boolean {
        val nm = NotificationManagerCompat.from(this)

        // 1) Android 13+: phải được cấp POST_NOTIFICATIONS
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }

        // 2) App-level notification có đang bị tắt không
        return nm.areNotificationsEnabled()
    }

    fun Context.safeNotify(id: Int, notification: Notification) {
        if (!canPostNotifications()) {
            Log.w("FCM", "Notifications disabled or permission missing")
            return
        }
        try {
            NotificationManagerCompat.from(this).notify(id, notification)
        } catch (se: SecurityException) {
            Log.e("FCM", "SecurityException when posting notification", se)
        }
    }
}