package com.test.safetyconnect.repolistner

import android.net.Uri
import com.test.safetyconnect.baseresponse.BaseInteractor
import com.test.safetyconnect.model.UploadImageResponse

/**
 * Created on 01/01/23.
 */
interface UploadImageListener : BaseInteractor {
    fun uploadImage(
        uri: Uri?,
        responseListener: BaseInteractor.IResponseListener<UploadImageResponse>
    )

    fun uploadImageV2(
        uri: Uri?,
        responseListener: BaseInteractor.IResponseListener<UploadImageResponse>
    )
}
