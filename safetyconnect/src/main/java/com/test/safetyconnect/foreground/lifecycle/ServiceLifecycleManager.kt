package com.test.safetyconnect.foreground.lifecycle

import android.app.Notification
import android.app.Service
import android.content.pm.ServiceInfo
import android.os.Build
import timber.log.Timber

/**
 * Handles foreground service lifecycle operations
 * Manages service startup, shutdown, and foreground state
 */
class ServiceLifecycleManager(private val service: Service) {

    /**
     * Start service in foreground mode with notification
     * @param notificationId The notification ID
     * @param notification The notification to display
     */
    fun startForeground(notificationId: Int, notification: Notification) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                service.startForeground(
                    notificationId,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                )
            } else {
                service.startForeground(notificationId, notification)
            }
            Timber.d("Service started in foreground")
        } catch (e: Exception) {
            Timber.e(e, "Failed to start service in foreground")
        }
    }

    /**
     * Stop foreground service
     * @param removeNotification Whether to remove the notification
     */
    fun stopForeground(removeNotification: Boolean = true) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val flags = if (removeNotification) {
                    Service.STOP_FOREGROUND_REMOVE
                } else {
                    Service.STOP_FOREGROUND_DETACH
                }
                service.stopForeground(flags)
            } else {
                @Suppress("DEPRECATION")
                service.stopForeground(removeNotification)
            }
            Timber.d("Service stopped foreground")
        } catch (e: Exception) {
            Timber.e(e, "Failed to stop foreground service")
        }
    }

    /**
     * Stop the service completely
     */
    fun stopService() {
        try {
            service.stopSelf()
            Timber.d("Service stopped")
        } catch (e: Exception) {
            Timber.e(e, "Failed to stop service")
        }
    }
}

