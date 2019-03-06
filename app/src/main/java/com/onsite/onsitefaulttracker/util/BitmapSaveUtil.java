package com.onsite.onsitefaulttracker.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.location.Location;
import android.support.annotation.NonNull;
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
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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

    private ExecutorService mThreadPool;

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
        //mThreadPool = ThreadUtil.threadPool(10);
        mThreadPool  = Executors.newFixedThreadPool(10);
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

    public ExecutorService getThreadPool() {
        return mThreadPool;
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
                                       final Location location,
                                       final long time) {
        Date nowDate = new Date();
        String halfAppend = "";
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

        final String shortFileName = cameraIdPrefix + dateString + halfAppend;

        long availableSpace = CalculationUtil.sharedInstance().getAvailableStorageSpaceKB();

        if (availableSpace <= 1024) {
            return SaveBitmapResult.Error;
        }

        Runnable task = new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "ThreadCount-SaveBMP: " + Thread.activeCount());
                Log.i(TAG, "Current thread" + Thread.currentThread().getName());
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

                    Matrix matrix = new Matrix();
                    if (isLandscape) {
                        matrix.postRotate(-90);
                    }
                    Bitmap rotatedBitmap = Bitmap.createBitmap(sizedBmp, 0, 0,
                            sizedBmp.getWidth(), sizedBmp.getHeight(), matrix, true);

                    Bitmap resizedBitmap = Bitmap.createScaledBitmap(rotatedBitmap,
                            (int)(rotatedBitmap.getWidth() * THUMBNAIL_REDUCTION),
                            (int)(rotatedBitmap.getHeight() * THUMBNAIL_REDUCTION), true);

                    if (rotatedBitmap == null) {
                        return;
                    }
                    sizedBmp.recycle();
                    rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, CalculationUtil
                            .sharedInstance().estimateQualityValueForImageSize(), fOutputStream);
                    rotatedBitmap.recycle();
                    resizedBitmap.compress(Bitmap.CompressFormat.JPEG, CalculationUtil
                            .sharedInstance().estimateQualityValueForImageSize(), rOutputStream);
                    resizedBitmap.recycle();
                    bitmapToSave.recycle();

                    //BLTManager.sharedInstance().sendPhoto(message.toString(), resizedBitmap);
                    fOutputStream.flush();
                    fOutputStream.close();
                    rOutputStream.flush();
                    rOutputStream.close();

                    String message = buildMessage(time, filename, location);

                    sendMessage(message);
                    String _file = file.getAbsolutePath();
                    String _rfile = file_resize.getAbsolutePath();
                    GPSUtil.sharedInstance().geoTagFile(_file, location, time);
                    GPSUtil.sharedInstance().geoTagFile(_rfile, location, time);

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
                bitmapToSave.recycle();
            }
        };
        mThreadPool.submit(task);
        Log.i(TAG, "Photo Saved");

//        ThreadUtil.executeOnNewThread(new Runnable() {
//            @Override
//            public void run() {
//                Log.i(TAG, "Current thread" + Thread.currentThread());
//                String path = RecordUtil.sharedInstance().getPathForRecord(record);
//                OutputStream fOutputStream;
//                OutputStream rOutputStream;
//                File folder = new File(path);
//                if (!folder.exists()) {
//                    Log.e(TAG, "Error saving snap, Record path does not exist");
//                    return;
//                }
//                File file = new File(path + "/", filename + ".jpg");
//                File file_resize = new File(path + "/", filename + "R" + ".jpg");
//
//                try {
//                    fOutputStream = new FileOutputStream(file);
//                    rOutputStream = new FileOutputStream(file_resize);
//
//                    float reductionScale = CalculationUtil.sharedInstance().estimateScaleValueForImageSize();
//                    int outWidth = Math.round(bitmapToSave.getHeight() / widthDivisor);
//                    int outHeight = bitmapToSave.getHeight();
//                    Bitmap sizedBmp = Bitmap.createScaledBitmap(bitmapToSave,
//                            Math.round(outWidth * reductionScale), Math.round(outHeight * reductionScale), true);
//
//                    Matrix matrix = new Matrix();
//                    if (isLandscape) {
//                        matrix.postRotate(-90);
//                    }
//                    Bitmap rotatedBitmap = Bitmap.createBitmap(sizedBmp, 0, 0,
//                            sizedBmp.getWidth(), sizedBmp.getHeight(), matrix, true);
//
//                    Bitmap resizedBitmap = Bitmap.createScaledBitmap(rotatedBitmap,
//                            (int)(rotatedBitmap.getWidth() * THUMBNAIL_REDUCTION),
//                            (int)(rotatedBitmap.getHeight() * THUMBNAIL_REDUCTION), true);
//
//                    if (rotatedBitmap == null) {
//                        return;
//                    }
//                    sizedBmp.recycle();
//                    rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, CalculationUtil
//                            .sharedInstance().estimateQualityValueForImageSize(), fOutputStream);
//                    rotatedBitmap.recycle();
//                    resizedBitmap.compress(Bitmap.CompressFormat.JPEG, CalculationUtil
//                            .sharedInstance().estimateQualityValueForImageSize(), rOutputStream);
//                    resizedBitmap.recycle();
//                    bitmapToSave.recycle();
//
//                    //BLTManager.sharedInstance().sendPhoto(message.toString(), resizedBitmap);
//                    fOutputStream.flush();
//                    fOutputStream.close();
//                    rOutputStream.flush();
//                    rOutputStream.close();
//
//                    sendMessage(time, filename, location);
//                    GPSUtil.sharedInstance().geoTagFile(file.getAbsolutePath(), location);
//                    GPSUtil.sharedInstance().geoTagFile(file_resize.getAbsolutePath(), location);
//
//                    //Log.i(TAG, "Latitude: " + location.getLatitude());
//                    //Log.i(TAG, "Longitude: " + location.getLongitude());
//                    //Log.i(TAG, "Accuracy: " + location.getAccuracy());
//                    //send photo name to client
//                    //TcpConnection.getSharedInstance().sendMessage(filename + ".jpg");
//                    String satellites = Integer
//                                .toString(GPSUtil.sharedInstance().getSatellites());
//
//                    StringBuilder message = new StringBuilder();
//                    message.append("T:" + convertDate(time) + ",");
//                    message.append("C:" + shortFileName + ",");
//                    message.append("S:" + satellites + ",");
//                    message.append("A:" + location.getAccuracy());
//
////                    if ((BLTManager.sharedInstance().getState() == 3)) {
////                        if (count % 10 == 0) {
////                            BLTManager.sharedInstance().sendPhoto(message.toString(), resizedBitmap);
////                        } else {
////                            BLTManager.sharedInstance().sendMessage(message.toString());
////                        }
////                        count++;
////                    }
//                    //BLTManager.sharedInstance().sendMessage(message.toString());
////                    BLTManager.sharedInstance().sendPoolMessage(message.toString());
////                    Log.i(TAG, "Photo Saved");
//                } catch (FileNotFoundException e) {
//                    e.printStackTrace();
//                    return;
//                } catch (IOException e) {
//                    e.printStackTrace();
//                    return;
//                }
//                bitmapToSave.recycle();
//            }
//        });
        if (availableSpace <= LOW_DISK_SPACE_THRESHOLD) {
            return SaveBitmapResult.SaveLowDiskSpace;
        } else {
            return SaveBitmapResult.Save;
        }
    }

    private String buildMessage(long time, String file, Location location) {
        String satellites = Integer
                .toString(GPSUtil.sharedInstance().getSatellites());

        StringBuilder messageString = new StringBuilder();
        messageString.append("T:" + convertDate(time) + "|");
        messageString.append(file + "|");
        messageString.append(satellites + "|");
        messageString.append((int)location.getAccuracy() + ",");
        String message = messageString.toString();
        return message;
    }

    private void sendMessage(final String message ) {
        Runnable messgaeSend = new Runnable() {
            @Override
            public void run() {
                BLTManager.sharedInstance().sendPoolMessage(message);
            }
        };
        mThreadPool.execute(messgaeSend);

    }

    private String convertDate(long timestamp) {
        Calendar cal = Calendar.getInstance();
        TimeZone tz = cal.getTimeZone();
        /* date formatter in local timezone */
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        sdf.setTimeZone(tz);
        String localTime = sdf.format(new Date(timestamp));
        //Log.d("Time: ", localTime);
        return localTime;
    }

} //end class
