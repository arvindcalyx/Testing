import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import com.test.safetyconnect.BuildConfig
import com.test.safetyconnect.foreground.SafetyConnectService
import com.test.safetyconnect.model.SensorResponse
import com.test.safetyconnect.sdkinit.ImageDetector
import com.test.safetyconnect.sdkinit.SafetyConnect
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

class SafetyConnectSDK {

    companion object {
        var sensorFilters: SensorFilters? = null
        var sdkVersion:String = BuildConfig.SDK_VERSION_CODE
        var activity: WeakReference<Activity>? = null
        private var registerForCallBack: ConcurrentHashMap<SafetyTypes, SafetyConnectCommunicator?> =
            ConcurrentHashMap()
        private var safetyConnect: WeakReference<SafetyConnect?>? = null
        private var detector: WeakReference<ImageDetector?>? = null


        fun initSDK(
            sensorFilters: SensorFilters?,
            activity: Activity,
            registerForCallBack: SafetyConnectCommunicator? = null
        ) {
            // Initialize Timber for logging
            if (Timber.treeCount() == 0) {
                Timber.plant(Timber.DebugTree())
            }

            this.activity = WeakReference(activity)
            initializeSensorFilter(sensorFilters)
            addActiveListener(registerForCallBack)
        }


        private fun initializeSensorFilter(sensorFilters: SensorFilters?) {
            if (this.sensorFilters == null)
                this.sensorFilters = SensorFilters()
            this.sensorFilters?.isEMFDetectionEnabled = sensorFilters?.isEMFDetectionEnabled
            this.sensorFilters?.maxEmfThreshold = sensorFilters?.maxEmfThreshold
            this.sensorFilters?.isSpeedDetectionEnabled = sensorFilters?.isSpeedDetectionEnabled
            this.sensorFilters?.maxSpeedThreshold = sensorFilters?.maxSpeedThreshold
            this.sensorFilters?.isFallCrashDetectionEnabled = sensorFilters?.isFallCrashDetectionEnabled
            this.sensorFilters?.networkCallFrequency = sensorFilters?.networkCallFrequency
            this.sensorFilters?.sensorDataFrequency = sensorFilters?.sensorDataFrequency
            this.sensorFilters?.isFeedbackRequired = sensorFilters?.isFeedbackRequired ?: false
            this.sensorFilters?.crashDetectionEdge = sensorFilters?.crashDetectionEdge
            this.sensorFilters?.emfDetectionEdge = sensorFilters?.emfDetectionEdge
            this.sensorFilters?.speedDetectionEdge = sensorFilters?.speedDetectionEdge
            this.sensorFilters?.speedCallBackFrequency = sensorFilters?.speedCallBackFrequency
            this.sensorFilters?.emfCallBackFrequency = sensorFilters?.emfCallBackFrequency
            this.sensorFilters?.crashCallBackFrequency = sensorFilters?.crashCallBackFrequency
            this.sensorFilters?.harshDrivingDurationFrequency = sensorFilters?.harshDrivingDurationFrequency
            this.sensorFilters?.harshDrivingCallbackFrequency = sensorFilters?.harshDrivingCallbackFrequency
            this.sensorFilters?.harshDrivingThresholdInKm = sensorFilters?.harshDrivingThresholdInKm
            this.sensorFilters?.harshDrivingEdge = sensorFilters?.harshDrivingEdge
            this.sensorFilters?.harshDrivingCaptureEnabled = sensorFilters?.harshDrivingCaptureEnabled
            this.sensorFilters?.disableEmfMinimumThresholdInKm = sensorFilters?.disableEmfMinimumThresholdInKm
            this.sensorFilters?.safetyType = sensorFilters?.safetyType
            this.sensorFilters?.isEMFACDetectionEnabled= sensorFilters?.isEMFACDetectionEnabled
            this.sensorFilters?.maxACEmfThreshold= sensorFilters?.maxACEmfThreshold
            this.sensorFilters?.maxAcEmfValue= sensorFilters?.maxAcEmfValue

        }

        fun notifyAllLocationNotPermittedListener() {
            for (safetyConnectCommunicator in registerForCallBack.values) {
                safetyConnectCommunicator?.locationPermissionNotGranted()
            }
        }

//        fun notifyAllActivityRecognisitionNotPermittedListener() {
//            for (safetyConnectCommunicator in registerForCallBack.values) {
//                safetyConnectCommunicator?.activityRecognisitionPermissionNotGranted()
//            }
//        }

        fun notifyAllNotificationPermissionsRequiredListener() {
            for (safetyConnectCommunicator in registerForCallBack.values) {
                safetyConnectCommunicator?.notificationPermissionsRequired()
            }
        }

        fun notifyAllOnCrashFallDetectedListener(
            sensorResponse: SensorResponse,
            crashDetectionEdge: String?
        ) {
            for (safetyConnectCommunicator in registerForCallBack.values) {
                safetyConnectCommunicator?.onCrashFallDetected(sensorResponse, crashDetectionEdge)
            }
        }

        fun notifyAllMagneticFieldDetectedListener(sqrt: Double, emfDetectionEdge: String?) {
            for (safetyConnectCommunicator in registerForCallBack.values) {
                safetyConnectCommunicator?.magneticFieldDetected(sqrt, emfDetectionEdge)
            }
        }

        fun notifyAllHarshDrivingListener(
            speed: Float?,
            jerkSpeedLimit: String?,
            eventType: String
        ) {
            for (safetyConnectCommunicator in registerForCallBack.values) {
                safetyConnectCommunicator?.onHarshDrivingDetected(speed, jerkSpeedLimit, eventType)
            }
        }


        public fun hasTotalActiveObserver(): Int {
            return registerForCallBack.values.size
        }


        public fun addObserver(
            safetyType: SafetyTypes?,
            safetyConnectCommunicator: SafetyConnectCommunicator
        ) {
            safetyType?.let { registerForCallBack.put(it, safetyConnectCommunicator) }
        }

        public fun removeObserver(safetyType: SafetyTypes?) {
            registerForCallBack.remove(safetyType)
        }

        fun notifyAllOverSpeedDetectedListener(location: Location?, speedDetectionEdge: String?) {
            for (safetyConnectCommunicator in registerForCallBack.values) {
                safetyConnectCommunicator?.overSpeedDetected(location, speedDetectionEdge)
            }
        }


        fun notifyAllTurnOnGpsLocationListener() {
            for (safetyConnectCommunicator in registerForCallBack.values) {
                safetyConnectCommunicator?.turnOnGpsLocation()
            }
        }


        private fun addActiveListener(registerForCallBack: SafetyConnectCommunicator?) {
            if (this.registerForCallBack[sensorFilters?.safetyType] == null) {
                sensorFilters?.safetyType?.let {
                    registerForCallBack?.let { it1 ->
                        this.registerForCallBack.put(
                            it,
                            it1
                        )
                    }
                }
                registerForCallBack?.onSdkInitialized()

            }
        }

        fun isGpsPermissionEnabled(): Boolean {
            val locationManager =
                activity?.get()?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            return isGPSEnabled
        }

        private fun startService(sensorFilters: SensorFilters?) {
            activity?.get()?.let { safeActivity ->
                if (!NotificationManagerCompat.from(safeActivity).areNotificationsEnabled()) {
                    notifyAllNotificationPermissionsRequiredListener()
                    return
                }
                if (ActivityCompat.checkSelfPermission(
                        safeActivity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(
                        safeActivity,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(
                        safeActivity,
                        Manifest.permission.FOREGROUND_SERVICE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                    && isGpsPermissionEnabled()
                ) {
                    notifyAllLocationNotPermittedListener()
                    return
                }
                safetyConnect = WeakReference(
                    SafetyConnect().init(
                        context = safeActivity,
                        sensorFilters
                    )
                )


                /**
                 * Enable Safety Connect Sensor Module for getting Mobile Application Sensor data
                 * */
                safetyConnect?.get()?.enableSafetySensor(true)?.enableSafetySensorFeedback(true)
                    ?.enableSensorFrequency(sensorFilters?.sensorDataFrequency ?: 15000)
                    ?.initSensor()


                /**
                 * Get Sensor API response from Backend weather we have detected crash or not
                 * */
                safetyConnect?.get()?.getAccidentDetectorInstance()
                    ?.initSensorsAPIResponse { sensorResponse ->
                        Timber.tag("okhttpdata").e(sensorResponse.message.toString())
                    }


                /**
                 * Validate weather crash is valid or not
                 * */
                safetyConnect?.get()?.getAccidentDetectorInstance()
                    ?.getFeedbackCallback { sensorResponse ->
                        Timber.tag("okhttpdata").e(sensorResponse.message.toString())
                        for (callback in registerForCallBack.values) {
                            callback?.getFeedbackCallback(sensorResponse)

                        }

                    }


                /**
                 * Bind Application With Android Life Cycle
                 * */
                safetyConnect?.get()?.getAccidentDetectorInstance()?.bindSensor()


                /**
                 * Initialise Safety Equipment Detection
                 * */
                safetyConnect?.get()?.enableSafetyEquipmentDetectionFeature(true)
                    ?.enableSafetyEquipmentAPIResponse(true)?.initSafetyEquipmentDetection()

                detector = WeakReference(
                    safetyConnect?.get()?.getImageDetectorInstance()?.imageDetectorInit()
                )
            }

        }


        fun startService(activity: Activity?) {
            activity?.let {
                if (!NotificationManagerCompat.from(activity).areNotificationsEnabled()) {
                    notifyAllNotificationPermissionsRequiredListener()
                    return
                }
                if (ActivityCompat.checkSelfPermission(
                        activity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(
                        activity,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(
                        activity,
                        Manifest.permission.FOREGROUND_SERVICE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                    && isGpsPermissionEnabled()
                ) {
                    notifyAllLocationNotPermittedListener()
                    return
                }

                val intent = Intent(activity, SafetyConnectService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.getActivity(
                        activity,
                        1,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                } else {
                    PendingIntent.getActivity(
                        activity,
                        1,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                    )
                }
                startService(sensorFilters)
                intent.action = SafetyConnectService.ACTION_START_OR_RESUME_SERVICE
                activity.startService(intent)
            }

        }

        fun killService(activity: Activity?) {
            val intent = Intent(activity, SafetyConnectService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.getActivity(
                    activity,
                    1,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            } else {
                PendingIntent.getActivity(
                    activity,
                    1,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            }
            unregisterListener()
            intent.action = SafetyConnectService.ACTION_STOP_SERVICE
            activity?.startService(intent)

        }

        private fun unregisterListener() {
            // do it later
            registerForCallBack.clear()
        }


    }

}

interface SafetyConnectCommunicator {

    fun onCrashFallDetected(sensorResponse: SensorResponse, crashDetectionEdge: String?)
    fun locationPermissionNotGranted()
//    fun activityRecognisitionPermissionNotGranted()
    fun getFeedbackCallback(sensorResponse: SensorResponse)
    fun notificationPermissionsRequired()
    fun turnOnGpsLocation()
    fun onSdkInitialized()
    fun overSpeedDetected(location: Location?, speedDetectionEdge: String?)
    fun magneticFieldDetected(sqrt: Double, emfDetectionEdge: String?)
    fun onHarshDrivingDetected(speed: Float?, harshDrivingEdge: String?, eventType: String)

}

enum class SafetyTypes {
    CRASH_DETECTION,
    SPEED_DETECTION,
    EMF_DETECTION
}

data class SensorFilters(
    var isEMFDetectionEnabled: Boolean? = false,
    var isEMFACDetectionEnabled: Boolean? = true,
    var maxEmfThreshold: Float? = 60.0f,
    var maxACEmfThreshold: Float? = 60.0f,
    var maxAcEmfValue: Float? = 5000.0f,
    var isSpeedDetectionEnabled: Boolean? = false,
    var maxSpeedThreshold: Float? = 60.0f,
    var isFallCrashDetectionEnabled: Boolean? = false,
    var networkCallFrequency: Long? = 15000,
    var sensorDataFrequency: Long? = 15000,
    var isFeedbackRequired: Boolean = false,
    var crashDetectionEdge: String? = null,
    var emfDetectionEdge: String? = null,
    var speedDetectionEdge: String? = null,
    var speedCallBackFrequency: Long? = 30 * 1000L,
    var emfCallBackFrequency: Long? = 30 * 1000L,
    var crashCallBackFrequency: Long? = 30 * 1000L,
    var harshDrivingThresholdInKm: Float? = 3.0F,
    var harshDrivingDurationFrequency: Long? = 3000L,
    var harshDrivingCallbackFrequency: Long? = 90 * 1000,
    var harshDrivingEdge: String? = null,
    var harshDrivingCaptureEnabled: Boolean? = false,
    var disableEmfMinimumThresholdInKm: Float? = 3.0f,
    var safetyType: SafetyTypes? = null,
    val configuredEmfBeepDistance: Double? = 10.0,
    val configuredEmfBeepInterval: Long? = 90 * 1000L

) {
}