package com.test.safetyconnect.repoimpl

import android.content.Context
import android.util.Log
import com.test.safetyconnect.baseresponse.BaseInteractor
import com.test.safetyconnect.model.AccelerometerDataModel
import com.test.safetyconnect.model.GyroscopeDataModel
import com.test.safetyconnect.model.MagnetometerDataModel
import com.test.safetyconnect.model.SensorResponse
import com.test.safetyconnect.network.NetworkModule
import com.test.safetyconnect.repolistner.SensorDataListener
import com.test.safetyconnect.utils.Logger
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Created on 01/01/23.
 */
class DataInteractImpl(val context: Context?,val frequency: Long,var responseCallback: (SensorResponse) -> Unit) :
    SensorDataListener {

    private var service: NetworkModule? = null
    private var repository: SendDataListenerImpl? = null

    //Actual Data
    private var accelerometerDataModelList = CopyOnWriteArrayList<AccelerometerDataModel>()
    private var gyroscopeDataModelList = CopyOnWriteArrayList<GyroscopeDataModel>()
    private var magnetometerDataModelList = CopyOnWriteArrayList<MagnetometerDataModel>()

    //Interval Temp Data
    private var accelerometerDataModelListTemp = CopyOnWriteArrayList<AccelerometerDataModel>()
    private var gyroscopeDataModelListTemp = CopyOnWriteArrayList<GyroscopeDataModel>()
    private var magnetometerDataModelListTemp = CopyOnWriteArrayList<MagnetometerDataModel>()


    private var mTimerTask: TimerTask? = null
    private val timer = Timer()

    private fun initListener() {
        //Service Layer
        service = NetworkModule(context)

        //Repository Layer
        repository = SendDataListenerImpl(service)

        onTimerStart()
    }

    override fun onBind() {
        initListener()
    }

    override fun addAccelerometerData(ax: Float, ay: Float, ats: Float) {
        accelerometerDataModelList.add(AccelerometerDataModel(ax, ay, ats))
    }

    override fun addMagnetometerMeterData(ax: Float, ay: Float, ats: Float) {
        magnetometerDataModelList.add(MagnetometerDataModel(ax, ay, ats))
    }

    override fun addGyroscopeData(ax: Float, ay: Float, ats: Float) {
        gyroscopeDataModelList.add(GyroscopeDataModel(ax, ay, ats))
    }

    override fun onUpdate(
        accelerometerDataModelList: CopyOnWriteArrayList<AccelerometerDataModel>,
        gyroscopeDataModelList: CopyOnWriteArrayList<GyroscopeDataModel>,
        magnetometerDataModelList: CopyOnWriteArrayList<MagnetometerDataModel>
    ) {
        onClean(
            accelerometerDataModelListTemp,
            gyroscopeDataModelListTemp,
            magnetometerDataModelListTemp
        )
        accelerometerDataModelListTemp.addAll(accelerometerDataModelList)
        gyroscopeDataModelListTemp.addAll(gyroscopeDataModelList)
        magnetometerDataModelListTemp.addAll(magnetometerDataModelList)
        onClean(accelerometerDataModelList, gyroscopeDataModelList, magnetometerDataModelList)
    }

    override fun onClean(
        accelerometerDataModelList: CopyOnWriteArrayList<AccelerometerDataModel>,
        gyroscopeDataModelList: CopyOnWriteArrayList<GyroscopeDataModel>,
        magnetometerDataModelList: CopyOnWriteArrayList<MagnetometerDataModel>
    ) {
        accelerometerDataModelList.clear()
        gyroscopeDataModelList.clear()
        magnetometerDataModelList.clear()
    }

    override fun onTimerStart() {
        mTimerTask = object : TimerTask() {
            override fun run() {

                Logger.printLogE(tag="datacapture",msg= "TimerTask Start " +
                        " Acc Size:" + accelerometerDataModelList.size
                        + " Gyro Size:" + gyroscopeDataModelList.size
                        + " Magn Size:" + magnetometerDataModelList.size + " -- End")

                onUpdate(
                    accelerometerDataModelList,
                    gyroscopeDataModelList,
                    magnetometerDataModelList
                )

                onSend()
            }
        }
        timer.schedule(mTimerTask, 3000,frequency)
    }

    override fun onSend() {
        Log.e("datacapture", "onSend")

        repository?.sendSensorData(context,
            accelerometerDataModelListTemp,
            gyroscopeDataModelListTemp,
            magnetometerDataModelListTemp,
            object : BaseInteractor.IResponseListener<SensorResponse> {
                override fun onProgress() {
                    Logger.printLogE(tag="datacapture",msg="onProgress ")
                }

                override fun onSuccess(resp: SensorResponse) {
                    Logger.printLogE(tag="datacapture",msg="Success ")

                    responseCallback.invoke(resp)
                }

                override fun onFailure(t: Throwable) {
                    Logger.printLogE(tag="datacapture",msg="Failure " + t.printStackTrace())

                    responseCallback.invoke(SensorResponse(message = t.printStackTrace().toString()))

                }

            })
    }

    override fun onUnBind() {
        repository?.unbind()
    }
}