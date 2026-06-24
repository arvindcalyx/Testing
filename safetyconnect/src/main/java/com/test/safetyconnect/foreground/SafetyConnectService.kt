package com.test.safetyconnect.foreground

import SafetyConnectSDK
import SafetyConnectSDK.Companion.activity
import SafetyConnectSDK.Companion.notifyAllLocationNotPermittedListener
import SafetyConnectSDK.Companion.notifyAllOverSpeedDetectedListener
import SafetyConnectSDK.Companion.notifyAllTurnOnGpsLocationListener
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.media.MediaPlayer
import android.os.IBinder
import com.test.safetyconnect.R
import com.test.safetyconnect.foreground.emf.EmfDetector
import com.test.safetyconnect.foreground.harsh.HarshDrivingDetector
import com.test.safetyconnect.foreground.lifecycle.ServiceLifecycleManager
import com.test.safetyconnect.foreground.permission.PermissionValidator
import com.test.safetyconnect.foreground.speed.SpeedManager
import com.test.safetyconnect.foreground.speed.SpeedResult
import com.test.safetyconnect.foreground.util.convertToKmPerHr
import com.test.safetyconnect.utils.Manager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.roundToLong

/**
 * Foreground service for safety monitoring
 * Handles speed detection, EMF detection, and harsh driving detection
 *
 * @author SDK Team
 * @createdDate 14/11/23
 */
class SafetyConnectService : Service(), SensorEventListener, CurrentLocation.GetLocation {

    private  var speedManager: SpeedManager?=null
    private  var emfDetector: EmfDetector?=null
    private  var harshDrivingDetector: HarshDrivingDetector?=null
    private  var notificationManager: com.test.safetyconnect.foreground.notification.NotificationManager?=null
    private  var lifecycleManager: ServiceLifecycleManager?=null

    // Sensors
    private var sensorManager: SensorManager? = null
    private var emfSensor: Sensor? = null
    private var accelerometerSensor: Sensor? = null

    // Location & Speed
    private var locationManager: CurrentLocation? = null
    private var currentLocation: Location? = null
    private var disableEmfSpeedInKmHr: Float = 0f

    // Media
    private var mediaPlayer: MediaPlayer? = null

    // State
    private var isFirstRun = true
    private var serviceKilled = false
    private val coroutineScope = CoroutineScope(Dispatchers.Default + Job())

    // UI State
    private var lastKnownEmf: String = "EMF: "
    private var lastKnownSpeed: String = "SPEED: "

    // Detection tracking
    private val lastOverSpeedDetected = AtomicLong(0L)

    override fun onCreate() {
        super.onCreate()
        if (activity?.get() == null) {
            stopSelf()
            return
        }

        initializeManagers()
        checkForPermissionAndStartService()
    }

    private fun initializeManagers() {
        speedManager = SpeedManager()
        harshDrivingDetector = HarshDrivingDetector()
        notificationManager = com.test.safetyconnect.foreground.notification.NotificationManager(this)
        lifecycleManager = ServiceLifecycleManager(this)
        initMediaPlayer()
    }


    private fun checkForPermissionAndStartService() {
        activity?.get()?.let { safeActivity ->
            if (!PermissionValidator.hasRequiredPermissions(safeActivity)) {
                notifyAllLocationNotPermittedListener()
                stopSelf()
                return
            }

            startForegroundService()
        }
    }

    private fun startForegroundService() {
        val notification = notificationManager?.createNotification(lastKnownEmf, lastKnownSpeed)
        notification?.let {
            lifecycleManager?.startForeground(
                com.test.safetyconnect.foreground.notification.NotificationManager.NOTIFICATION_ID,
                it
            )
        }
    }

    private fun initSensors() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        emfSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        accelerometerSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        registerSensors()
    }

    private fun registerSensors() {
        sensorManager?.registerListener(this, emfSensor, SensorManager.SENSOR_DELAY_UI)
        sensorManager?.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_UI)
    }

    override fun startForegroundService(service: Intent?): ComponentName? {
        notificationManager?.showNotification("", "")
        return super.startForegroundService(service)
    }

    override fun onSensorChanged(sensorEvent: SensorEvent?) {
        sensorEvent?.let {
            if (it.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                coroutineScope.launch {
                    handleMagneticFieldSensor(it)
                }
            }
        }
    }

    private fun handleMagneticFieldSensor(sensorEvent: SensorEvent) {
        Manager.getManagerInstance().runTask {
            emfDetector?.processSensorEvent(sensorEvent, currentLocation, disableEmfSpeedInKmHr)?.let { emfValue ->
                lastKnownEmf = emfValue
                notificationManager?.showNotification(emfValue, lastKnownSpeed)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureManagersInitialized()
        checkForPermissionAndStopService()
        initializeDetectors()
        startSpeedService()

        Manager.getManagerInstance().runTask {
            handleServiceAction(intent?.action)
        }

        return START_STICKY
    }

    private fun ensureManagersInitialized() {
        if (speedManager == null ||
            harshDrivingDetector == null ||
            notificationManager == null ||
            lifecycleManager == null ||
            emfDetector == null) {
            initializeManagers()
        }
    }

    private fun initializeDetectors() {
        harshDrivingDetector?.initialize()
        lastOverSpeedDetected.set(System.currentTimeMillis())
    }

    private fun handleServiceAction(action: String?) {
        when (action) {
            ACTION_START_OR_RESUME_SERVICE -> {
                if (isFirstRun) {
                    runServiceForFirstTime()
                } else {
                    // Service restarted by system, re-register sensors if needed
                    ensureSensorsRegistered()
                }
            }
            ACTION_PAUSE_SERVICE -> {
                Timber.d("Paused Service")
                pauseService()
            }
            ACTION_STOP_SERVICE -> {
                Timber.d("Stopped service.")
                killService()
            }
        }
    }

    private fun ensureSensorsRegistered() {
        if (sensorManager == null || emfSensor == null || accelerometerSensor == null) {
            initSensors()
        }
    }

    private fun checkForPermissionAndStopService() {
        activity?.get()?.let {
            if (PermissionValidator.areAllPermissionsDenied(it)) {
                notifyAllLocationNotPermittedListener()
                stopSelf()
            }
        }
    }

    private fun runServiceForFirstTime() {
        isFirstRun = false
        serviceKilled = false
        initSensors()
        notificationManager?.showNotification("", "")
    }

    private fun startSpeedService() {
        if (SafetyConnectSDK.sensorFilters?.isSpeedDetectionEnabled == true) {
            if (locationManager == null) {
                locationManager = CurrentLocation(this)
            }
        }
    }

    private fun initMediaPlayer() {
        try {
            mediaPlayer = MediaPlayer.create(applicationContext, R.raw.sound)
            emfDetector = EmfDetector(mediaPlayer)
            emfDetector?.initialize()
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize media player")
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun pauseService() {
        locationManager?.removeLocation()
    }

    private fun killService() {
        serviceKilled = true
        isFirstRun = true
        pauseService()
        cleanup()
        stopForegroundService()
    }

    private fun cleanup() {
        sensorManager?.unregisterListener(this)
        sensorManager = null
        mediaPlayer?.release()
        mediaPlayer = null
        speedManager?.clear()
        harshDrivingDetector?.clear()
        emfDetector?.clear()
    }

    private fun stopForegroundService() {
        lifecycleManager?.stopForeground(removeNotification = true)
        lifecycleManager?.stopService()
        notificationManager?.hideNotification()
    }

    override fun onLocationChanged(location: Location?) {
        if (serviceKilled) {
            notificationManager?.hideNotification()
            return
        }

        location?.let { loc ->
            processLocationUpdate(loc)
        }
    }

    private fun processLocationUpdate(location: Location) {
        when (val result = speedManager?.processLocation(location)) {
            is SpeedResult.Stationary -> {
                handleStationarySpeed(location)
            }
            is SpeedResult.Collecting -> {
                handleCollectingSpeed(result.currentSpeed, location)
            }
            is SpeedResult.Valid -> {
                handleValidSpeed(result.currentSpeed, result.medianSpeed, location)
            }
            is SpeedResult.Rejected -> {
                Timber.d("Location rejected: ${result.reason}")
            }
            null -> {
                Timber.w("SpeedManager is null, skipping location processing")
            }
        }
    }

    private fun handleStationarySpeed(location: Location) {
        currentLocation = location
        disableEmfSpeedInKmHr = 0f
        lastKnownSpeed = "Speed: 0 km/hr"
        notificationManager?.showNotification(lastKnownEmf, lastKnownSpeed)
    }

    private fun handleCollectingSpeed(speedInKmh: Float, location: Location) {
        currentLocation = location
        disableEmfSpeedInKmHr = speedInKmh
        lastKnownSpeed = "Speed: ${speedInKmh.roundToLong()} km/hr (collecting...)"
        notificationManager?.showNotification(lastKnownEmf, lastKnownSpeed)
    }

    private fun handleValidSpeed(currentSpeed: Float, medianSpeed: Float, location: Location) {
        currentLocation = location
        disableEmfSpeedInKmHr = currentSpeed
        lastKnownSpeed = "Speed: ${currentSpeed.roundToLong()} km/hr"

        if (SafetyConnectSDK.sensorFilters?.harshDrivingCaptureEnabled == true) {
            harshDrivingDetector?.analyze(location)
        }

        if ((SafetyConnectSDK.sensorFilters?.maxSpeedThreshold ?: 0f) <= medianSpeed) {
            fireOverSpeedingEvent(location.apply { speed = medianSpeed / 3.6f })
        }

        notificationManager?.showNotification(lastKnownEmf, "Speed: ${location.speed.convertToKmPerHr()} km/hr")
    }

    private fun fireOverSpeedingEvent(location: Location) {
        val currentTimeMillis = System.currentTimeMillis()
        val callbackFrequency = SafetyConnectSDK.sensorFilters?.speedCallBackFrequency ?: 30000L
        synchronized(lastOverSpeedDetected) {
            if (currentTimeMillis - lastOverSpeedDetected.get() >= callbackFrequency) {
                lastOverSpeedDetected.set(currentTimeMillis)
                notifyAllOverSpeedDetectedListener(
                    location,
                    SafetyConnectSDK.sensorFilters?.speedDetectionEdge
                )
                Timber.d("Overspeed detected: ${location.speed.convertToKmPerHr()} km/hr")
            }
        }
    }


    override fun onProviderEnabled(s: String?) {}

    override fun onProviderDisabled(s: String?) {
        notifyAllTurnOnGpsLocationListener()
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
        cleanup()
    }

    companion object {
        const val ACTION_START_OR_RESUME_SERVICE = "action_start_or_resume_service"
        const val ACTION_PAUSE_SERVICE = "action_pause_service"
        const val ACTION_STOP_SERVICE = "action_stop_service"
    }
}
