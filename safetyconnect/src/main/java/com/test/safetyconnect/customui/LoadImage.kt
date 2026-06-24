package com.test.safetyconnect.customui

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.TextView
import androidx.core.view.isVisible
import com.test.safetyconnect.R

/**
 * Created on 01/01/23.
 */
@SuppressLint("StaticFieldLeak")
object LoadImage {
    var mDialog: Dialog? = null
    var time_counter: TextView? = null

    @SuppressLint("InflateParams", "StaticFieldLeak")
    fun getBannerImage(context: Context, callback: ((Boolean) -> Unit)?) {
        try {
            if (mDialog != null && mDialog?.isShowing!!) {
                return
            }

            mDialog = Dialog(context)
            mDialog?.setCancelable(false)
            mDialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
            mDialog?.setContentView(
                LayoutInflater.from(context).inflate(R.layout.popup_banner_new, null)
            )
            mDialog?.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            mDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)

            time_counter = mDialog?.findViewById<View>(R.id.time_counter) as TextView
            val success_btn = mDialog?.findViewById<View>(R.id.success_btn) as Button
            success_btn.setOnClickListener {
                callback?.invoke(true)
                mDialog?.dismiss()
            }


            val failure_btn = mDialog?.findViewById<View>(R.id.failure_btn) as Button
            failure_btn.setOnClickListener {
                callback?.invoke(false)
                mDialog?.dismiss()
            }



            mDialog?.show()
        } catch (ignore: Exception) {
        }
    }


    fun getTimerView(): TextView? {
        if (mDialog != null && mDialog?.isShowing!!) {
            if (time_counter != null && time_counter?.isVisible!!) {
                return time_counter
            }
        }
        return null
    }

    fun dismissDialog(){
        if (mDialog != null && mDialog?.isShowing!!) {
            mDialog?.dismiss()
        }
    }


}