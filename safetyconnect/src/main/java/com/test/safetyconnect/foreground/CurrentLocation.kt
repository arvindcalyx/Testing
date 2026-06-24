package com.test.safetyconnect.foreground

import SafetyConnectSDK.Companion.notifyAllLocationNotPermittedListener
import SafetyConnectSDK.Companion.notifyAllNotificationPermissionsRequiredListener
import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationRequest
import android.os.Build
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat
import com.test.safetyconnect.BuildConfig
import com.test.safetyconnect.foreground.permission.PermissionValidator
import com.test.safetyconnect.foreground.permission.PermissionValidator.PermissionDenialReason

class CurrentLocation(var context: Context) : LocationListener {
    private var locationManager: LocationManager? = null
    private var getLocation: GetLocation

    init {
        getLocation = context as GetLocation
        location()
    }


    fun removeLocation() {
        locationManager?.removeUpdates(this)
    }

    @SuppressLint("MissingPermission")
    private fun location() {
        synchronized(this) {
            locationManager = context.getSystemService(LOCATION_SERVICE) as LocationManager

            val validationResult = PermissionValidator.validateLocationPermissions(context)

            if (!validationResult.isValid) {
                when (validationResult.reason) {
                    PermissionDenialReason.NOTIFICATIONS_DISABLED -> {
                        notifyAllNotificationPermissionsRequiredListener()
                    }
                    PermissionDenialReason.LOCATION_PERMISSIONS_DENIED -> {
                        notifyAllLocationNotPermittedListener()
                    }
                    null -> {} // Should not happen
                }
                return
            }

            val provider = LocationManager.GPS_PROVIDER

            if (locationManager?.isProviderEnabled(provider) == true || BuildConfig.DEBUG) {
                val lastLocation = locationManager?.getLastKnownLocation(provider)
                lastLocation?.let { onLocationChanged(it) }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val locationRequest = LocationRequest.Builder(2000L)
                        .setMinUpdateDistanceMeters(1f)
                        .setQuality(LocationRequest.QUALITY_HIGH_ACCURACY)
                        .build()

                    locationManager?.requestLocationUpdates(
                        provider,
                        locationRequest,
                        ContextCompat.getMainExecutor(context),
                        this
                    )
                } else {
                    val locationRequest = LocationRequestCompat.Builder(2000L)
                        .setMinUpdateDistanceMeters(1f)
                        .setQuality(LocationRequestCompat.QUALITY_HIGH_ACCURACY)
                        .build()

                    LocationManagerCompat.requestLocationUpdates(
                        locationManager!!,
                        provider,
                        locationRequest,
                        ContextCompat.getMainExecutor(context)
                    ) { location: Location ->
                        onLocationChanged(location)
                    }
                }
            } else {
                getLocation.onProviderDisabled("GPS Provider is disabled")
                Toast.makeText(context, "GPS Provider is disabled", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onLocationChanged(location: Location) {
        getLocation.onLocationChanged(location)
    }

    override fun onProviderEnabled(s: String) {
        getLocation.onProviderEnabled(s)
    }

    override fun onProviderDisabled(s: String) {
        getLocation.onProviderDisabled(s)
    }

    interface GetLocation {
        fun onLocationChanged(location: Location?)
        fun onProviderEnabled(s: String?)
        fun onProviderDisabled(s: String?)
    }

}