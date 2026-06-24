package com.test.capturelibrary

import android.app.Activity
import android.content.Intent
import android.net.Uri
import com.test.capturelibrary.activity.CaptureActivity

class CaptureSdk {

    companion object {
        @JvmStatic
        fun captureDocument(activity: Activity, uri: Uri, cameraModeType: Int) {
            val intent = Intent(activity, CaptureActivity::class.java)
            intent.putExtra(CaptureConstants.FILE_IMAGE_PATH, uri.path)
            intent.putExtra(CaptureConstants.CAMERA_MODE_TYPE, cameraModeType)
            activity.startActivityForResult(intent, CaptureConstants.CAMERA_CAPTURE)
        }
    }
}