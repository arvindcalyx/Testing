package com.test.safetyconnect.foreground.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.test.safetyconnect.R
import timber.log.Timber

/**
 * Manages foreground service notification
 * Handles notification creation and updates
 */
class NotificationManager(private val context: Context) {

    fun createNotification(emfValue: String, speed: String): Notification {
        val notificationLayout = RemoteViews(context.packageName, R.layout.notification_main)
        notificationLayout.setTextViewText(R.id.txt_speed, speed.ifEmpty { "Speed: N/A" })
        notificationLayout.setTextViewText(R.id.txt_emf, emfValue.ifEmpty { "EMF: N/A" })

        val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.safety_connect_active))
            .setSmallIcon(R.drawable.ic_notification_alert)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSilent(true)
            .setOngoing(true)
            .setCustomBigContentView(notificationLayout)
            .setCustomContentView(notificationLayout)
            .setColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
            .setColorized(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableLights(true)
                lightColor = Color.RED
            }
            notificationManager.createNotificationChannel(channel)
            builder.setChannelId(NOTIFICATION_CHANNEL_ID)
        }

        return builder.build()
    }

    fun showNotification(emfValue: String, speed: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        try {
            notificationManager.notify(NOTIFICATION_ID, createNotification(emfValue, speed))

            if (emfValue.isEmpty() && speed.isEmpty()) {
                notificationManager.cancel(NOTIFICATION_ID)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to show notification")
        }
    }

    fun hideNotification() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }

    companion object {
        const val NOTIFICATION_ID = 1
        const val NOTIFICATION_CHANNEL_ID = "1"
        const val NOTIFICATION_CHANNEL_NAME = "SafetyConnectService"
    }
}

