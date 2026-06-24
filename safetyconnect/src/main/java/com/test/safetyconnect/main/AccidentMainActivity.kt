package com.test.safetyconnect.main

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.test.safetyconnect.main.Utils.getOutputMediaFileUriForCamera
import com.test.capturelibrary.CaptureConstants
import com.test.capturelibrary.activity.CaptureActivity
import com.test.safetyconnect.R
import com.test.safetyconnect.customui.CustomFeedbackAlertDialog
import com.test.safetyconnect.model.UploadImageResponse
import com.test.safetyconnect.sdkinit.ImageDetector
import com.test.safetyconnect.sdkinit.SafetyConnect
import com.bumptech.glide.Glide
import com.test.safetyconnect.databinding.MainActivityBinding
import java.io.File

class AccidentMainActivity : AppCompatActivity() {

    private lateinit var binding: MainActivityBinding
    private var poiFrontImageUri: Uri? = null
    private val URI_RESULT = "result"
    private var safetyConnect: SafetyConnect? = null
    private var isCheckingForBikeHelmetOnly: Boolean = false
    private var detector: ImageDetector? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.saveImage.setOnClickListener {

            /**
             * Upload Image URI data over Backend and get Response Helmet/Ladder/Safety Harness/Worker
             * */
            if (isCheckingForBikeHelmetOnly) {
                detector?.uploadImageV2(poiFrontImageUri) { uploadImageResponse ->
                    //show pop up and take confirmation and call new API for feedback loop
                    uploadImageResponse?.let {
                        showFeedbackLoopForBikeHelmetOnly(uploadImageResponse)
                    } ?: kotlin.run {
                        Toast.makeText(this, R.string.something_went_wrong, Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                detector?.uploadImage(poiFrontImageUri) { uploadImageResponse ->
                    //show pop up and take confirmation and call new API for feedback loop
                    uploadImageResponse?.let {
                        showFeedbackLoopForSafetyParams(uploadImageResponse)
                    } ?: kotlin.run {
                        Toast.makeText(this, R.string.something_went_wrong, Toast.LENGTH_LONG).show()
                    }
                }

            }
        }


        binding.clickCaptureNormal.setOnClickListener {
            isCheckingForBikeHelmetOnly = false
            openImagePicker()
        }

        binding.clickCaptureBikeHelmet.setOnClickListener {
            isCheckingForBikeHelmetOnly = true
            openImagePicker()
        }
    }

    private fun showFeedbackLoopForBikeHelmetOnly(uploadImageResponse: UploadImageResponse) {
        uploadImageResponse.equipment?.let {
            val dialog = CustomFeedbackAlertDialog(this, true, uploadImageResponse.requestId, uploadImageResponse) {
                if (it != null) {
                    detector?.sendImageDetectionFeedbackAPI(it)
                } else {
                    Toast.makeText(this, "please complete the correctness of data", Toast.LENGTH_SHORT).show()
                }
            }
            dialog.show()
        }
    }

    private fun showFeedbackLoopForSafetyParams(uploadImageResponse: UploadImageResponse) {
        val dialog = CustomFeedbackAlertDialog(this, false, uploadImageResponse.requestId, uploadImageResponse) {
            if (it != null) {
                detector?.sendImageDetectionFeedbackAPI(it)
            } else {
                Toast.makeText(this, "please complete the correctness of data", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
    }


    private fun openImagePicker() {
        val imagePathUri = getOutputMediaFileUriForCamera("", this)
        imagePathUri?.let {
            val intent = Intent(this@AccidentMainActivity, CaptureActivity::class.java)
            intent.putExtra(CaptureConstants.FILE_IMAGE_PATH, imagePathUri.path)
            intent.putExtra(CaptureConstants.CAMERA_MODE_TYPE, 2)
            startActivityForResult(intent, CaptureConstants.CAMERA_CAPTURE)
        } ?: run {
            Toast.makeText(this, R.string.open_camera_problem_msg, Toast.LENGTH_LONG).show()
        }

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {

                1001 -> {
                    poiFrontImageUri = Uri.fromFile(File(data?.getStringExtra(URI_RESULT)))

                    Glide.with(this@AccidentMainActivity).load(File(poiFrontImageUri?.path)).fitCenter().into(binding.showFrontImage)

                    binding.showFrontImage.visibility = View.VISIBLE
                    binding.saveImage.visibility = View.VISIBLE
                }

            }

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        /**
         * Un-Bind Application With Android Life Cycle
         * */
        safetyConnect?.getAccidentDetectorInstance()?.unBindSensor()

    }


}