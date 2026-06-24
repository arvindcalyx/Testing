package com.test.safetyconnect.repolistner

import com.test.safetyconnect.model.AccelerometerDataModel
import com.test.safetyconnect.model.GyroscopeDataModel
import com.test.safetyconnect.model.MagnetometerDataModel
import java.util.concurrent.CopyOnWriteArrayList

interface SensorDataListener {
    fun onBind()
    fun addAccelerometerData(ax: Float, ay: Float, ats: Float)
    fun addMagnetometerMeterData(ax: Float, ay: Float, ats: Float)
    fun addGyroscopeData(ax: Float, ay: Float, ats: Float)
    fun onUpdate(accelerometerDataModelList: CopyOnWriteArrayList<AccelerometerDataModel>,
                 gyroscopeDataModelList: CopyOnWriteArrayList<GyroscopeDataModel>,
                 magnetometerDataModelList: CopyOnWriteArrayList<MagnetometerDataModel>
    )
    fun onClean(accelerometerDataModelList: CopyOnWriteArrayList<AccelerometerDataModel>,
                gyroscopeDataModelList: CopyOnWriteArrayList<GyroscopeDataModel>,
                magnetometerDataModelList: CopyOnWriteArrayList<MagnetometerDataModel>)
    fun onTimerStart()
    fun onSend()
    fun onUnBind()
}
