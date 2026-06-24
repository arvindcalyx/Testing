package com.test.safetyconnect.main;



import static com.test.safetyconnect.main.Utils.getOutputMediaFileUri;
import static com.test.safetyconnect.main.Utils.getOutputMediaFileUriForCamera;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.location.Location;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.test.safetyconnect.R;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;


public class CameraActivity extends Activity implements OnClickListener {

    public static final String PIC_EXTRA = "image";
    private static final int EXTERNAL_PERM = 20;
    //keep track of camera capture intent
    final int CAMERA_CAPTURE = 1;
    final int PIC_CROP = 2;
    final int PIC_SIZE = 1024;
    // For Debugging
    private final String className = "CameraAct";
    private boolean showClassLog = true;
    //captured picture uri
    private Uri requestPicUri, currentPicUri, croppedPicUri, oldPicUri;
    private ImageView clickedPic;
    //private Bitmap picBitmap;
    private boolean isPassportSize = false;
    private static String customerImage = "";
    private static Location location;

    private Context mContext;

    public CameraActivity newInstance(@NotNull String s) {
        CameraActivity cameraActivity = new CameraActivity();
        customerImage = s;
        return cameraActivity;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            mContext = this;
            try {
                StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
                StrictMode.setVmPolicy(builder.build());
            } catch (Exception e) {
            }
            setContentView(R.layout.activity_camera);
            clickedPic = (ImageView) findViewById(R.id.clickedpic);
            try {
                StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
                StrictMode.setVmPolicy(builder.build());
            } catch (Exception e) {
                Timber.e(e, className + " >> onCreate >> Exception: " + e);
            }
            findViewById(R.id.retakebtn).setOnClickListener(this);
            findViewById(R.id.approvebtn).setOnClickListener(this);
            if (savedInstanceState == null) {
                if (!Utils.isPermissionsGranted(this)) {
                    new AlertDialog.Builder(CameraActivity.this)
                            .setMessage(getResources().getString(R.string.please_provide))
                            .setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    checkCameraPermissions();
                                }
                            })
                            .setCancelable(false)
                            .show();

                } else {
                    if (!customerImage.equals("customerImage") && customerImage.isEmpty()) {
                        Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        requestPicUri = getOutputMediaFileUriForCamera("", this);

                        captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, requestPicUri);
                        captureIntent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, PIC_SIZE);

                        captureIntent.putExtra("outputX", PIC_SIZE);
                        captureIntent.putExtra("outputY", PIC_SIZE);
                        captureIntent.putExtra("aspectX", PIC_SIZE);
                        captureIntent.putExtra("aspectY", PIC_SIZE);
                        captureIntent.putExtra("scale", true);
                        captureIntent.putExtra("scaleUpIfNeeded", false);

                        startActivityForResult(captureIntent, CAMERA_CAPTURE);
                    } else {
                        takeCustomerImage();
                    }

                }
            }
        } catch (Exception e) {
            Timber.e(e, className + " >> onCreate >> Exception: " + e);
        }
    }

    public void takeCustomerImage() {

    }

    public void addWaterMarkImage(@Nullable Uri data) {

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (requestPicUri != null) {
            outState.putString("requestUri", requestPicUri.toString());
        }

        if (croppedPicUri != null) {
            outState.putString("croppedUri", croppedPicUri.toString());
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState.containsKey("requestUri")) {
            requestPicUri = Uri.parse(savedInstanceState.getString("requestUri"));
        }

        if (savedInstanceState.containsKey("croppedUri")) {
            croppedPicUri = Uri.parse(savedInstanceState.getString("croppedUri"));
        }
    }

    private void performCrop() {
        currentPicUri = requestPicUri;
        rotationImage();
        clickedPic.setImageURI(requestPicUri);

    }

    private void rotationImage() {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(currentPicUri.getPath(), bounds);

        BitmapFactory.Options opts = new BitmapFactory.Options();
        Bitmap bm = BitmapFactory.decodeFile(currentPicUri.getPath(), opts);
//        bounds.inBitmap = bm;
        ExifInterface exif = null;
        try {
            exif = new ExifInterface(currentPicUri.getPath());
            String orientString = exif.getAttribute(ExifInterface.TAG_ORIENTATION);
            int orientation = orientString != null ? Integer.parseInt(orientString) : ExifInterface.ORIENTATION_NORMAL;

            int rotationAngle = 0;
            if (orientation == ExifInterface.ORIENTATION_ROTATE_90) rotationAngle = 90;
            if (orientation == ExifInterface.ORIENTATION_ROTATE_180) rotationAngle = 180;
            if (orientation == ExifInterface.ORIENTATION_ROTATE_270) rotationAngle = 270;

            Matrix matrix = new Matrix();
            matrix.setRotate(rotationAngle, (float) bm.getWidth() / 2, (float) bm.getHeight() / 2);
            Bitmap rotatedBitmap = Bitmap.createBitmap(bm, 0, 0, bounds.outWidth, bounds.outHeight, matrix, true);
            clickedPic.setImageBitmap(rotatedBitmap);
            Timber.d(className + " >> Org Uri: " + currentPicUri);
            Timber.d(className + " >> Org Uri Path: " + currentPicUri.getPath());
            saveToFile(currentPicUri.getPath(), rotatedBitmap);
        } catch (IOException e) {
            Timber.e(e, className + " >> rotationImage >> Exception: " + e);
        }
    }

    public void saveToFile(String fileName, Bitmap bitmap) {
        try {
            FileOutputStream out = new FileOutputStream(fileName);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            Timber.e(e, className + " >> saveToFile >> Exception: " + e);
        }
    }

    public Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 0) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            if (resultCode == RESULT_OK) {
                if (requestCode == CAMERA_CAPTURE) {
                    if (data != null && data.getData() != null) {
                        currentPicUri = data.getData();
                    } else {
                        currentPicUri = requestPicUri;
                    }
                    if (currentPicUri != null) {
                        try {
                            performCrop();
                        } catch (Exception e) {
                            Timber.e(e, className + " >> onActivityResult >> Exception: " + e);
                            Toast.makeText(this, getResources().getString(R.string.sorry_we_would), Toast.LENGTH_LONG).show();
                        }
                    }
                }
                if (requestCode == PIC_CROP) {
                    clickedPic.setImageURI(croppedPicUri);
                    currentPicUri = croppedPicUri;
                }
            } else {
                setResult(RESULT_CANCELED);
                finish();
            }
        } catch (Exception e) {
            Timber.e(e, className + " >> onActivityResult >> Exception: " + e);
        }
    }

    @Override
    public void onClick(View v) {
        try {
            int id = v.getId();
            if (id == R.id.approvebtn) {
                if (currentPicUri != null) {
                    customerImage = "";
                    Intent picInt = new Intent();
                    oldPicUri = currentPicUri;
                    currentPicUri = Uri.parse(compressImage(currentPicUri.getPath()));
                    picInt.putExtra(PIC_EXTRA, currentPicUri);
                    new File(oldPicUri.getPath()).delete();
                    setResult(RESULT_OK, picInt);
                    finish();
                }
            } else if (id == R.id.retakebtn) {
                if (!customerImage.equals("customerImage") && customerImage.isEmpty()) {
                    Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    // requestPicUri = getOutputMediaFileUri("");
                    requestPicUri = getOutputMediaFileUriForCamera("", this);
                    captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, requestPicUri);
                    captureIntent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, PIC_SIZE);
                    captureIntent.putExtra("outputX", PIC_SIZE);
                    captureIntent.putExtra("outputY", PIC_SIZE);
                    captureIntent.putExtra("aspectX", PIC_SIZE);
                    captureIntent.putExtra("aspectY", PIC_SIZE);
                    captureIntent.putExtra("scale", true);
                    captureIntent.putExtra("scaleUpIfNeeded", false);
                    startActivityForResult(captureIntent, CAMERA_CAPTURE);
                } else {
                    takeCustomerImage();
                }
            }
        } catch (Exception e) {
            Timber.e(e, className + " >> onClick >> Exception: " + e);
        }
    }

    private void checkCameraPermissions() {
        boolean value = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;

        List<String> permissions = new ArrayList<>();

        if (!value) {
            permissions.add(Manifest.permission.CAMERA);
        }


        Dexter.withActivity(this)
                .withPermissions(permissions)
                .withListener(new MultiplePermissionsListener() {

                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {

                        if (report.areAllPermissionsGranted()) {
                            //Camera Permission has been granted, show camera preview.
                            Timber.d(className + " >> Camera Permission has " +
                                    "been granted.");
                            Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                            requestPicUri = getOutputMediaFileUriForCamera("", CameraActivity.this);
                            captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, requestPicUri);
                            //			captureIntent.putExtra(MediaStore.EXTRA_SCREEN_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                            captureIntent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, PIC_SIZE);
                            //if(isPassportSize){̥
                            //                captureIntent.putExtra("crop", "true");
                            captureIntent.putExtra("outputX", PIC_SIZE);
                            captureIntent.putExtra("outputY", PIC_SIZE);
                            captureIntent.putExtra("aspectX", PIC_SIZE);
                            captureIntent.putExtra("aspectY", PIC_SIZE);
                            captureIntent.putExtra("scale", true);
                            captureIntent.putExtra("scaleUpIfNeeded", false);
                            startActivityForResult(captureIntent, CAMERA_CAPTURE);
                        } else if (report.isAnyPermissionPermanentlyDenied()) {
                            new AlertDialog.Builder(CameraActivity.this)
                                    .setMessage(getResources().getString(R.string.retail_sr))
                                    .setPositiveButton(getResources().getString(R.string.settings), new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                            goToSettings();
                                        }
                                    })
                                    .setCancelable(false)
                                    .show();

                        } else if (report.getDeniedPermissionResponses().size() > 0) {
                            finish();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }

                }).withErrorListener(error -> {
            Toast.makeText(this, error.toString(), Toast.LENGTH_SHORT).show();
            finish();
        }).onSameThread().check();
//        ActivityCompat.requestPermissions(this, new String[]{
//                Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, EXTERNAL_PERM);
    }

    /**
     * Callback received when permission request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == EXTERNAL_PERM) {
            // Received Permission result for Storage Permission
            Timber.d(className + " >> Received response for " +
                    "Camera Permission");
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //Camera Permission has been granted, show camera preview.
                Timber.d(className + " >> Camera Permission has " +
                        "been granted.");
                Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                requestPicUri = getOutputMediaFileUriForCamera("", this);
                captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, requestPicUri);
                //			captureIntent.putExtra(MediaStore.EXTRA_SCREEN_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                captureIntent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, PIC_SIZE);
                //if(isPassportSize){̥
                //                captureIntent.putExtra("crop", "true");
                captureIntent.putExtra("outputX", PIC_SIZE);
                captureIntent.putExtra("outputY", PIC_SIZE);
                captureIntent.putExtra("aspectX", PIC_SIZE);
                captureIntent.putExtra("aspectY", PIC_SIZE);
                captureIntent.putExtra("scale", true);
                captureIntent.putExtra("scaleUpIfNeeded", false);
                startActivityForResult(captureIntent, CAMERA_CAPTURE);
            } else {
                // Camera Permission was not granted or the SyncData has checked "Don't show again."
                // while requesting permission.
                Timber.d(className + " >> Camera Permission was " +
                        "not granted.");
                // Check to see if the SyncData has denied permission first time or checked
                // "Don't show again.
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                    Toast.makeText(CameraActivity.this, getResources().getString(R.string.permissions_not_granted), Toast.LENGTH_SHORT).show();
                    //close the app
                    finish();
//                    android.os.Process.killProcess(android.os.Process.myPid());
//                    System.exit(0);
                } else {
                    new AlertDialog.Builder(CameraActivity.this)
                            .setMessage(getResources().getString(R.string.retail_sr))
                            .setPositiveButton(getResources().getString(R.string.settings), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    goToSettings();
                                }
                            })
                            .setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
//                                    android.os.Process.killProcess(android.os.Process.myPid());
                                    finish();
                                }
                            })
                            .setCancelable(false)
                            .show();
                }
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /**
     * Open settings of the App.
     */
    private void goToSettings() {
        Intent myAppSettings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        myAppSettings.setData(Uri.parse("package:" + getPackageName()));
        myAppSettings.addCategory(Intent.CATEGORY_DEFAULT);
        myAppSettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(myAppSettings);
    }

    public String compressImage(String imageUri) {

        //String filePath = getRealPathFromURI(imageUri);
        String filePath = imageUri;
        Bitmap scaledBitmap = null;

        BitmapFactory.Options options = new BitmapFactory.Options();

//      by setting this field as true, the actual bitmap pixels are not loaded in the memory. Just the bounds are loaded. If
//      you try the use the bitmap here, you will get null.
        options.inJustDecodeBounds = true;
        //options.inSampleSize = 3;
        Bitmap bmp = BitmapFactory.decodeFile(filePath, options);

        int actualHeight = options.outHeight;
        int actualWidth = options.outWidth;

//      max Height and width values of the compressed image is taken as 816x612

        float maxHeight = 816.0f;
        float maxWidth = 612.0f;
        float imgRatio = actualWidth / actualHeight;
        float maxRatio = maxWidth / maxHeight;

//      width and height values are set maintaining the aspect ratio of the image

        if (actualHeight > maxHeight || actualWidth > maxWidth) {
            if (imgRatio < maxRatio) {
                imgRatio = maxHeight / actualHeight;
                actualWidth = (int) (imgRatio * actualWidth);
                actualHeight = (int) maxHeight;
            } else if (imgRatio > maxRatio) {
                imgRatio = maxWidth / actualWidth;
                actualHeight = (int) (imgRatio * actualHeight);
                actualWidth = (int) maxWidth;
            } else {
                actualHeight = (int) maxHeight;
                actualWidth = (int) maxWidth;

            }
        }

//      setting inSampleSize value allows to load a scaled down version of the original image

        options.inSampleSize = calculateInSampleSize(options, actualWidth, actualHeight);

//      inJustDecodeBounds set to false to load the actual bitmap
        options.inJustDecodeBounds = false;

//      this options allow android to claim the bitmap memory if it runs low on memory
        options.inPurgeable = true;
        options.inInputShareable = true;
        options.inTempStorage = new byte[16 * 1024];

        try {
//          load the bitmap from its path
            bmp = BitmapFactory.decodeFile(filePath, options);
        } catch (OutOfMemoryError exception) {

        }
        try {
            scaledBitmap = Bitmap.createBitmap(actualWidth, actualHeight, Bitmap.Config.ARGB_8888);
        } catch (OutOfMemoryError exception) {
        }

        float ratioX = actualWidth / (float) options.outWidth;
        float ratioY = actualHeight / (float) options.outHeight;
        float middleX = actualWidth / 2.0f;
        float middleY = actualHeight / 2.0f;

        Matrix scaleMatrix = new Matrix();
        scaleMatrix.setScale(ratioX, ratioY, middleX, middleY);

        Canvas canvas = new Canvas(scaledBitmap);
        canvas.setMatrix(scaleMatrix);
        canvas.drawBitmap(bmp, middleX - bmp.getWidth() / 2, middleY - bmp.getHeight() / 2, new Paint(Paint.FILTER_BITMAP_FLAG));

//      check the rotation of the image and display it properly
        ExifInterface exif;
        try {
            exif = new ExifInterface(filePath);

            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, 0);
            Matrix matrix = new Matrix();
            if (orientation == 6) {
                matrix.postRotate(90);
            } else if (orientation == 3) {
                matrix.postRotate(180);
            } else if (orientation == 8) {
                matrix.postRotate(270);
            }
            scaledBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0,
                    scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix,
                    true);
        } catch (IOException e) {
        }

        FileOutputStream out = null;
        String filename = getOutputMediaFileUri("", mContext).getPath();
        try {
            out = new FileOutputStream(filename);

//          write the compressed bitmap at the destination specified by filename.
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);

        } catch (FileNotFoundException e) {
        }

        return filename;

    }

    /*public String getFilename() {
        File file = new File(Environment.getExternalStorageDirectory().toString() + File.separator + Constants.RETSL_FOLDER,
                Constants.IMAGES_TEMP);
        if (!file.exists()) {
            file.mkdirs();
        }
        String uriSting = (file.getAbsolutePath() + "/" + System.currentTimeMillis() + ".jpg");
        return uriSting;

    }*/

    private String getRealPathFromURI(String contentURI) {
        Uri contentUri = Uri.parse(contentURI);
        Cursor cursor = getContentResolver().query(contentUri, null, null, null, null);
        if (cursor == null) {
            return contentUri.getPath();
        } else {
            cursor.moveToFirst();
            int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            return cursor.getString(index);
        }
    }

    public int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        final float totalPixels = width * height;
        final float totalReqPixelsCap = reqWidth * reqHeight * 2;
        while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
            inSampleSize++;
        }

        return inSampleSize;
    }


}