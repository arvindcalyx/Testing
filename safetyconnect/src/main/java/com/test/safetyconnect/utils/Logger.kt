package com.test.safetyconnect.utils

import android.util.Log

/**
 * Created on 01/01/23.
 */
object Logger {
    var TAG: String = "Logger"

   public fun printLogE(tag:String?=null,msg:String?=null) {
       if(tag!=null && msg!=null){
           Log.e(tag,msg)
       }else if(tag==null && msg!=null){
           Log.e(TAG,msg)
       }
    }

}