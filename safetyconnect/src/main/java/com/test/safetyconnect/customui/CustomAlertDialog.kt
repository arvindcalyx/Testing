package com.test.safetyconnect.customui

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.test.safetyconnect.R

/**
 * Created on 01/01/23.
 */
object CustomAlertDialog {
    private var alertDialog: AlertDialog? = null

    fun showFailureDialog(
        message: String,
        context: Context,
        myCallback: ((status: AlertDialogEnum?) -> Unit?)? = null
    ) {
        if (alertDialog != null && alertDialog?.isShowing!!) {
            alertDialog?.setMessage(message)
            return
        }

        alertDialog = AlertDialog.Builder(context)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton(context.resources.getString(R.string.ok)) { dialog, _ ->
                myCallback?.invoke(AlertDialogEnum.SUCCESS)
                dialog.dismiss()
            }
            .show()
    }

    enum class AlertDialogEnum {
        SUCCESS, FAILURE
    }
}