package com.test.capturelibrary.activity

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import com.test.capturelibrary.CaptureConstants
import com.example.capturelibrary.R
import com.otaliastudios.cameraview.*
import com.otaliastudios.cameraview.controls.Audio
import com.otaliastudios.cameraview.controls.Facing
import com.otaliastudios.cameraview.controls.Flash
import com.otaliastudios.cameraview.controls.Mode
import java.io.File
import java.io.FileOutputStream


class CaptureActivity : AppCompatActivity() {

    companion object {
        var TAG = "capture-library/${CaptureActivity::class.simpleName}"
    }

    // CameraView cameraView;
    private lateinit var cameraView: CameraView
    private lateinit var captureButton: View
    private lateinit var cameraTorchSwitch: SwitchCompat
    private lateinit var cameraFullViewSwitch: TextView
    private lateinit var cameraHorizontalViewSwitch: TextView
    private lateinit var cameraVerticalViewSwitch: TextView
    private lateinit var cameraViewSwitch: ImageView
    private lateinit var fileImagePath: String
    private lateinit var cameraOptions: CameraOptions
    private var cameraModeType = 1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fileImagePath = intent.extras?.getString(CaptureConstants.FILE_IMAGE_PATH).toString()
        cameraModeType = intent.extras?.getInt(CaptureConstants.CAMERA_MODE_TYPE)?:1
        doCameraVerification()

        setContentView(R.layout.activity_capture)
        initView()

        captureButton.setOnClickListener {
            cameraView.takePicture()
        }

        cameraView.setLifecycleOwner(this)
        cameraView.mode = Mode.PICTURE
        cameraView.useDeviceOrientation = false
        cameraView.audio = Audio.OFF

        cameraView.addCameraListener(object : CameraListener() {
            override fun onPictureTaken(@NonNull result: PictureResult) {
                result.toBitmap(3000, 3000) { bitmap ->
                    if (bitmap == null) {
                        //LOG ERROR
                        return@toBitmap
                    }
                    val bitmapScaled: Bitmap = getScaledBitmap(bitmap, result.rotation)
                    onClickPhoto(bitmapScaled)
                }
            }

            override fun onCameraError(exception: CameraException) {
                super.onCameraError(exception)
            }

            override fun onCameraOpened(options: CameraOptions) {
                super.onCameraOpened(options)
                cameraOptions = options
                setCameraToolsVisibility()
            }
        })


        CameraLogger.registerLogger { level, tag, message, throwable ->
            run {
            }
        }
    }

    private fun doCameraVerification() {
        if (!CameraUtils.hasCameras(this)) {
            Toast.makeText(
                this,
                getString(R.string.camera_not_supported_error_msg),
                Toast.LENGTH_SHORT
            ).show()
            sendImagePath(false, fileImagePath)
        }
    }

    private fun initView() {
        captureButton = findViewById(R.id.capture)
        cameraView = findViewById(R.id.cameraView)
        cameraTorchSwitch = findViewById(R.id.torchSwitch)
        cameraViewSwitch = findViewById(R.id.switchCamera)
        cameraFullViewSwitch = findViewById(R.id.cameraFullViewSwitch)
        cameraHorizontalViewSwitch = findViewById(R.id.cameraHorizontalViewSwitch)
        cameraVerticalViewSwitch = findViewById(R.id.cameraVerticalViewSwitch)

        cameraTorchSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            run {
                cameraView.flash = if (isChecked) Flash.TORCH else Flash.OFF
                setCameraToolsVisibility()
            }
        }
        cameraViewSwitch.setOnClickListener {
            cameraView.facing = if (cameraView.facing == Facing.BACK) Facing.FRONT else Facing.BACK
            setCameraToolsVisibility()
        }

        cameraFullViewSwitch.setOnClickListener {
            cameraModeType = 2
            setCameraViewSize()
            setCameraViewSelectedTextColor()
        }

        cameraHorizontalViewSwitch.setOnClickListener {
            cameraModeType = 1
            setCameraViewSize()
            setCameraViewSelectedTextColor()
        }

        cameraVerticalViewSwitch.setOnClickListener {
            cameraModeType = 3
            setCameraViewSize()
            setCameraViewSelectedTextColor()
        }

        setCameraViewSize()
        setCameraViewSelectedTextColor()
    }

    private fun setCameraViewSelectedTextColor() {
        cameraFullViewSwitch.setTextColor(ContextCompat.getColor(this, if(cameraModeType == 2) R.color.selected_camera_view_color else R.color.size_text_color))
        cameraHorizontalViewSwitch.setTextColor(ContextCompat.getColor(this, if(cameraModeType == 1) R.color.selected_camera_view_color else R.color.size_text_color))
        cameraVerticalViewSwitch.setTextColor(ContextCompat.getColor(this, if(cameraModeType == 3) R.color.selected_camera_view_color else R.color.size_text_color))
    }

    private fun setCameraViewSize() {
        windowManager.defaultDisplay.getMetrics(DisplayMetrics())
        val localLayoutParams3: RelativeLayout.LayoutParams =
            RelativeLayout.LayoutParams(getWidth(), getHeight())
//        localLayoutParams3.setMargins(
//            getLefMargin(),
//            getTopMargin(),
//            getLefMargin(),
//            getTopMargin()
//        )
        localLayoutParams3.addRule(RelativeLayout.CENTER_IN_PARENT)
        cameraView.layoutParams = localLayoutParams3
        val cameraFrameView: View = findViewById(R.id.camera_frame)
        cameraFrameView.layoutParams = cameraView.layoutParams
        cameraFrameView.layoutParams.height = cameraView.layoutParams.height
        cameraFrameView.layoutParams.width = cameraView.layoutParams.width
    }

    private fun setCameraToolsVisibility() {
        cameraTorchSwitch.visibility =
            if (cameraOptions.supports(Flash.TORCH) && cameraOptions.supports(Flash.OFF))
                View.VISIBLE
            else
                View.GONE

        cameraViewSwitch.visibility =
            if (cameraOptions.supports(Facing.FRONT) && cameraOptions.supports(Facing.BACK))
                View.VISIBLE
            else
                View.GONE
    }

    private fun onClickPhoto(bitmapPhoto: Bitmap) {
        if (isFinishing) {
            return
        }
        processImage(bitmapPhoto)
    }

    private fun processImage(bitmap: Bitmap) {
        try {
            val out = FileOutputStream(fileImagePath)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            out.flush()
            out.close()
            sendImagePath(true, fileImagePath)
        } catch (e: Exception) {
            sendImagePath(false, fileImagePath)
        }
    }

//    fun cropBitmap(bitmap: Bitmap): Bitmap? {
//        val factor = 0.6f
//        return Bitmap.createBitmap(
//            bitmap,
//            (bitmap.width * factor).toInt(), 0, (bitmap.width * (1 - factor)).toInt(), bitmap.height
//        )
//    }

    private fun sendImagePath(isSuccess: Boolean, imagePath: String?) {
        val returnIntent = Intent()
        returnIntent.putExtra("result", imagePath)
        returnIntent.putExtra(CaptureConstants.CAMERA_MODE_TYPE, cameraModeType)
        setResult(if (isSuccess) Activity.RESULT_OK else Activity.RESULT_CANCELED, returnIntent)
        finish()
    }

    fun getScaledBitmap(bitmapPhoto: Bitmap, degree: Int): Bitmap {
        val matrix = Matrix()
        matrix.postRotate((-1 * degree).toFloat())
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        return Bitmap.createBitmap(
            bitmapPhoto,
            0,
            getStartPoint(bitmapPhoto),
            bitmapPhoto.width,
            getBitmapHeight(bitmapPhoto)
        )
    }

//    fun resizeBitmap(bitmap: Bitmap, newHeight: Float): Bitmap? {
//        if (bitmap.height == newHeight.toInt()) return bitmap
//        val h = bitmap.height.toFloat()
//        val w = bitmap.width.toFloat()
//        val newWidth = newHeight / (h / w)
//        return Bitmap.createScaledBitmap(bitmap, newWidth.toInt(), newHeight.toInt(), true)
//    }

    private fun getWidth(): Int {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val aspectRatio: Float = getAspectRatio()
        return (if (displayMetrics.widthPixels * aspectRatio > displayMetrics.heightPixels) (displayMetrics.widthPixels / aspectRatio).toInt() else (displayMetrics.widthPixels * .95 - 4).toInt())
    }


    private fun getHeight(): Int {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val aspectRatio: Float = getAspectRatio()
        return if (getWidth() * aspectRatio < displayMetrics.heightPixels) (getWidth() * aspectRatio).toInt() else (displayMetrics.heightPixels)
    }

    private fun getBitmapHeight(bitmap: Bitmap): Int {
        val aspectRatio: Float = getAspectRatio()
        return if (bitmap.width * aspectRatio < bitmap.height) (bitmap.width * aspectRatio).toInt() else bitmap.height
    }

    private fun getLefMargin(): Int {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        return if (getWidth() > getHeight()) (displayMetrics.widthPixels * .025).toInt() else 0
    }

    private fun getTopMargin(): Int {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        return if (getWidth() < getHeight()) (displayMetrics.heightPixels * .025).toInt() else 0
    }

    private fun getStartPoint(bitmap: Bitmap): Int {
        return if ((bitmap.height - getBitmapHeight(bitmap)) / 2 > 0) (bitmap.height - getBitmapHeight(
            bitmap
        )) / 2 else 0
    }

//    private fun getStartPointRotate(bitmap: Bitmap): Int {
//        return if ((bitmap.width - getBitmapWidth(bitmap)) / 2 > 0) (bitmap.width - getBitmapWidth(
//            bitmap
//        )) / 2 else 0
//    }

//    private fun getBitmapWidth(bitmap: Bitmap): Int {
//        val aspectRatio = getAspectRatio()
//        return (if (bitmap.height * aspectRatio < bitmap.width) (bitmap.height * aspectRatio).toInt() else bitmap.width)
//    }

    private fun getAspectRatio(): Float {
        return when (cameraModeType) {
            1 -> 0.65f //nic
            2 -> 1.7f
            3 -> 1.4f
            else -> 1.7f
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults[0] != PackageManager.PERMISSION_GRANTED &&
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !shouldShowRequestPermissionRationale(
                permissions[0]
            ))
        ) {
            Toast.makeText(
                this,
                getString(R.string.camera_permission_not_granted),
                Toast.LENGTH_LONG
            ).show()
            sendImagePath(false, fileImagePath)
        }
    }
}