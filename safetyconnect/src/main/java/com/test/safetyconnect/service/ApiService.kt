package com.test.safetyconnect.service

import com.test.safetyconnect.model.FeadbackRequestModel
import com.test.safetyconnect.model.ImageDetectionFeedbackRequestModel
import com.test.safetyconnect.model.SensorDataModel
import com.test.safetyconnect.model.SensorResponse
import com.test.safetyconnect.model.UploadImageResponse
import io.reactivex.Single
import okhttp3.MultipartBody
import retrofit2.http.*


/**
 * @author XD
 */
interface ApiService {

    @POST("data")
    fun updateSensorData(@Body data: SensorDataModel): Single<SensorResponse>

    @POST("label")
    fun updateFeadbackSensorData(@Body data: FeadbackRequestModel? = null): Single<SensorResponse>

    @POST("label_image")
    fun updateImageDetectionFeedbackData(@Body data: ImageDetectionFeedbackRequestModel? = null): Single<SensorResponse>

    @Multipart
    @POST("image_check")
    fun uploadImage(@Part depositReceiptImage: MultipartBody.Part? = null): Single<UploadImageResponse>

    @Multipart
    @POST("check_2W")
    fun uploadImageForBikeHelmet(@Part depositReceiptImage: MultipartBody.Part? = null): Single<UploadImageResponse>
// second option
}