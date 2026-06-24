package com.test.safetyconnect.repoimpl

import android.content.Context
import com.test.safetyconnect.baseresponse.BaseInteractor
import com.test.safetyconnect.exception.ApiResponseException
import com.test.safetyconnect.model.FeadbackRequestModel
import com.test.safetyconnect.model.ImageDetectionFeedbackRequestModel
import com.test.safetyconnect.model.SensorResponse
import com.test.safetyconnect.network.NetworkModule
import com.test.safetyconnect.repolistner.SendFeadbackDataListener
import com.test.safetyconnect.utils.Logger
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

/**
 * Created on 01/01/23.
 */
class SendFeedbackDataListenerImpl(var networkModule: NetworkModule?) : SendFeadbackDataListener {
    private lateinit var disposable: Disposable
    override fun sendFeadbackData(
        context: Context?,
        feadbackRequestModel: FeadbackRequestModel?,
        responseListener: BaseInteractor.IResponseListener<SensorResponse>
    ) {

        disposable = networkModule?.getApiService()?.updateFeadbackSensorData(feadbackRequestModel)
            ?.map { resp ->
                if (200 == resp.status) {
                    Logger.printLogE(tag = "getNewresponse", msg = " >> sendFeadbackData: $resp")
                    resp
                } else {
                    Logger.printLogE(tag = "getNewresponse", msg = " ApiResponseException")

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

    override fun sendImageDetectionFeedbackData(context: Context?, imageDetectionFeedbackModel: ImageDetectionFeedbackRequestModel?, responseListener: BaseInteractor.IResponseListener<SensorResponse>) {
        disposable = networkModule?.getApiService()?.updateImageDetectionFeedbackData(imageDetectionFeedbackModel)
            ?.map { resp ->
                if (200 == resp.status) {
                    Logger.printLogE(tag = "getNewresponse", msg = " >> sendFeedbackData: $resp")
                    resp
                } else {
                    Logger.printLogE(tag = "getNewresponse", msg = " ApiResponseException")

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
