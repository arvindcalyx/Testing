package com.test.safetyconnect.repolistner

import android.content.Context
import com.test.safetyconnect.baseresponse.BaseInteractor
import com.test.safetyconnect.model.AccelerometerDataModel
import com.test.safetyconnect.model.GyroscopeDataModel
import com.test.safetyconnect.model.MagnetometerDataModel
import com.test.safetyconnect.model.SensorResponse
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Created on 01/01/23.
 */
interface SendDataListener: BaseInteractor {
    fun sendSensorData(
        context: Context?,
        accelerometerDataModelList: CopyOnWriteArrayList<AccelerometerDataModel> = CopyOnWriteArrayList<AccelerometerDataModel>(),
        gyroscopeDataModelList: CopyOnWriteArrayList<GyroscopeDataModel> = CopyOnWriteArrayList<GyroscopeDataModel>(),
        magnetometerDataModelList: CopyOnWriteArrayList<MagnetometerDataModel> = CopyOnWriteArrayList<MagnetometerDataModel>(),
        responseListener: BaseInteractor.IResponseListener<SensorResponse>
    )
}
