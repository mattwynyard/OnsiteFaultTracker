package com.onsite.onsitefaulttracker;

import android.app.Application;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;

import com.onsite.onsitefaulttracker.activity.home.HomeFragment;
import com.onsite.onsitefaulttracker.connectivity.BLEManager;
import com.onsite.onsitefaulttracker.connectivity.BLTManager;
import com.onsite.onsitefaulttracker.connectivity.TcpConnection;
import com.onsite.onsitefaulttracker.model.notifcation_events.TCPStopRecordingEvent;
import com.onsite.onsitefaulttracker.util.BatteryUtil;
import com.onsite.onsitefaulttracker.util.BitmapSaveUtil;
import com.onsite.onsitefaulttracker.util.BusNotificationUtil;
import com.onsite.onsitefaulttracker.util.CalculationUtil;
import com.onsite.onsitefaulttracker.util.CameraUtil;
import com.onsite.onsitefaulttracker.util.RecordUtil;
import com.onsite.onsitefaulttracker.util.SettingsUtil;
import com.onsite.onsitefaulttracker.util.GPSUtil;

/**
 * Created by hihi on 6/6/2016.
 *
 * The Application class for this application.
 * Sets up Singletons and Utility classes.
 */
public class OnsiteApplication extends Application {

    // The tag name for this fragment
    private static final String TAG = OnsiteApplication.class.getSimpleName();

    /**
     * On Create
     * <p>
     * Sets up singletons and Utility classes
     */
    @Override
    public void onCreate() {
        super.onCreate();

        Fabric.with(this, new Crashlytics());
        // initialize the singletons used throughout this app
        SettingsUtil.initialize(this);
        CalculationUtil.initialize(this);
        CameraUtil.initialize(this);
        BatteryUtil.initialize(this);
        BitmapSaveUtil.initialize(this);
        RecordUtil.initialize(this);
        BLTManager.initialize(this);
        BusNotificationUtil.initialize(this);
        TcpConnection.initialize(this);
        GPSUtil.initialize(this);

        Thread.setDefaultUncaughtExceptionHandler(
                new Thread.UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(Thread thread, Throwable e) {
                        System.out.println("AppCrash");
                        //TcpConnection.getSharedInstance().sendMessage("Crash");
                        BLTManager.sharedInstance().sendMessage("Crash");
                        BusNotificationUtil.sharedInstance().postNotification(new TCPStopRecordingEvent());
                        //System.exit(1);
                    }
                });
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        //super.onLowMemory();
        Log.i(TAG, "APP: TERMINATE");
        TcpConnection.getSharedInstance().sendHomeWindowStatus("APP: TERMINATE");

    }
}

