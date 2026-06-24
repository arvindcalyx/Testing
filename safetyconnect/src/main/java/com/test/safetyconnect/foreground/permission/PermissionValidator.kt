package com.test.safetyconnect.foreground.permission

import SafetyConnectSDK.Companion.isGpsPermissionEnabled
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Validates and checks required permissions for SafetyConnect service
 * Handles location permission validation
 */
object PermissionValidator {

    /**
     * Check if the app has required location permissions
     * @param context Android context
     * @return true if permissions are granted or not required
     */
    fun hasRequiredPermissions(context: Context): Boolean {
        return hasLocationPermission(context) || !isGpsPermissionEnabled()
    }

    /**
     * Check if location permissions are granted
     * @param context Android context
     * @return true if fine or coarse location permission is granted
     */
    fun hasLocationPermission(context: Context): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if all location permissions are denied
     * @param context Android context
     * @return true if all location permissions are denied and GPS is required
     */
    fun areAllPermissionsDenied(context: Context): Boolean {
        val fineLocationDenied = ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED

        val coarseLocationDenied = ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED

        val foregroundServiceLocationDenied = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.FOREGROUND_SERVICE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        } else {
            false // Not applicable for lower API levels
        }

        return fineLocationDenied &&
               coarseLocationDenied &&
               foregroundServiceLocationDenied &&
               isGpsPermissionEnabled()
    }

    /**
     * Check if notification permissions are enabled
     * @param context Android context
     * @return true if notifications are enabled
     */
    fun areNotificationsEnabled(context: Context): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /**
     * Validate all permissions required for location tracking
     * @param context Android context
     * @return PermissionValidationResult with validation status and message
     */
    fun validateLocationPermissions(context: Context): PermissionValidationResult {
        if (!areNotificationsEnabled(context)) {
            return PermissionValidationResult(
                isValid = false,
                reason = PermissionDenialReason.NOTIFICATIONS_DISABLED
            )
        }

        if (areAllPermissionsDenied(context)) {
            return PermissionValidationResult(
                isValid = false,
                reason = PermissionDenialReason.LOCATION_PERMISSIONS_DENIED
            )
        }

        return PermissionValidationResult(isValid = true, reason = null)
    }

    /**
     * Result of permission validation
     */
    data class PermissionValidationResult(
        val isValid: Boolean,
        val reason: PermissionDenialReason?
    )

    /**
     * Reasons for permission denial
     */
    enum class PermissionDenialReason {
        NOTIFICATIONS_DISABLED,
        LOCATION_PERMISSIONS_DENIED
    }
}

