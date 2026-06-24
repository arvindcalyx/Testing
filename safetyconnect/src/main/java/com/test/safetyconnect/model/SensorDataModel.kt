package com.test.safetyconnect.model

import java.util.concurrent.CopyOnWriteArrayList

/**
 * Created on 01/01/23.
 */
class SensorDataModel(
    val name: String? = null,
    val data: SensorData? = null
)

class SensorData(
    var acc: CopyOnWriteArrayList<AccelerometerDataModel> = CopyOnWriteArrayList<AccelerometerDataModel>(),
    var gyro: CopyOnWriteArrayList<GyroscopeDataModel> = CopyOnWriteArrayList<GyroscopeDataModel>(),
    var mag: CopyOnWriteArrayList<MagnetometerDataModel> = CopyOnWriteArrayList<MagnetometerDataModel>()

)