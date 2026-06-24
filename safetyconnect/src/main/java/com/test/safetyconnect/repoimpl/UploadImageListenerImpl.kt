package com.test.safetyconnect.repoimpl

import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.annotation.NonNull
import com.test.safetyconnect.baseresponse.BaseInteractor
import com.test.safetyconnect.exception.ApiResponseException
import com.test.safetyconnect.model.UploadImageResponse
import com.test.safetyconnect.network.NetworkModule
import com.test.safetyconnect.repolistner.UploadImageListener
import com.test.safetyconnect.utils.Logger
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File

/**
 * Created on 01/01/23.
 */
class UploadImageListenerImpl(var networkModule: NetworkModule?) : UploadImageListener {

    private lateinit var disposable: Disposable

    override fun uploadImage(
        uri: Uri?,
        responseListener: BaseInteractor.IResponseListener<UploadImageResponse>
    ) {
        responseListener.onProgress()
        val retailerSignaturePathBody: MultipartBody.Part? = uri?.let {
            prepareFilePart(
                "file1",
                it
            )
        }

        disposable = networkModule?.getApiService()?.uploadImage(retailerSignaturePathBody)
            ?.map { resp ->
                if (200 == resp.status) {
                    Logger.printLogE(tag = "getNewresponse", msg = "  >> uploadImage: $resp")
                    resp
                } else {
                    Logger.printLogE(tag = "getNewresponse", msg = "uploadImage")
                    throw ApiResponseException(
                        resp.status!!,
                        resp.message
                    )
                }
            }
            ?.subscribeOn(Schedulers.io())
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.subscribe(responseListener::onSuccess, responseListener::onFailure)!!
    }

    override fun uploadImageV2(uri: Uri?, responseListener: BaseInteractor.IResponseListener<UploadImageResponse>) {
        responseListener.onProgress()
        val retailerSignaturePathBody: MultipartBody.Part? = uri?.let {
            prepareFilePart(
                "file1",
                it
            )
        }

        disposable = networkModule?.getApiService()?.uploadImageForBikeHelmet(retailerSignaturePathBody)
            ?.map { resp ->
                if (200 == resp.status) {
                    Logger.printLogE(tag = "getNewresponse", msg = "  >> uploadImage: $resp")
                    resp
                } else {
                    Logger.printLogE(tag = "getNewresponse", msg = "uploadImage")
                    throw ApiResponseException(
                        resp.status!!,
                        resp.message
                    )
                }
            }
            ?.subscribeOn(Schedulers.io())
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.subscribe(responseListener::onSuccess, responseListener::onFailure)!!
    }


    @NonNull
    private fun prepareFilePart(partName: String, fileUri: Uri): MultipartBody.Part? {
        // use the FileUtils to get the actual file by uri
        val file = File(fileUri.path)
        // create RequestBody instance from file
        val requestFile: RequestBody = RequestBody.create(
            MediaType.parse(getMimeType(fileUri.path)),
            file
        )
        // MultipartBody.Part is used to send also the actual file name
        return MultipartBody.Part.createFormData(partName, file.name, requestFile)
    }

    fun getMimeType(url: String?): String? {
        var type: String? = null
        val extension: String = MimeTypeMap.getFileExtensionFromUrl(url)
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        }
        return type
    }


    override fun unbind() {
        disposable.dispose()
    }
}
