package com.test.safetyconnect.main

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.util.Base64
import android.util.DisplayMetrics
import android.util.Log
import java.io.*
import java.text.SimpleDateFormat
import java.util.*


object FileUtils_APP {
    const val RETSL_FOLDER = ".RETSL"

    private const val TAG = "retsl-facelib/FileUtils"
    const val maxLogSize = 2000
    //const val SCANNED_IMAGE: String = "scannedImage"
    const val FILE_FORMAT_KEY: String = "isFileFormatRequired"
    var KEY_VALUE = ""


//    fun convertPixelsToDp(px: Float, context: Context): Float {
//        return px / (context.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
//    }

    fun convertDpToPixel(dp: Float, context: Context): Float {
        return dp * (context.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
    }

//    fun getProgressData(): ProgressBarData {
//        return ProgressBarData.ProgressBarBuilder()
//            .setCancelable(true)
//            .setProgressMessage("Processing")
//            .setProgressMessageColor(Color.parseColor("#4A4A4A"))
//            .setBackgroundViewColor(Color.parseColor("#FFFFFF"))
//            .setProgressbarTintColor(Color.parseColor("#FAC42A")).build()
//    }

    // method to store image to sdcard
    fun storeImage(file: File, bmp: Bitmap?) {
        try {
            val out = FileOutputStream(file)
            bmp?.compress(Bitmap.CompressFormat.JPEG, 80, out)
            out.flush()
            out.close()

        } catch (e: Exception) {
        }
    }

    fun showAlert(message: String, context: Activity?, onRetake: () -> Unit, onCancel: () -> Unit) {
        if (context != null && !context.isFinishing && !context.isDestroyed) {
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Alert")
            builder.setMessage(message)
            builder.setPositiveButton("Retake") { dialog, _ ->
                onRetake()
                dialog.dismiss()
            }

            builder.setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                onCancel()
            }

            val dialog: AlertDialog = builder.create()
            dialog.show()
        }
    }

    fun bitmapToBase64(bitmap: Bitmap?): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap?.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    fun getXoffset(width: Int): Float {
        return width / 21f
    }

    fun getYoffset(height: Int): Float {
        return height / 19f
    }

    @Synchronized
    fun log(string: String) {
        for (i in 0..string.length / maxLogSize) {
            val start = i * maxLogSize
            var end = (i + 1) * maxLogSize
            end = if (end > string.length) string.length else end
            Log.d(Throwable().stackTrace[1].className, string.substring(start, end))
        }
    }

    fun getImageInAssets(context: Context, imageName: String): String {
        val encodedImageBase64 = ""
        val assetManager = context.assets
        var fileStream: InputStream? = null
        try {
            fileStream = assetManager.open(imageName)
            if (fileStream != null) {
                //                  BitmapFactory.Options bfo = new BitmapFactory.Options();
                //                  bfo.inPreferredConfig = Bitmap.Config.ARGB_8888;
                //                  Bitmap bitmap = BitmapFactory.decodeStream(fileStream, null, bfo);

                val bitmap = BitmapFactory.decodeStream(fileStream)
                // Convert bitmap to Base64 encoded image for web
                val byteArrayOutputStream = ByteArrayOutputStream()

                // to get image extension file name split the received
                val fileExtensionPosition = imageName.lastIndexOf('.')
                val fileExtension = imageName.substring(fileExtensionPosition + 1)
                //                  Log.d(IConstants.TAG,"fileExtension: " + fileExtension);

                if (fileExtension.equals("png", ignoreCase = true)) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
                    //                      Log.d(IConstants.TAG,"fileExtension is PNG");
                } else if (fileExtension.equals(
                        "jpg",
                        ignoreCase = true
                    ) || fileExtension.equals("jpeg", ignoreCase = true)
                ) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
                    //                      Log.d(TAG,"fileExtension is JPG");
                }

                val byteArray = byteArrayOutputStream.toByteArray()

                return Base64.encodeToString(byteArray, Base64.DEFAULT)
            }
        } catch (e: IOException) {
            return ""
        } finally {
            //Always clear and close
            try {
                if (fileStream != null) {
                    fileStream.close()
                }
            } catch (e: IOException) {
            }

        }
        return encodedImageBase64
    }


    fun createImageFromBitmap(
        context: Context?,
        bitmap: Bitmap?,
        stringExtra: Uri
    ): Uri? {

        var uri = stringExtra
        try {

            val file = context?.let { getExtStorageDirFileForImages("", context) } ?: return null
            val bytes = ByteArrayOutputStream()
            bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
            val bitmapData = bytes.toByteArray()
            val fos = FileOutputStream(file)
            fos.write(bitmapData)
            fos.flush()
            fos.close()

            uri = Uri.fromFile(file)

        } catch (e: Exception) {
        }
        return uri
    }

    fun getExtStorageDirFileForImages(type : String, context : Context) : File? {
        val timeStamp =
            SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.getDefault()).format(Date())
        val fileName =
            "IMG_" + (if (type.isBlank()) "" else type + "_") + timeStamp + ".jpg"
        if(isExternalStorageWritable()) {
            try {
                val path =
                    context.getExternalFilesDir(RETSL_FOLDER)
                        ?: throw Exception("getExtStorageDirFileForImages shared storage not available")
                val file = File(path, fileName)
                return file
            } catch (ex: Exception) {
            }
        }
        return getIntStorageDirFileForImages(context, fileName)
    }

    private fun getIntStorageDirFileForImages(context: Context, filename: String): File? {
        try {
            val dir = File(context.filesDir, RETSL_FOLDER)
            if (!dir.exists()) {
                dir.mkdir()
            }
            return File(dir, filename)
        } catch (ex: Exception) {
        }
        return null
    }

    private fun isExternalStorageWritable(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }

    fun getExternalDirOrFile(context : Context, fileName: String, directoryType: DirectoryType) : File? {
        try {
            val type: String?
            when(directoryType) {
                DirectoryType.LOG -> type = "log"
                else -> type = null
            }
            val path =
                context.getExternalFilesDir(type) ?: throw Exception("Shared storage not available")

            val file = File(path, fileName)
            return file
        } catch (ex : Exception) {
        }
        return null
    }


    private fun getBase64(input: String): String? {
        return Base64.encodeToString(input.toByteArray(), Base64.NO_WRAP)
    }

    fun getImageAuthToken(userToken: String): String? {
        val keyNum = Date(Date().time + 60000*30).time / 1000
        val token = userToken + "@" + keyNum.toInt()
        val encodedToken = getBase64(token)
        return encodedToken
    }

    fun getImageAuthToken(): String? {
        val keyNum = Date(Date().time + 60000*30).time / 1000
        val token = KEY_VALUE + keyNum.toInt()
        return getBase64(token)
    }

    enum class DirectoryType {
        LOG, ZIP, IMAGE
    }

}