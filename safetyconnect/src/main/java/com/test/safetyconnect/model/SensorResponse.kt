package com.test.safetyconnect.model

import com.google.gson.annotations.SerializedName

/**
 * Created on 01/01/23.
 */
class SensorResponse(

    @field:SerializedName("batch")
    val batch: String?=null,

    @field:SerializedName("crash")
    val crash: String?=null,

    @field:SerializedName("message")
    val message: String?=null,

    @field:SerializedName("status")
    val status: Int = 0

){
    override fun toString(): String {
        return super.toString()
    }
}


