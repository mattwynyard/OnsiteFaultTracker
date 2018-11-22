package com.onsite.onsitefaulttracker.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.location.Location;
import android.support.media.ExifInterface;
import android.util.Log;

import com.onsite.onsitefaulttracker.connectivity.BLTManager;
import com.onsite.onsitefaulttracker.connectivity.TcpConnection;
import com.onsite.onsitefaulttracker.model.Record;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.ByteArrayOutputStream;
import java.util.TimeZone;

/**
 * Created by hihi on 6/21/2016.
 *
 * Utility class which will handle saving bitmaps to storage
 */
public class BitmapSaveUtil {

    // The tag name for this utility class
    private static final String TAG = BitmapSaveUtil.class.getSimpleName();

    // The low disk space threshold
    private static final long LOW_DISK_SPACE_THRESHOLD = 102400L;

    // An enum which has all the SaveBitmapResult values
    public enum SaveBitmapResult {
        Save,
        SaveLowDiskSpace,
        Error
    }

    // The format of file names when converted from a date
    private static final String FILE_DATE_FORMAT = "yyMMdd HHmmss";

    // A static instance of the bitmap save utilities
    private static BitmapSaveUtil sBitmapSaveUtil;

    // Store the application context for access to storage
    private static Context mContext;

    /**
     * Store the appication context for access to storage
     *
     * @param context
     */
    public static void initialize(final Context context) {
        sBitmapSaveUtil = new BitmapSaveUtil(context);
    }

    /**
     * Contructor, to be called internally via. initialize
     * @param context
     */
    private BitmapSaveUtil(final Context context) {
        mContext = context;
    }

    /**
     * Returns a shared instance of BitmapSaveUtil
     * @return
     */
    public static BitmapSaveUtil sharedInstance() {
        if (sBitmapSaveUtil != null) {
            return sBitmapSaveUtil;
        } else {
            throw new RuntimeException("BitmapSaveUtil must be initialized in the Application class before use");
        }
    }

    //TODO fix for sending photos to controller
//    public void sendBitmapResult(final Bitmap bmp) {
//
//        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
//        bmp.compress(Bitmap.CompressFormat.JPEG, 0, byteStream);
//        byte[] array = byteStream.toByteArray();
//        TcpConnection.getSharedInstance().sendPhoto(array);
//
//    }
    /**
     * Saves a bitmap to storage taking in a temp number for now for the filename
     *
     * @param bitmapToSave
     * @param record
     * @param widthDivisor a factor to divide the width by
     */
    public SaveBitmapResult saveBitmap(final Bitmap bitmapToSave,
                                       final Record record,
                                       final float widthDivisor,
                                       final boolean isLandscape,
                                       final Location location) {
        Date nowDate = new Date();
        String halfAppend = "";
        long time = nowDate.getTime();
        boolean useHalfAppend = (SettingsUtil.sharedInstance().getPictureFrequency() % 1000 > 0);
        if (useHalfAppend && (time % 1000) >= 500) {
            halfAppend = "_500";
        } else {
            halfAppend = "_000";
        }

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(FILE_DATE_FORMAT);
        String dateString = simpleDateFormat.format(nowDate);
        String cameraIdPrefix = SettingsUtil.sharedInstance().getCameraId();
        if (cameraIdPrefix == null) {
            cameraIdPrefix = "NOID";
        }
        cameraIdPrefix += "_";
        final String filename = cameraIdPrefix + "IMG" + dateString + halfAppend;

        long availableSpace = CalculationUtil.sharedInstance().getAvailableStorageSpaceKB();

        if (availableSpace <= 1024) {
            return SaveBitmapResult.Error;
        }

        ThreadUtil.executeOnNewThread(new Runnable() {
            @Override
            public void run() {
                String path = RecordUtil.sharedInstance().getPathForRecord(record);
                OutputStream fOutputStream;
                File folder = new File(path);
                if (!folder.exists()) {
                    Log.e(TAG, "Error saving snap, Record path does not exist");
                    return;
                }
                File file = new File(path + "/", filename + ".jpg");

                try {
                    fOutputStream = new FileOutputStream(file);

                    float reductionScale = CalculationUtil.sharedInstance().estimateScaleValueForImageSize();
                    int outWidth = Math.round(bitmapToSave.getHeight() / widthDivisor);
                    int outHeight = bitmapToSave.getHeight();
                    Bitmap sizedBmp = Bitmap.createScaledBitmap(bitmapToSave, Math.round(outWidth * reductionScale), Math.round(outHeight * reductionScale), true);

                    Matrix matrix = new Matrix();
                    if (isLandscape) {
                        matrix.postRotate(-90);
                    }
                    Bitmap rotatedBitmap = Bitmap.createBitmap(sizedBmp, 0, 0, sizedBmp.getWidth(), sizedBmp.getHeight(), matrix, true);
                    if (rotatedBitmap == null) {
                        return;
                    }
                    sizedBmp.recycle();

                    rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, CalculationUtil.sharedInstance().estimateQualityValueForImageSize(), fOutputStream);
                    rotatedBitmap.recycle();

                    bitmapToSave.recycle();


                    fOutputStream.flush();
                    fOutputStream.close();
                    geoTagFile(file.getAbsolutePath(), location);

                    //send photo name to client
                    //TcpConnection.getSharedInstance().sendMessage(filename + ".jpg");
                    if (BLTManager.sharedInstance().getState() == 3) {
                        BLTManager.sharedInstance().sendMessage(filename + ".jpg");
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
                bitmapToSave.recycle();
            }
        });

        if (availableSpace <= LOW_DISK_SPACE_THRESHOLD) {
            return SaveBitmapResult.SaveLowDiskSpace;
        } else {
            return SaveBitmapResult.Save;
        }
    }

    //private String getImage(String)

    private void geoTagFile(String path, Location location) {
        File f = new File(path);
        long gpsTime = location.getTime();

        Date date = new Date(gpsTime);
        DateFormat dateformater = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        dateformater.setTimeZone(TimeZone.getDefault());
        String dateFormatted = dateformater.format(date);

        Log.d(TAG, "GPS Time: " + dateFormatted);
//        if (f.exists()) {
//            Log.e(TAG, "File exists and canRead = " + f.canRead());
//        }
        // if (path == null || location == null) return false;
        ExifInterface exif;
        try {
            exif = new ExifInterface(path);
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, DMS(location.getLatitude()));
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, location.getLatitude()
                    < 0 ? "S" : "N");
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, DMS(location.getLongitude()));
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, location.getLongitude()
                    < 0 ? "W" : "E");
            //exif.setAttribute(ExifInterface.);
            exif.saveAttributes();

            Log.d(TAG, "Wrote geotag" + path);
            Log.d(TAG, "Latitude " + exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE));
            Log.d(TAG, "Longitude " + exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE));
        } catch (IOException e) {
            e.printStackTrace();
            //return false;
        }
        //return true;
    }

    private String DMS(double x) {
        double d = Math.abs(x);
        int degrees = (int) Math.floor(d);
        int minutes = (int) Math.floor(((d - (double)degrees) * 60));
        int seconds = (int)(((((d - (double)degrees) * 60) - (double)minutes) * 60) * 1000);
        return String.format("%d/1,%d/1,%d/1000", degrees, minutes, seconds);
    }


} //end class
