package com.onsite.onsitefaulttracker.util;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

/**
 * Created by hihi on 6/21/2016.
 *
 * Battery Util,  provides functions to return battery status information
 */
public class BatteryUtil {

    // The tag name for this utility class
    private static final String TAG = BatteryUtil.class.getSimpleName();

    // Static instance of the Battery Utils class
    private static BatteryUtil sBatteryUtil;

    // The application context,  used for enquiring of of the battery status
    private Context mContext;

    /**
     * initialize the BatteryUtil class,  to be called once from the application class
     *
     * @param context The application context
     */
    public static void initialize(final Context context) {
        sBatteryUtil = new BatteryUtil(context);
    }

    /**
     * returns the shared instance of BatteryUtil
     *
     * @return
     */
    public static BatteryUtil sharedInstance() {
        if (sBatteryUtil != null) {
            return sBatteryUtil;
        } else {
            throw new RuntimeException("BatteryUtil must be initialized before use");
        }
    }

    /**
     * Constructor, called privately through the initialize function
     *
     * @param context
     */
    private BatteryUtil(final Context context) {
        mContext = context;
    }

    /**
     * returns true if the charger is currently connected to the android device
     * @return
     */
    public boolean isChargerConnected() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = mContext.registerReceiver(null, ifilter);
        int status = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1) : -1;
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;
    }

    /**
     * Get the current battery level percentage
     *
     * @return
     */
    public float getBatteryLevel() {
        Intent batteryIntent = mContext.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryIntent == null) {
            return 50.0f;
        }
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        // Check for error results, if error return 50 percent
        if(level == -1 || scale == -1) {
            return 50.0f;
        }

        return ((float)level / (float)scale) * 100.0f;
    }

}
