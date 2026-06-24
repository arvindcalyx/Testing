package com.test.safetyconnect.activityrecognition

import android.app.IntentService
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity

open class DetectedActivitiesIntentService : IntentService(TAG) {

    @Deprecated("Deprecated in Java")
    override fun onHandleIntent(intent: Intent?) {
        val result = intent?.let { ActivityRecognitionResult.extractResult(it) }
        // Get the list of the probable activities associated with the current state of the
        // device. Each activity is associated with a confidence level, which is an int between
        // 0 and 100.
        val detectedActivities: ArrayList<DetectedActivity?>? =
            result?.probableActivities as? ArrayList<DetectedActivity?>

        if (detectedActivities != null) {
            for (activity in detectedActivities) {
                Log.e(TAG, "Detected activity: " + activity?.type + ", " + activity?.confidence)
                activity?.let { broadcastActivity(it) }
            }
        }
    }

    private fun broadcastActivity(activity: DetectedActivity) {
        val intent = Intent(Constants.BROADCAST_DETECTED_ACTIVITY)
        intent.putExtra("type", activity.type)
        intent.putExtra("confidence", activity.confidence)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    companion object {
        protected val TAG: String = DetectedActivitiesIntentService::class.java.simpleName
    }
}