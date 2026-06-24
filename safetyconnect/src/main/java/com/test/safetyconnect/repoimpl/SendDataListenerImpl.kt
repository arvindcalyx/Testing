package com.test.safetyconnect.repoimpl

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import com.test.safetyconnect.exception.ApiResponseException
import com.test.safetyconnect.baseresponse.BaseInteractor
import com.test.safetyconnect.model.*
import com.test.safetyconnect.network.NetworkModule
import com.test.safetyconnect.repolistner.SendDataListener
import com.test.safetyconnect.utils.Logger
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Created on 01/01/23.
 */
class SendDataListenerImpl(var networkModule: NetworkModule?) : SendDataListener {
    private lateinit var disposable: Disposable


    @SuppressLint("CheckResult")
    override fun sendSensorData(
        context:Context?,
        accelerometerDataModelList: CopyOnWriteArrayList<AccelerometerDataModel>,
        gyroscopeDataModelList: CopyOnWriteArrayList<GyroscopeDataModel>,
        magnetometerDataModelList: CopyOnWriteArrayList<MagnetometerDataModel>,
        responseListener: BaseInteractor.IResponseListener<SensorResponse>
    ) {
        val deviceDetails = Settings.Secure.getString(context?.contentResolver, Settings.Secure.ANDROID_ID) + Build.MANUFACTURER + Build.MODEL


        disposable = networkModule?.getApiService()?.updateSensorData(
            SensorDataModel(
                deviceDetails,
                SensorData(
                    accelerometerDataModelList,
                    gyroscopeDataModelList,
                    magnetometerDataModelList
                )
            )
        )
            ?.map { resp ->
                if (200 == resp.status) {
                    Logger.printLogE(tag="getNewresponse",msg="  >> sendSensorData Resp: $resp")
                    resp
                } else {
                    Logger.printLogE(tag="getNewresponse",msg=" >> ApiResponseException")

                    throw ApiResponseException(
                        resp.status,
                        resp.message
                    )
                }
            }
            ?.subscribeOn(Schedulers.io())
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.subscribe(responseListener::onSuccess, responseListener::onFailure)!!
    }

    override fun unbind() {
        disposable.dispose()
    }
}
