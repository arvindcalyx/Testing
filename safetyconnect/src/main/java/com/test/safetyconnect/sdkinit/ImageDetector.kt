package com.test.safetyconnect.sdkinit

import SafetyConnectSDK
import android.content.Context
import android.net.Uri
import android.widget.Toast
import com.test.safetyconnect.baseresponse.BaseInteractor
import com.test.safetyconnect.customui.CustomAlertDialog
import com.test.safetyconnect.customui.CustomSnackProgressDialog
import com.test.safetyconnect.model.ImageDetectionFeedbackRequestModel
import com.test.safetyconnect.model.SensorResponse
import com.test.safetyconnect.model.UploadImageResponse
import com.test.safetyconnect.network.NetworkModule
import com.test.safetyconnect.repoimpl.SendFeedbackDataListenerImpl
import com.test.safetyconnect.repoimpl.UploadImageListenerImpl
import com.test.safetyconnect.utils.Logger

/**
 * Created on 01/01/23.
 */
class ImageDetector(val context: Context, private val showResponseDialog: Boolean = false) {

    private var uploadImageListenerImpl: UploadImageListenerImpl? = null
    private var sendFeedbackDataListenerImpl: SendFeedbackDataListenerImpl? = null

    fun imageDetectorInit(): ImageDetector {
        uploadImageListenerImpl = UploadImageListenerImpl(NetworkModule(context))
        return this
    }

    fun uploadImage(imageUri: Uri?, responseCallback_: ((UploadImageResponse?) -> Unit)?) {
        uploadImageListenerImpl?.uploadImage(imageUri, object : BaseInteractor.IResponseListener<UploadImageResponse> {

            override fun onProgress() {
                CustomSnackProgressDialog.getSnackBarDialog(
                    context, "Uploading please wait ...."
                )?.show()
            }

            override fun onSuccess(resp: UploadImageResponse) {
                CustomSnackProgressDialog.getSnackBarDialog(context)?.dismiss()
                if (showResponseDialog) {
                    CustomAlertDialog.showFailureDialog(
                        "Response Submitted \n\n" + "Harness= " + resp.equipment?.Safety_Harness + "\n" + "Safety_Helmet= " + resp.equipment?.Helmet + "\n" + "Ladder= " + resp.equipment?.Ladder + "\n" + "Safety_Jacket= " + resp.equipment?.Safety_Jacket + "\n" +
//                                    "Bike_Helmet= " + resp.equipment?.Bike_Helmet + "\n" +
                                "Worker= " + resp.equipment?.Worker, context
                    )
                }
                responseCallback_?.invoke(resp)
                printLog(resp.toString())
            }

            override fun onFailure(t: Throwable) {
                CustomSnackProgressDialog.getSnackBarDialog(context)?.dismiss()
                printLog(t.stackTraceToString())
                Toast.makeText(context, "Response Failure", Toast.LENGTH_LONG).show()
                responseCallback_?.invoke(null)

            }
        })

    }

    fun uploadImageV2(imageUri: Uri?, responseCallback_: ((UploadImageResponse?) -> Unit)?) {
        uploadImageListenerImpl?.uploadImageV2(imageUri, object : BaseInteractor.IResponseListener<UploadImageResponse> {

            override fun onProgress() {
                CustomSnackProgressDialog.getSnackBarDialog(context, "Uploading please wait ....")?.show()
            }

            override fun onSuccess(resp: UploadImageResponse) {
                CustomSnackProgressDialog.getSnackBarDialog(context)?.dismiss()
                if (showResponseDialog) {
                    CustomAlertDialog.showFailureDialog(
                        "Response Submitted \n\n" +
//                                    "Harness= " + resp.equipment?.Safety_Harness + "\n" +
//                                    "Safety_Helmet= " + resp.equipment?.Helmet + "\n" +
//                                    "Ladder= " + resp.equipment?.Ladder + "\n" +
//                                    "Safety_Jacket= " + resp.equipment?.Safety_Jacket + "\n" +
                                "Bike_Helmet= " + resp.equipment?.Bike_Helmet + "\n"
//                                    "Worker= " + resp.equipment?.Worker,
                        , context
                    )
                }
                responseCallback_?.invoke(resp)
                printLog(resp.toString())
            }

            override fun onFailure(t: Throwable) {
                CustomSnackProgressDialog.getSnackBarDialog(context)?.dismiss()
                printLog(t.stackTraceToString())
                Toast.makeText(context, "Response Failure", Toast.LENGTH_LONG).show()
                responseCallback_?.invoke(null)

            }
        })
    }

    fun sendImageDetectionFeedbackAPI(feedbackRequestModel: ImageDetectionFeedbackRequestModel?) {
        sendFeedbackDataListenerImpl = SendFeedbackDataListenerImpl(NetworkModule(context))
        CustomSnackProgressDialog.getSnackBarDialog(context, "Please wait ....")?.show()
        sendFeedbackDataListenerImpl?.sendImageDetectionFeedbackData(context, feedbackRequestModel, object : BaseInteractor.IResponseListener<SensorResponse> {
            override fun onProgress() {
                CustomSnackProgressDialog.getSnackBarDialog(
                    context, "Please wait ...."
                )?.show()
            }

            override fun onSuccess(resp: SensorResponse) {
                CustomSnackProgressDialog.getSnackBarDialog(context)?.dismiss()
                Toast.makeText(context, "Response Submitted", Toast.LENGTH_LONG).show()

            }

            override fun onFailure(t: Throwable) {
                CustomSnackProgressDialog.getSnackBarDialog(context)?.dismiss()
                Toast.makeText(context, "Response Failure", Toast.LENGTH_LONG).show()

            }
        })
    }


    private fun printLog(logName: String) {
        Logger.printLogE(tag = "ImageDetector", msg = logName)
    }

}