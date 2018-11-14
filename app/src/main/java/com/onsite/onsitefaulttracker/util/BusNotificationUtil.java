package com.onsite.onsitefaulttracker.util;

import android.content.Context;

import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

/**
 * Bus Notification Utils
 *
 * Provides an interface that uses Bus Notifications to easily send notifications
 * throughout the app
 */
public class BusNotificationUtil {

    // Tag name for this utility class
    private static final String TAG = BusNotificationUtil.class.getSimpleName();

    // Shared instance to be initialized once and used throughout the app
    private static BusNotificationUtil sBusNotificationUtil;

    // Bus object of Bus Notifications
    private static Bus mBus;

    /**
     * Initialize BusNotificationUtil
     *
     * @param context
     */
    public static void initialize(Context context) {
        sBusNotificationUtil = new BusNotificationUtil(context);
    }

    /**
     * Gets the shared instance of BusNotificationUtil
     *
     * @return BusNotificationUtil shared instance
     */
    public static BusNotificationUtil sharedInstance() {
        if (sBusNotificationUtil != null) {
            return sBusNotificationUtil;
        } else {
            throw new RuntimeException("BusNotificationUtils must be initialized before use");
        }
    }

    /**
     * Constructor, only to be called internally on initialization
     *
     * @param context
     */
    private BusNotificationUtil(Context context) {
        mBus = new Bus(ThreadEnforcer.MAIN);
    }

    /**
     * Return the Bus object that will be used throughout the app
     *
     * @return
     */
    public Bus getBus() {
        return mBus;
    }

    /**
     * Post a bus notification
     *
     * @param notification
     */
    public void postNotification(final Object notification) {
        ThreadUtil.executeOnMainThread(new Runnable() {
            @Override
            public void run() {
                mBus.post(notification);
            }
        });
    }

}
