package com.test.safetyconnect.utils

import android.Manifest
import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.text.*
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.channels.FileChannel
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.graphics.scale

/**
 * Basic Utilities for the App.
 *
 */


object Utils {

    private val className = "Utils"

    // Lengths
    private const val SIM_LENGTH = 20
    private const val IMSI_LENGTH = 15
    private const val SIM_LENGTH_TYPE_1 = 16
    private const val SIM_LENGTH_TYPE_2 = 5
    private const val SIM_LENGTH_TYPE_3 = 20
    private const val MPIN_LENGTH = 4

    // Regex
    private const val NIC_OLD_REG = "^\\d{9}[xvXV]\$"
    private const val NIC_NEW_REG = "^\\d{12}\$"
    private const val MOB_REG = "^[7][5]\\d{7}\$"

    private const val EMAIL_REG = "^[a-zA-Z0-9._-]+@[a-zA-Z0-9-]+(\\.[a-zA-Z]{2,4}){1,2}$"

    private const val SIM_PATTERN = "^\\d{19}[f]\$"
    private const val SIM_PATTERN_2 = "^\\d{5}\$"
    private const val PIN_VALIDATION =
        "^(?!.*?(?:0(?:12|98)|123|234|3(?:45|21)|4(?:56|32)|5(?:67|43)|6(?:78|54)|7(?:89|65)|876|987))(?!.*?(.)\\1{2})[0-9]{4}"

    fun isValidNic(number: String): Boolean {
        return number.isNotBlank()
                && (number.matches(NIC_OLD_REG.toRegex()) || number.matches(NIC_NEW_REG.toRegex()))
    }

    /**
     * Validate Mobile with First Digit in Range of [6 - 9] and of Range 10
     *
     * @param phone
     * @return valid or not
     */
    fun isValidMobile(phone: String): Boolean {
        return phone.length == 9
    }

    fun isValidMobileAlternate(phone: String): Boolean {
        return phone.length == 9 || phone.length == 10
    }

    fun isValidMobileAlternateSImSwap(phone: String): Boolean {
        return phone.length == 0 || phone.length == 9 || phone.length == 10
    }

    fun isValidSimWithF(number: String): Boolean {
        return number.isNotBlank() && number.matches(SIM_PATTERN.toRegex()) && number.get(number.length - 1)
            .toString() == "f" || number.get(
            number.length - 1
        ).toString() == "F"
                || number.length == SIM_LENGTH_TYPE_2

    }

    fun isValidSimSize(number: String): Boolean {
        return number.isNotBlank() && (number.length == SIM_LENGTH_TYPE_2 || (number.length in SIM_LENGTH_TYPE_1..SIM_LENGTH_TYPE_3))
    }

    fun isValidSimSizeCYN(number: String): Boolean {
        return number.isNotBlank() && (number.length in SIM_LENGTH_TYPE_1..SIM_LENGTH_TYPE_3)
    }

    fun isValidImsi(number: String): Boolean {
        return number.isNotBlank() && number.length == IMSI_LENGTH
    }

    fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && email.matches(EMAIL_REG.toRegex())
    }

    fun isValidSimLast(number: String): Boolean {
        return number.isNotBlank() && number.matches(SIM_PATTERN_2.toRegex())
    }

    fun isValidMPin(mPin: String): Boolean {
        return mPin.isNotBlank() && mPin.length == MPIN_LENGTH && mPin.matches(PIN_VALIDATION.toRegex())
    }


    fun getProgressDialog(context: Context, message: String): AlertDialog {
        val llPadding = 30
        val ll = LinearLayout(context)
        ll.orientation = LinearLayout.HORIZONTAL
        ll.setPadding(llPadding, llPadding, llPadding, llPadding)
        ll.gravity = Gravity.CENTER
        var llParam = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        llParam.gravity = Gravity.CENTER
        ll.layoutParams = llParam

        val progressBar = ProgressBar(context)
        progressBar.isIndeterminate = true
        progressBar.setPadding(0, 0, llPadding, 0)
        progressBar.layoutParams = llParam

        llParam = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        llParam.gravity = Gravity.CENTER
        val tvText = TextView(context)
        tvText.text = message
        tvText.setTextColor(Color.parseColor("#000000"))
        tvText.textSize = 20.toFloat()
        tvText.layoutParams = llParam

        ll.addView(progressBar)
        ll.addView(tvText)

        val builder = AlertDialog.Builder(context)
        builder.setCancelable(true)
        builder.setView(ll)

        val dialog = builder.create()
        dialog.setCancelable(false)
        val window = dialog.window
        if (window != null) {
            val layoutParams = WindowManager.LayoutParams()
            layoutParams.copyFrom(dialog.window?.attributes)
            layoutParams.width = LinearLayout.LayoutParams.WRAP_CONTENT
            layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
            dialog.window?.attributes = layoutParams
        }
        return dialog
    }


    /**
     * TODO: To Optimize the code and make it more Generic
     *
     * Check Camera permission is provided or not.
     *
     * @param context
     * @return
     */
    @JvmStatic
    fun isPermissionsGranted(context: Activity): Boolean {
        return !(Build.VERSION.SDK_INT > 22 && ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) != PackageManager.PERMISSION_GRANTED)
    }

    /**
     * Create a file Uri for saving an image or video
     */
    @JvmStatic
    fun getOutputMediaFileUri(
        type: String, context: Context
    ): Uri {
        return Uri.fromFile(FileUtils.getExtStorageDirFileForImages(type, context))
    }


    fun changeUri(path: String?, context: Context): Uri {
        val despath = getOutputMediaFileUri("", context).path

        copyFileOrDirectory(path, despath)

        return Uri.parse(despath)
    }

    fun copyFileOrDirectory(srcDir: String?, dstDir: String?) {
        try {
            val src = File(srcDir)
            val dst = File(dstDir)
            if (src.isDirectory) {
                val files: Array<String> = src.list()
                val filesLength = files.size
                for (i in 0 until filesLength) {
                    val src1: String = File(src, files[i]).path
                    val dst1: String = dst.path
                    copyFileOrDirectory(src1, dst1)
                }
            } else {
                copyFile(src, dst)
            }
        } catch (e: Exception) {
        }
    }

    @Throws(IOException::class)
    fun copyFile(sourceFile: File?, destFile: File) {
        if (!destFile.parentFile.exists()) destFile.parentFile.mkdirs()
        if (!destFile.exists()) {
            destFile.createNewFile()
        }
        var source: FileChannel? = null
        var destination: FileChannel? = null
        try {
            source = FileInputStream(sourceFile).channel
            destination = FileOutputStream(destFile).channel
            destination.transferFrom(source, 0, source.size())
        } finally {
            if (source != null) {
                source.close()
            }
            if (destination != null) {
                destination.close()
            }
        }
    }


    /**
     * Calculates InSampleSize for Bitmaps
     *
     * @param options
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        // Raw height and width of image
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1


        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            // Calculate the largest inSampleSize value that is a power of 2 and keep both
            // height and width larger than the required height and width.
            while (halfHeight / inSampleSize > reqHeight && halfWidth / inSampleSize > reqWidth) {
                inSampleSize *= 2
            }
        }


        return inSampleSize
    }

    /**
     * Creates Bitmap of required size from a resource.
     *
     * @param uri
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    fun decodeSampleBitmapFromResource(
        res: Resources,
        resId: Int,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap {
        // First decode with inJustDecodeBounds=true to check dimensions
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeResource(res, resId, options)

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeResource(res, resId, options)
    }

    /**
     * Creates Bitmap of required size from a Uri.
     *
     * @param uri
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    fun decodeSampleBitmapFromUri(uri: Uri, reqWidth: Int, reqHeight: Int): Bitmap {
        // First decode with inJustDecodeBounds=true to check dimensions
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(uri.path, options)

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeFile(uri.path, options)
    }

    fun getUri(resources: Resources, resourceId: Int): Uri {
        return Uri.parse(
            ContentResolver.SCHEME_ANDROID_RESOURCE +
                    "://" + resources.getResourcePackageName(resourceId) +
                    '/' + resources.getResourceTypeName(resourceId) +
                    '/' + resources.getResourceEntryName(resourceId)
        )
    }

    private const val BITMAP_SCALE = 0.4f
    private const val BLUR_RADIUS = 8

    @JvmStatic
    fun fastBlur(sentBitmap: Bitmap): Bitmap? {
        var sentBitmap = sentBitmap
        val scale = BITMAP_SCALE
        val radius = BLUR_RADIUS
        val width = Math.round(sentBitmap.width * scale)
        val height = Math.round(sentBitmap.height * scale)
        sentBitmap = sentBitmap.scale(width, height, false)

        val bitmap = sentBitmap.copy(sentBitmap.config ?: Bitmap.Config.ARGB_8888, true)
        if (radius < 1) {
            return null
        }

        val w = bitmap.width
        val h = bitmap.height

        val pix = IntArray(w * h)
        Log.e("pix", w.toString() + " " + h + " " + pix.size)
        bitmap.getPixels(pix, 0, w, 0, 0, w, h)

        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val div = radius + radius + 1

        val r = IntArray(wh)
        val g = IntArray(wh)
        val b = IntArray(wh)
        var rsum: Int
        var gsum: Int
        var bsum: Int
        var x: Int
        var y: Int
        var i: Int
        var p: Int
        var yp: Int
        var yi: Int
        var yw: Int
        val vmin = IntArray(Math.max(w, h))

        var divsum = div + 1 shr 1
        divsum *= divsum
        val dv = IntArray(256 * divsum)
        i = 0
        while (i < 256 * divsum) {
            dv[i] = i / divsum
            i++
        }

        yi = 0
        yw = yi

        val stack = Array(div) { IntArray(3) }
        var stackpointer: Int
        var stackstart: Int
        var sir: IntArray
        var rbs: Int
        val r1 = radius + 1
        var routsum: Int
        var goutsum: Int
        var boutsum: Int
        var rinsum: Int
        var ginsum: Int
        var binsum: Int

        y = 0
        while (y < h) {
            bsum = 0
            gsum = bsum
            rsum = gsum
            boutsum = rsum
            goutsum = boutsum
            routsum = goutsum
            binsum = routsum
            ginsum = binsum
            rinsum = ginsum
            i = -radius
            while (i <= radius) {
                p = pix[yi + Math.min(wm, Math.max(i, 0))]
                sir = stack[i + radius]
                sir[0] = p and 0xff0000 shr 16
                sir[1] = p and 0x00ff00 shr 8
                sir[2] = p and 0x0000ff
                rbs = r1 - Math.abs(i)
                rsum += sir[0] * rbs
                gsum += sir[1] * rbs
                bsum += sir[2] * rbs
                if (i > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
                i++
            }
            stackpointer = radius

            x = 0
            while (x < w) {

                r[yi] = dv[rsum]
                g[yi] = dv[gsum]
                b[yi] = dv[bsum]

                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum

                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]

                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]

                if (y == 0) {
                    vmin[x] = Math.min(x + radius + 1, wm)
                }
                p = pix[yw + vmin[x]]

                sir[0] = p and 0xff0000 shr 16
                sir[1] = p and 0x00ff00 shr 8
                sir[2] = p and 0x0000ff

                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]

                rsum += rinsum
                gsum += ginsum
                bsum += binsum

                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer % div]

                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]

                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]

                yi++
                x++
            }
            yw += w
            y++
        }
        x = 0
        while (x < w) {
            bsum = 0
            gsum = bsum
            rsum = gsum
            boutsum = rsum
            goutsum = boutsum
            routsum = goutsum
            binsum = routsum
            ginsum = binsum
            rinsum = ginsum
            yp = -radius * w
            i = -radius
            while (i <= radius) {
                yi = Math.max(0, yp) + x

                sir = stack[i + radius]

                sir[0] = r[yi]
                sir[1] = g[yi]
                sir[2] = b[yi]

                rbs = r1 - Math.abs(i)

                rsum += r[yi] * rbs
                gsum += g[yi] * rbs
                bsum += b[yi] * rbs

                if (i > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }

                if (i < hm) {
                    yp += w
                }
                i++
            }
            yi = x
            stackpointer = radius
            y = 0
            while (y < h) {
                // Preserve alpha channel: ( 0xff000000 & pix[yi] )
                pix[yi] =
                    -0x1000000 and pix[yi] or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]

                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum

                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]

                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]

                if (x == 0) {
                    vmin[y] = Math.min(y + r1, hm) * w
                }
                p = x + vmin[y]

                sir[0] = r[p]
                sir[1] = g[p]
                sir[2] = b[p]

                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]

                rsum += rinsum
                gsum += ginsum
                bsum += binsum

                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer]

                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]

                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]

                yi += w
                y++
            }
            x++
        }

        Log.e("pix", w.toString() + " " + h + " " + pix.size)
        bitmap.setPixels(pix, 0, w, 0, 0, w, h)

        return bitmap

    }


    fun isValidIndex(arr: List<String>, index: Int): Boolean {
        return index >= 0 && index < arr.size
    }

    fun getDate(date: Date, days: Int): Date {
        val calnder: Calendar = Calendar.getInstance()
        calnder.time = date
        calnder.add(Calendar.DATE, days)

        return calnder.time
    }

    fun getDateInFormatDDMMM(day: String, noOfDays: Int): String {

        val format = SimpleDateFormat("dd MMM yyyy")
        var myDate: Date = format.parse(day)
        myDate = getDate(myDate, noOfDays)
        val dateFormat: DateFormat = SimpleDateFormat("dd MMM yyyy")
        return dateFormat.format(myDate)
    }

    fun getDateInFormat(day: String): String {

        val format = SimpleDateFormat("dd/MM/yyyy")
        var myDate: Date = format.parse(day)
        val dateFormat: DateFormat = SimpleDateFormat("dd MMM yyyy")
        return dateFormat.format(myDate)
    }

    fun getDateInFormat2(day: String): String {
        val format = SimpleDateFormat("dd/MM/yyyy")
        var myDate: Date = format.parse(day)
        val dateFormat: DateFormat = SimpleDateFormat("dd/MM/yyyy")
        return dateFormat.format(myDate)
    }

    fun getDateWithoutDelims(date: String): String {
        return try {
            val dateArray = date.split("/")
            val delimeterLessDate = dateArray[0] + dateArray[1] + dateArray[2]
            delimeterLessDate
        } catch (exception: Exception) {
            ""
        }
    }

    fun getTodayDate(): String {
        Locale.setDefault(Locale.ENGLISH)
        val date: Date = Calendar.getInstance().time
        val dateFormat: DateFormat = SimpleDateFormat("dd MMM yyyy")
        return dateFormat.format(date)

    }

    fun getTodayDatefromMills(mills: Long): String {

        val date: Date = Calendar.getInstance().time
        val dateFormat: DateFormat = SimpleDateFormat("dd MMM yyyy")
        return dateFormat.format(date)

    }

    fun getDate(date: String): Int {
        Locale.setDefault(Locale.ENGLISH)
        val format = SimpleDateFormat("dd MMM yyyy")
        var myDate: Date = format.parse(date)
        return myDate.date

    }

    fun getMonth(date: String): Int {
        Locale.setDefault(Locale.ENGLISH)
        val format = SimpleDateFormat("dd MMM yyyy")
        var myDate: Date = format.parse(date)
        return myDate.month

    }

    fun getYear(date: String): Int {
        Locale.setDefault(Locale.ENGLISH)
        val format = SimpleDateFormat("dd MMM yyyy")
        var myDate: Date = format.parse(date)
        return myDate.year + 1900

    }

    @JvmStatic
    fun getTimeFromTimeZone(string: String): Long {
        val isoFormat =
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
        isoFormat.timeZone = TimeZone.getTimeZone("UTC")
        val date = isoFormat.parse(string)
        return date.time
    }

    fun getMilliFromDate(date: String): Long {
        var selectedDate = date
        if (selectedDate.isEmpty())
            selectedDate = getTodayDate()
        val dateTimeFormatter = SimpleDateFormat("dd MMM yyyy")
        var myDate: Date = dateTimeFormatter.parse(selectedDate)
        return myDate.time
    }

    @JvmStatic
    fun getDatefromMills(dateInMills: Long): String {
        val formatter = SimpleDateFormat("dd:MM:yyyy HH:mm:ss")
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = dateInMills
        return formatter.format(calendar.time)
    }

    @JvmStatic
    fun getOnlyDatefromMills(dateInMills: Long): String {
        val formatter = SimpleDateFormat("dd-MM-yyyy")
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = dateInMills
        return formatter.format(calendar.time)
    }

    @JvmStatic
    fun getOnlyDatefromMills2(dateInMills: Long): String {
        val formatter = SimpleDateFormat("dd/MM/yyyy")
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = dateInMills
        return formatter.format(calendar.time)
    }

    @JvmStatic
    fun getTimefromMills(dateInMills: Long): String {
        val formatter = SimpleDateFormat("HH:mm:ss")
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = dateInMills
        return formatter.format(calendar.time)
    }


    fun toast(message: CharSequence, context: Context?) =
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()


    fun editextFixedValue(value: EditText) {
        value.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (!s.toString().startsWith("75")) {
                    value.setText("75")
                    Selection.setSelection(value.text, value.text.length)

                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }
        })
    }

    fun editextAgentFixedValue(value: EditText) {
        value.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s.toString().startsWith("75")) {
                    value.setText("")
                    Selection.setSelection(value.text, value.text.length)

                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }
        })
    }

    fun isDeviceRooted(context: Context?): Boolean? {
        return isrooted1() || isrooted2()
    }

    private fun isrooted1(): Boolean {
        val file = File("/system/app/Superuser.apk")
        return file.exists()
    }

    // try executing commands
    private fun isrooted2(): Boolean {
        return (canExecuteCommand("/sbin/su")
                || canExecuteCommand("/system/bin/su")
                || canExecuteCommand("/system/bin/failsafe/su")
                || canExecuteCommand("/system/xbin/su")
                || canExecuteCommand("/system/xbin/busybox  ")
                || canExecuteCommand("/system/sd/xbi"))
    }

    private fun canExecuteCommand(command: String): Boolean {
        return try {
            Runtime.getRuntime().exec(command)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun deleteImage(uri: Uri): Boolean = uri.path?.let { path ->
        File(path).delete()
    } ?: false

    @JvmStatic
    fun checkNumberWhileScanning(number: String): String {
        if (number.length == 9)
            return number
        else if (number.length == 10) {
            return number.substring(1, 10)
        }
        return number
    }


    fun isDistributor(role: String): Boolean {
        if (role.equals("DIST", true)) {
            return true
        }
        return false
    }

//
//    private fun getGlideUrl(filePath: String?, token: String): GlideUrl? {
//        var url: GlideUrl? = null
//        if (filePath != null) {
//            url = GlideUrl(
//                filePath,
//                LazyHeaders.Builder()
//                    .addHeader("token", token)
//                    .build()
//            )
//        }
//        return url
//    }
//
//    fun loadImage(context: Context, path: String?, token: String, imageView: ImageView) {
//        getGlideUrl(path, token).let { imageUrl ->
//            Glide.with(context)
//                .load(imageUrl)
//                .diskCacheStrategy(DiskCacheStrategy.ALL)
//                .into(imageView)
//        }
//    }


    /**
     * Here is the key method to apply the animation
     */
    fun setAnimation(context: Context?, viewToAnimate: View, position: Int) {
        val animation: Animation =
            AnimationUtils.loadAnimation(context, android.R.anim.slide_in_left)
        viewToAnimate.startAnimation(animation)
    }


}