package com.test.safetyconnect.model

import com.google.gson.annotations.SerializedName

/**
 * Created on 01/01/23.
 */
class UploadImageResponse(

    @field:SerializedName("equipment")
    val equipment: UploadEquipment? = null,

    @field:SerializedName("message")
    val message: String? = null,

    @field:SerializedName("status")
    val status: Int? = 0,

    @SerializedName("request_id")
    val requestId: String? = null
)

class UploadEquipment(

    @field:SerializedName("Ladder")
    val Ladder: Boolean,

    @field:SerializedName("Helmet")
    val Helmet: Boolean,

    @field:SerializedName("Safety_Harness")
    val Safety_Harness: Boolean,

    @field:SerializedName("Safety_Jacket")
    val Safety_Jacket: Boolean,

    @field:SerializedName("Worker")
    val Worker: Boolean,

    @field:SerializedName("Bike_Helmet")
    val Bike_Helmet: Boolean

)


