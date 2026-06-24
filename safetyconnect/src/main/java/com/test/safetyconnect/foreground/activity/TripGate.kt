package com.test.safetyconnect.foreground.activity

import SafetyConnectSDK
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionEvent
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Gates telematics on whether the user is actually driving.
 *
 * Uses the modern [ActivityRecognition] transition API (replacing the
 * deprecated IntentService + LocalBroadcastManager approach) to subscribe to
 * ENTER/EXIT transitions and exposes [isDriving] for consumers to gate on.
 *
 * isDriving:
 *  - becomes true only after a sustained IN_VEHICLE period
 *    (sensorFilters.inVehicleSustainSeconds, default 30s)
 *  - becomes false immediately on IN_VEHICLE EXIT
 *  - becomes false on WALKING / RUNNING / ON_BICYCLE ENTER
 */
class TripGate {

    private val _isDriving = MutableStateFlow(false)
    val isDriving: StateFlow<Boolean> = _isDriving.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var sustainJob: Job? = null

    private var pendingIntent: PendingIntent? = null
    private var receiver: BroadcastReceiver? = null
    private var registered = false

    private val monitoredActivities = listOf(
        DetectedActivity.IN_VEHICLE,
        DetectedActivity.WALKING,
        DetectedActivity.RUNNING,
        DetectedActivity.ON_BICYCLE,
        DetectedActivity.STILL
    )

    @SuppressLint("MissingPermission")
    fun start(context: Context) {
        if (registered) return
        val appContext = context.applicationContext

        receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                intent ?: return
                if (ActivityTransitionResult.hasResult(intent)) {
                    val result = ActivityTransitionResult.extractResult(intent) ?: return
                    for (event in result.transitionEvents) {
                        handleTransition(event)
                    }
                }
            }
        }

        val filter = IntentFilter(ACTION_TRANSITIONS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            appContext.registerReceiver(receiver, filter)
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pi = PendingIntent.getBroadcast(
            appContext,
            REQUEST_CODE,
            Intent(ACTION_TRANSITIONS).setPackage(appContext.packageName),
            flags
        )
        pendingIntent = pi

        val request = ActivityTransitionRequest(buildTransitions())

        try {
            ActivityRecognition.getClient(appContext)
                .requestActivityTransitionUpdates(request, pi)
                .addOnSuccessListener { Timber.d("TripGate: transition updates registered") }
                .addOnFailureListener { e -> Timber.e(e, "TripGate: failed to register transition updates") }
        } catch (e: SecurityException) {
            Timber.e(e, "TripGate: missing ACTIVITY_RECOGNITION permission")
        }

        registered = true
    }

    @SuppressLint("MissingPermission")
    fun stop(context: Context) {
        val appContext = context.applicationContext
        sustainJob?.cancel()
        sustainJob = null

        pendingIntent?.let { pi ->
            try {
                ActivityRecognition.getClient(appContext).removeActivityTransitionUpdates(pi)
            } catch (e: SecurityException) {
                Timber.e(e, "TripGate: failed to remove transition updates")
            }
            pi.cancel()
        }
        pendingIntent = null

        if (registered) {
            receiver?.let {
                try {
                    appContext.unregisterReceiver(it)
                } catch (e: IllegalArgumentException) {
                    // Receiver was not registered; ignore.
                }
            }
            registered = false
        }
        receiver = null
        _isDriving.value = false
    }

    private fun handleTransition(event: ActivityTransitionEvent) {
        val enter = event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER
        val exit = event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_EXIT
        when (event.activityType) {
            DetectedActivity.IN_VEHICLE -> {
                when {
                    enter -> scheduleDrivingConfirm()
                    exit -> setNotDriving("IN_VEHICLE exit")
                }
            }
            DetectedActivity.WALKING,
            DetectedActivity.RUNNING,
            DetectedActivity.ON_BICYCLE -> {
                if (enter) setNotDriving("non-vehicle activity ${event.activityType}")
            }
            // STILL: no state change (e.g. stopped at a light while driving).
        }
    }

    private fun scheduleDrivingConfirm() {
        sustainJob?.cancel()
        val sustainSeconds = SafetyConnectSDK.sensorFilters?.inVehicleSustainSeconds ?: 30
        sustainJob = scope.launch {
            delay(sustainSeconds * 1000L)
            _isDriving.value = true
            Timber.d("TripGate: IN_VEHICLE sustained ${sustainSeconds}s -> isDriving=true")
        }
    }

    private fun setNotDriving(reason: String) {
        sustainJob?.cancel()
        sustainJob = null
        if (_isDriving.value) {
            Timber.d("TripGate: $reason -> isDriving=false")
        }
        _isDriving.value = false
    }

    private fun buildTransitions(): List<ActivityTransition> {
        val transitions = mutableListOf<ActivityTransition>()
        for (activity in monitoredActivities) {
            transitions.add(
                ActivityTransition.Builder()
                    .setActivityType(activity)
                    .setActivityTransitionType(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build()
            )
            transitions.add(
                ActivityTransition.Builder()
                    .setActivityType(activity)
                    .setActivityTransitionType(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                    .build()
            )
        }
        return transitions
    }

    companion object {
        private const val ACTION_TRANSITIONS = "com.test.safetyconnect.ACTION_ACTIVITY_TRANSITIONS"
        private const val REQUEST_CODE = 4321
    }
}
