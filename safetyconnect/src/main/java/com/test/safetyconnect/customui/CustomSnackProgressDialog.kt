package com.test.safetyconnect.customui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import com.test.safetyconnect.R
import com.google.android.material.snackbar.Snackbar

/**
 * Created on 01/01/23.
 */
object CustomSnackProgressDialog {
    @SuppressLint("StaticFieldLeak")
    private var snackbar: Snackbar? = null
    private var showText: String = "Loading please wait... "

    @SuppressLint("ShowToast")
    fun getSnackBarDialog(context: Context?, showText_: String? = null): Snackbar? {
        showText_?.let { msg -> showText = msg }
        if (snackbar != null && snackbar?.isShown!!) {
            return snackbar!!
        }
        snackbar = Snackbar.make(
            (context as Activity).findViewById(R.id.rootView),
            showText,
            Snackbar.LENGTH_INDEFINITE
        )
        val viewGroup =
            snackbar?.view?.findViewById<View>(com.google.android.material.R.id.snackbar_text)?.parent as ViewGroup
        viewGroup.addView(ProgressBar(context))
        return snackbar!!
    }
}