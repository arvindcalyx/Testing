package com.test.safetyconnect.sdkinit

import SensorFilters
import android.content.Context

/**
 * Created on 01/01/23.
 */
class SafetyConnect {
    private var sensorFilters: SensorFilters? = null
    private var accidentDetector: AccidentDetector? = null

    private var imageDetector: ImageDetector? = null

    var context: Context? = null

    private var accidentDetectorEnable: Boolean = false
    private var enableFeedback: Boolean = false
    private var captureFrequency: Long = 15 * 1000

    private var safetyEquipmentDetectionFeature: Boolean = false
    private var safetyEquipmentAPIResponse: Boolean = false


    fun init(context: Context?, sensorFilters: SensorFilters?): SafetyConnect {
        this.context = context
        this.sensorFilters = sensorFilters
        return this
    }

    //Crash Detection Feature

    fun initSensor(): SafetyConnect {
        context?.let { context ->
            accidentDetector = AccidentDetector(
                context,
                accidentDetectorEnable_ = accidentDetectorEnable,
                enableFeedback_ = enableFeedback
            )
            accidentDetector?.enableSensorFrequency(captureFrequency)
        }
        return this
    }

    fun enableSafetySensor(accidentDetectorEnable: Boolean): SafetyConnect {
        this.accidentDetectorEnable = accidentDetectorEnable
        return this
    }

    fun enableSafetySensorFeedback(enableFeedback_: Boolean): SafetyConnect {
        enableFeedback = enableFeedback_
        return this
    }

    fun enableSensorFrequency(captureFrequency_: Long): SafetyConnect {
        captureFrequency = captureFrequency_
        return this
    }

    fun getAccidentDetectorInstance(): AccidentDetector? {
        return accidentDetector!!
    }


    //Image Equipment Detection Feature


    fun initSafetyEquipmentDetection(): SafetyConnect {
        context?.let { context ->
            if (imageDetector == null) {
                imageDetector = ImageDetector(context, false)
            }
        }
        return this
    }

    fun enableSafetyEquipmentDetectionFeature(safetyEquipmentDetectionFeature_: Boolean): SafetyConnect {
        safetyEquipmentDetectionFeature = safetyEquipmentDetectionFeature_
        return this
    }

    fun enableSafetyEquipmentAPIResponse(safetyEquipmentAPIResponse_: Boolean): SafetyConnect {
        safetyEquipmentAPIResponse = safetyEquipmentAPIResponse_
        return this
    }

    fun getImageDetectorInstance(): ImageDetector? {
        return imageDetector!!
    }
}