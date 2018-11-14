package com.onsite.onsitefaulttracker.util;

import android.content.Context;
import android.os.Environment;
import android.os.StatFs;
import android.support.v4.content.ContextCompat;

import com.onsite.onsitefaulttracker.R;
import com.onsite.onsitefaulttracker.connectivity.TcpConnection;

/**
 * Created by hihi on 6/9/2016.
 *
 * Calculation Util provides functions for calculating if enough storage
 * space is available and all calculations related to this Application.
 * Access functions via CalculationUtil.sharedInstance()
 */
public class CalculationUtil {

    // The tag name for this utility class
    private static final String TAG = CalculationUtil.class.getSimpleName();

    // Number of seconds in one hour
    private static final long SECONDS_PER_HOUR = 3600;

    // Low storage space threshold
    private static final long LOW_STORAGE_THRESHOLD = 102400;

    // estimated size of an original bitmap to save in kb
    private static final long ESTIMATED_ORIGINAL_IMAGE_SIZE_KB = 1024;

    // Shared instance of the CalculationUtil to be initialized once and
    // used throughout the app
    private static CalculationUtil sCalculationUtil;

    // The application context used for retrieving storage space
    private Context mContext;

    /**
     * Initialize the Calculation Utils shared instance
     *
     * @param context
     */
    public static void initialize(final Context context) {
        sCalculationUtil = new CalculationUtil(context);
    }

    /**
     * Constructor, to be called internally only via the initialize function
     *
     * @param context The application context
     */
    private CalculationUtil(final Context context) {
        mContext = context;
    }

    /**
     * Returns the shared instance of CalculationUtil
     *
     * @return Shared instance of CalculationUttil
     */
    public static CalculationUtil sharedInstance() {
        if (sCalculationUtil != null) {
            return sCalculationUtil;
        } else {
            throw new RuntimeException("CalculationUtil must be initialized in the Application class before use");
        }
    }

    /**
     * Calculates and returns the needed storage space in kb to generate
     * one days work record
     *
     * @return needed storage space in KB to complete a days work
     */
    public long calculateNeededSpaceKB() {
        long imageSize = SettingsUtil.sharedInstance().getImageSize();
        float hours = SettingsUtil.sharedInstance().getRecordingHours();
        long millisecondsPerImage = SettingsUtil.sharedInstance().getPictureFrequency();

        long recordingSeconds = Math.round(hours * SECONDS_PER_HOUR);
        long imageCount = Math.round(recordingSeconds / (millisecondsPerImage / 1000.0));
        return imageSize * imageCount;
    }

    /**
     * Estimates and returns a float value that an image should be scaled by to get the
     * desired image size recorded in SettingsUtil
     *
     * @return
     */
    public float estimateScaleValueForImageSize() {
        long desiredKB = SettingsUtil.sharedInstance().getImageSize();
        if (desiredKB <= 50) {
            return 0.45f;
        } else if (desiredKB <= 100) {
            return 0.55f;
        } else if (desiredKB <= 150) {
            return 0.65f;
        } else if (desiredKB <= 200) {
            return 0.75f;
        } else if (desiredKB <= 250) {
            return 0.85f;
        } else if (desiredKB <= 300) {
            return 0.95f;
        } else {
            return 1.0f;
        }
    }

    /**
     * Estimates and returns a value that an image quality should be saved as to get the
     * desired image size recorded in SettingsUtil
     *
     * @return
     */
    public int estimateQualityValueForImageSize() {
        long desiredKB = SettingsUtil.sharedInstance().getImageSize();
        if (desiredKB <= 50) {
            return 50;
        } else if (desiredKB <= 100) {
            return 80;
        } else if (desiredKB <= 150) {
            return 90;
        } else if (desiredKB <= 200) {
            return 95;
        } else if (desiredKB <= 250) {
            return 95;
        } else if (desiredKB <= 300) {
            return 95;
        } else {
            return 100;
        }
    }

    /**
     * Get the available storage space in KB
     *
     * @return The available storage space in KB
     */
    public long getAvailableStorageSpaceKB() {
        StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
        long bytesAvailable = stat.getBlockSizeLong() * stat.getAvailableBlocksLong();
        return (bytesAvailable / 1024);
    }

    public long getLowStorageThreshold() {
        return LOW_STORAGE_THRESHOLD;
    }

    /**
     * gets the color that represents the available storage condition based on
     * how much is available and how much is required
     *
     * @return The color that represents the condition of available storage compared
     *         to required storage space
     */
    public int getAvailableStorageConditionColor() {
        long requiredStorage = calculateNeededSpaceKB();
        long availableStorage = getAvailableStorageSpaceKB();

        if (availableStorage <= requiredStorage) {
//            if (TcpConnection.getSharedInstance().isConnected()) {
//                TcpConnection.getSharedInstance().sendMessage("M: NOT ENOUGH AVAILABLE SPACE");
//            }
            return ContextCompat.getColor(mContext, R.color.no_storage);
        } else if (availableStorage - requiredStorage < LOW_STORAGE_THRESHOLD) {
//            if (TcpConnection.getSharedInstance().isConnected()) {
//                TcpConnection.getSharedInstance().sendMessage("M: LOW");
//            }
            return ContextCompat.getColor(mContext, R.color.low_storage);
        } else {
//            if (TcpConnection.getSharedInstance().isConnected()) {
//                TcpConnection.getSharedInstance().sendMessage("M: OK");
//            }
            return ContextCompat.getColor(mContext, R.color.good_storage);
        }
    }

    /**
     * Return a user friendly string display value given a value in kilobytes
     *
     * @return A user friendly string displaying the size in
     */
    public String getDisplayValueFromKB(final long valueInKb) {
        if (valueInKb < 1024) {
            return String.format(mContext.getString(R.string.kb_value), valueInKb);
        }
        float mbValue = valueInKb/1024.0f;
        if (mbValue < 1024.0f) {
            return String.format(mContext.getString(R.string.mb_value), mbValue);
        }
        float gbValue = mbValue/1024.0f;
        if (gbValue < 1024.0f) {
            return String.format(mContext.getString(R.string.gb_value), gbValue);
        }
        return " ";
    }

    /**
     * Returns a user friendly string displaying a time value from the given milliseconds
     *
     * @param milliseconds The milliseconds to convert into a display string
     * @return
     */
    public String getDisplayValueFromMilliseconds(final long milliseconds) {
        if (milliseconds == 0) {
            return String.format(mContext.getString(R.string.seconds_value), 0);
        } else if (milliseconds < 1000) {
            return String.format(mContext.getString(R.string.millis_value), milliseconds);
        }
        int secondsValue = (int)(milliseconds/1000);
        int minutesValue = secondsValue / 60;
        int hoursValue = minutesValue / 60;
        String hoursString = hoursValue > 0 ? String.format(mContext.getString(R.string.hours_value), hoursValue) + " " : "";
        String minutesString = minutesValue > 0 ? String.format(mContext.getString(R.string.minutes_value), minutesValue % 60) + " " : "";
        String secondsString = hoursValue == 0 ? String.format(mContext.getString(R.string.seconds_value), secondsValue % 60) : "";
        return hoursString + minutesString + secondsString;
    }

}
