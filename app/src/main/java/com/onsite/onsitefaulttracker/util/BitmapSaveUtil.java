package com.onsite.onsitefaulttracker.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.location.Location;
import android.support.media.ExifInterface;
import android.util.Log;

import com.onsite.onsitefaulttracker.connectivity.BLTManager;
import com.onsite.onsitefaulttracker.connectivity.BLTMessage;
import com.onsite.onsitefaulttracker.model.Record;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
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

    private static final double THUMBNAIL_REDUCTION = 0.25;

    int count = 0;

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
    private Context mContext;

    /**
     * Store the appication context for access to storage
     *
     * @param context - the application context
     */
    public static void initialize(final Context context) {
        sBitmapSaveUtil = new BitmapSaveUtil(context);
    }

    /**
     * Contructor, to be called internally via. initialize
     * @param context - the application context
     */
    private BitmapSaveUtil(final Context context) {
        mContext = context;
    }

    /**
     * Returns a shared instance of BitmapSaveUtil
     * @return - the shared instance of BitmapSaveUtil
     */
    public static BitmapSaveUtil sharedInstance() {
        if (sBitmapSaveUtil != null) {
            return sBitmapSaveUtil;
        } else {
            throw new RuntimeException("BitmapSaveUtil must be initialized in the Application class before use");
        }
    }
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
        final long time = nowDate.getTime();
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
                OutputStream rOutputStream;
                File folder = new File(path);
                if (!folder.exists()) {
                    Log.e(TAG, "Error saving snap, Record path does not exist");
                    return;
                }
                File file = new File(path + "/", filename + ".jpg");
                File file_resize = new File(path + "/", filename + "R" + ".jpg");

                try {
                    fOutputStream = new FileOutputStream(file);
                    rOutputStream = new FileOutputStream(file_resize);

                    float reductionScale = CalculationUtil.sharedInstance().estimateScaleValueForImageSize();
                    int outWidth = Math.round(bitmapToSave.getHeight() / widthDivisor);
                    int outHeight = bitmapToSave.getHeight();
                    Bitmap sizedBmp = Bitmap.createScaledBitmap(bitmapToSave,
                            Math.round(outWidth * reductionScale), Math.round(outHeight * reductionScale), true);

                    Log.i(TAG, "Landscape: " + isLandscape);

//                    Matrix matrix = new Matrix();
//                    if (isLandscape) {
//                        matrix.postRotate(-90);
//                    }
//                    Bitmap rotatedBitmap = Bitmap.createBitmap(sizedBmp, 0, 0,
//                            sizedBmp.getWidth(), sizedBmp.getHeight(), matrix, true);

                    Bitmap resizedBitmap = Bitmap.createScaledBitmap(sizedBmp,
                            (int)(sizedBmp.getWidth() * THUMBNAIL_REDUCTION),
                            (int)(sizedBmp.getHeight() * THUMBNAIL_REDUCTION), true);

//                    Bitmap tmpBitmap = Bitmap.createScaledBitmap(sizedBmp,
//                            (int)(sizedBmp.getWidth() * THUMBNAIL_REDUCTION),
//                            (int)(sizedBmp.getHeight() * THUMBNAIL_REDUCTION), true);



//                    if (rotatedBitmap == null) {
//                        return;
//                    }



//                    rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, CalculationUtil
//                            .sharedInstance().estimateQualityValueForImageSize(), fOutputStream);
                    sizedBmp.compress(Bitmap.CompressFormat.JPEG, CalculationUtil
                            .sharedInstance().estimateQualityValueForImageSize(), fOutputStream);
                    resizedBitmap.compress(Bitmap.CompressFormat.JPEG, CalculationUtil
                            .sharedInstance().estimateQualityValueForImageSize(), rOutputStream);

                    //BLTManager.sharedInstance().sendPhoto(message.toString(), resizedBitmap);

                    //sizedBmp.recycle();
                    //rotatedBitmap.recycle();


                    fOutputStream.flush();
                    fOutputStream.close();
                    rOutputStream.flush();
                    rOutputStream.close();

                    geoTagFile(file.getAbsolutePath(), location);
                    geoTagFile(file_resize.getAbsolutePath(), location);
                    Log.i(TAG, "Latitude: " + location.getLatitude());
                    Log.i(TAG, "Longitude: " + location.getLongitude());
                    Log.i(TAG, "Accuracy: " + location.getAccuracy());
                    //send photo name to client
                    //TcpConnection.getSharedInstance().sendMessage(filename + ".jpg");
                        String satellites = Integer
                                .toString(GPSUtil.sharedInstance().getSatellites());

                    StringBuilder message = new StringBuilder();
                    message.append("T:" + convertDate(time) + ",");
                    message.append("C:" + filename + ",");
                    message.append("S:" + satellites + ",");
                    message.append("A:" + location.getAccuracy());

//                    if ((BLTManager.sharedInstance().getState() == 3)) {
//                        if (count % 10 == 0) {
//                            BLTManager.sharedInstance().sendPhoto(message.toString(), tmpBitmap);
//                        } else {
//                            BLTManager.sharedInstance().sendMessage(message.toString());
//                        }
//                        count++;
//                    }
                    BLTManager.sharedInstance().sendMessage(message.toString());
                    sizedBmp.recycle();
                    resizedBitmap.recycle();
                    bitmapToSave.recycle();

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
                //bitmapToSave.recycle();
            }
        });
        if (availableSpace <= LOW_DISK_SPACE_THRESHOLD) {
            return SaveBitmapResult.SaveLowDiskSpace;
        } else {
            return SaveBitmapResult.Save;
        }
    }

    private String convertDate(long timestamp) {
        Calendar cal = Calendar.getInstance();
        TimeZone tz = cal.getTimeZone();
        /* date formatter in local timezone */
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        sdf.setTimeZone(tz);
        String localTime = sdf.format(new Date(timestamp));
        Log.d("Time: ", localTime);
        return localTime;
    }
                        //--EXIF FUNCTIONS--
//TODO fix for negative altitudes
    private void geoTagFile(String path, Location location) {
        File f = new File(path);
        long time = location.getTime();
        String timeStamp = getDateTimeStamp(time, "time");
        String dateStamp = getDateTimeStamp(time, "date");
        ExifInterface exif;
        try {
            exif = new ExifInterface(path);
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE,
                    DMS(location.getLatitude(), 10000));
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, location.getLatitude()
                    < 0 ? "S" : "N");
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE,
                    DMS(location.getLongitude(), 10000));
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, location.getLongitude()
                    < 0 ? "W" : "E");
            exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE,
                    formatEXIFDouble(location.getAltitude(), 100));
            exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF, location.getAltitude()
                    < 0 ? "1" : "0");
            exif.setAttribute(ExifInterface.TAG_GPS_IMG_DIRECTION,
                    formatEXIFDouble((double)location.getBearing(), 100));
            exif.setAttribute(ExifInterface.TAG_GPS_TIMESTAMP, timeStamp);
            exif.setAttribute(ExifInterface.TAG_GPS_DATESTAMP, dateStamp);
            exif.setAttribute(ExifInterface.TAG_GPS_MAP_DATUM, "WGS_84");
            exif.saveAttributes();
//
//            Log.d(TAG, "Wrote geotag" + path);
//            Log.d(TAG, "Latitude " + exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE));
//            Log.d(TAG, "Longitude " + exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE));
//            Log.d(TAG, "Altitude " + exif.getAttribute(ExifInterface.TAG_GPS_ALTITUDE));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getDateTimeStamp(long gpsTime, String type) {
        Date date = new Date(gpsTime);
        DateFormat dateformater = new SimpleDateFormat("yyyyMMddHHmmssZZZZZ");
        dateformater.setTimeZone(TimeZone.getDefault());
        String timestamp = dateformater.format(date);

        if (type.equals("date")) {
            StringBuilder s = new StringBuilder();
            String year = timestamp.substring(0,4);
            String month = timestamp.substring(4,6);
            String day = timestamp.substring(6,8);
            s.append(day);
            s.append(":");
            s.append(month);
            s.append(":");
            s.append(year);
            return s.toString();
        } else {
            StringBuilder s = new StringBuilder();
            String hour = timestamp.substring(8,10);
            String minutes = timestamp.substring(10,12);
            String seconds = timestamp.substring(12,14);
            //String zone = timestamp.substring(14,20);
            s.append(hour);
            s.append(":");
            s.append(minutes);
            s.append(":");
            s.append(seconds);
            return s.toString();
        }
    }
    /**
     * Converts a double value to the exif format
     * @param x - the number to convert
     * @param precision - the multiplier for altitude precision i.e the number of decimal places.
     * @return the converted coordinate as a string in the exif format
     */
    private String formatEXIFDouble(double x, int precision) {
        Double d = Math.abs(x) * precision;
        int altitude = (int)Math.floor(d);
        return String.format("%d/" + String.valueOf(precision), altitude);
    }
    /**
     * Converts decimal lat/long coordinate to degrees, minutes, seconds. The returned string is in
     * the exif format
     *
     * @param x - the coordinate to convert
     * @param precision - the multiplier for seconds precision
     * @return the converted coordinate as a string in the exif format
     */
    private String DMS(double x,  int precision) {
        double d = Math.abs(x);
        int degrees = (int) Math.floor(d);
        int minutes = (int) Math.floor(((d - (double)degrees) * 60));
        int seconds = (int)(((((d - (double)degrees) * 60) - (double)minutes) * 60) * precision);
        return String.format("%d/1,%d/1,%d/" + precision, degrees, minutes, seconds);
    }
} //end class
