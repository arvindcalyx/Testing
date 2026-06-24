package com.test.safetyconnect.sdkinit

import SafetyConnectSDK
import SafetyConnectSDK.Companion.notifyAllOnCrashFallDetectedListener
import android.content.Context
import android.widget.Toast
import com.test.safetyconnect.baseresponse.BaseInteractor
import com.test.safetyconnect.customui.LoadImage
import com.test.safetyconnect.model.FeadbackRequestModel
import com.test.safetyconnect.model.SensorResponse
import com.test.safetyconnect.network.NetworkModule
import com.test.safetyconnect.repoimpl.SendFeedbackDataListenerImpl
import com.test.safetyconnect.repoimpl.SensorInteractImpl
import com.test.safetyconnect.utils.Logger

/**
 * Created on 01/01/23.
 */
class AccidentDetector(
    val context: Context,
    private val accidentDetectorEnable_: Boolean,
    private val enableFeedback_: Boolean
) {
    private var lastCrashFallDetected = 0L
    private var sensorInteractImpl: SensorInteractImpl? = null
    private var frequency: Long = 15 * 1000
    private var sendFeadbackDataListenerImpl: SendFeedbackDataListenerImpl? = null
    var responseCallback: ((SensorResponse) -> Unit)? = null

    fun getAccidentDetectorInit(frequency_: Long?, responseCallback: ((SensorResponse) -> Unit)?) {
        frequency_?.let { frequency = it }

        if (accidentDetectorEnable_) {
            if (frequency < 30000) {
                frequency = 30 * 1000
            }
            sensorInteractImpl = SensorInteractImpl(context, frequency,
                { },
                { },
                { },
                { sensorResponse ->
                    responseCallback?.invoke(sensorResponse)
                    printLog(sensorResponse.message.toString())
                    if (sensorResponse.crash?.equals("1") == true) {
                        setFeedback(sensorResponse)
                    }
                })
        } else {
            printLog("Feature Not Enabled")
        }
    }

    fun initSensorsAPIResponse(responseCallback: ((SensorResponse) -> Unit)?) {

        if (accidentDetectorEnable_) {
            frequency = (SafetyConnectSDK.sensorFilters?.networkCallFrequency ?: 15000)
            sensorInteractImpl = SensorInteractImpl(context, frequency,
                { },
                { },
                { },
                { sensorResponse ->
                    responseCallback?.invoke(sensorResponse)
                    printLog(sensorResponse.message.toString())
                    if (sensorResponse.crash?.equals("1") == true) {
                        if (lastCrashFallDetected == 0L) {
                            lastCrashFallDetected = System.currentTimeMillis()
                        }
                        if (System.currentTimeMillis().minus(lastCrashFallDetected) >= (SafetyConnectSDK.sensorFilters?.crashCallBackFrequency
                                ?: (30 * 1000L))
                        ) {
                           notifyAllOnCrashFallDetectedListener(
                                sensorResponse,
                                SafetyConnectSDK.sensorFilters?.crashDetectionEdge
                            )
                            lastCrashFallDetected = System.currentTimeMillis()
                        }
                        when (SafetyConnectSDK.sensorFilters?.isFeedbackRequired) {
                            true -> {
                                setFeedback(sensorResponse)
                            }
                            false -> {}
                            null -> {}
                        }
                    }
                })
        } else {
            printLog("Feature Not Enabled")
        }
    }

    fun enableSensorFrequency(captureFrequency_: Long): AccidentDetector {
        frequency = captureFrequency_
        return this
    }

    private fun setFeedback(sensorResponse: SensorResponse?) {
        if (enableFeedback_) {
            LoadImage.getBannerImage(context) { callBack ->
                var label = ""
                if (callBack) {
                    label = "1"
                } else {
                    label = "0"
                }
                sendFeedbackAPI(FeadbackRequestModel(sensorResponse?.batch, label))
            }
        }
    }


    private fun sendFeedbackAPI(feedbackRequestModel: FeadbackRequestModel?) {
        LoadImage.dismissDialog()
        sendFeadbackDataListenerImpl = SendFeedbackDataListenerImpl(NetworkModule(context))
        sendFeadbackDataListenerImpl?.sendFeadbackData(context,
            feedbackRequestModel,
            object : BaseInteractor.IResponseListener<SensorResponse> {
                override fun onProgress() {

                }

                override fun onSuccess(resp: SensorResponse) {
                    Toast.makeText(context, "Response Submitted", Toast.LENGTH_LONG).show()
                    responseCallback?.invoke(resp)
                }

                override fun onFailure(t: Throwable) {
                    Toast.makeText(context, "Response Failure", Toast.LENGTH_LONG).show()
                    responseCallback?.invoke(SensorResponse(message = t.message))
                }

            }

        )

    }


    fun getFeedbackCallback(responseCallback_: ((SensorResponse) -> Unit)?) {
        responseCallback = responseCallback_
    }


    private fun printLog(logName: String) {
        Logger.printLogE(tag = "AccidentDetector", msg = logName)
    }


    fun bindSensor() {
        if (accidentDetectorEnable_) {
            sensorInteractImpl?.onBind()
        }
    }

    fun unBindSensor() {
        if (accidentDetectorEnable_) {
            sensorInteractImpl?.onUnBind()
        }
    }


}