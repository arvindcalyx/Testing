package com.test.safetyconnect.repoimpl

import android.content.Context
import com.test.safetyconnect.sensor.Accelerometer
import com.test.safetyconnect.sensor.Gyroscope
import com.test.safetyconnect.sensor.Magnetometer
import com.test.safetyconnect.sensor.SensorLifeCycle
import com.test.safetyconnect.model.SensorResponse

/**
 * Created on 01/01/23.
 */
class SensorInteractImpl(val context: Context,val frequency: Long,var accelerometerData: (String) -> Unit,var gyroscopeData: (String) -> Unit,var magnetometerData: (String) -> Unit,var responseCallback: (SensorResponse) -> Unit) : SensorLifeCycle,
    Accelerometer.Listener,
    Gyroscope.Listener, Magnetometer.Listener {

    private var accelerometer: Accelerometer? = null
    private var gyroscope: Gyroscope? = null
    private var magnetometer: Magnetometer? = null


     var dataInteractImpl: DataInteractImpl? = null

    init {
        //Data Layer

        dataInteractImpl = DataInteractImpl(context,frequency) { sensorResponse ->
            responseCallback.invoke(
                sensorResponse
            )
        }
        dataInteractImpl?.onBind()

        //Sensors Layer

        accelerometer = Accelerometer(context)
        gyroscope = Gyroscope(context)
        magnetometer = Magnetometer(context)

        initListener()
    }

    override fun onBind() {
        registerSensor()
    }

    private fun registerSensor() {
        accelerometer?.register()
        gyroscope?.register()
        magnetometer?.register()
    }

    private fun initListener() {
        accelerometer?.setListener(this)
        gyroscope?.setListener(this)
        magnetometer?.setListener(this)
    }


    override fun onUnBind() {
        accelerometer?.unregister()
        gyroscope?.unregister()
        magnetometer?.unregister()
        dataInteractImpl?.onUnBind()
    }

    override fun onTranslation(ax: Float, ay: Float, ats: Float) {
        //on translation method of accelerometer
        accelerometerData("Accelerometer:\n ax:$ax\nay:$ay\naz:$ats")
        dataInteractImpl?.addAccelerometerData(ax, ay, ats)
    }

    override fun onRotation(gx: Float, gy: Float, gts: Float) {
        // on rotation method of gyroscope
        gyroscopeData("Gyroscope:\n rx:$gx\nry:$gy\nrz:$gts")
        dataInteractImpl?.addGyroscopeData(gx, gy, gts)
    }

    override fun onDirectionChange(mx: Float, my: Float, mts: Float) {
        // on rotation method of magnetometer
        magnetometerData("Magnetometer:\n mx:$mx\nmy:$my\nmz:$mts")
        dataInteractImpl?.addMagnetometerMeterData(mx, my, mts)
    }

}