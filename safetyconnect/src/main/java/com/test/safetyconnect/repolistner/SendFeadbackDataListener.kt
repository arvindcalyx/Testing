package com.test.safetyconnect.repolistner

import android.content.Context
import com.test.safetyconnect.baseresponse.BaseInteractor
import com.test.safetyconnect.model.FeadbackRequestModel
import com.test.safetyconnect.model.ImageDetectionFeedbackRequestModel
import com.test.safetyconnect.model.SensorResponse

/**
 * Created on 01/01/23.
 */
interface SendFeadbackDataListener: BaseInteractor {
    fun sendFeadbackData(
        context: Context?,
        feadbackRequestModel: FeadbackRequestModel?,
        responseListener: BaseInteractor.IResponseListener<SensorResponse>
    )

    fun sendImageDetectionFeedbackData(context: Context?, imageDetectionFeedbackModel: ImageDetectionFeedbackRequestModel?, responseListener: BaseInteractor.IResponseListener<SensorResponse>)
}
