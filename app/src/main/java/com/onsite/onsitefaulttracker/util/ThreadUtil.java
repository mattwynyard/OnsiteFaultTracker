package com.onsite.onsitefaulttracker.util;

import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by hihi on 6/13/2016.
 *
 * Thread Util,
 * provides common functions for running code on the main thread,
 * or a background thread.
 */
public class ThreadUtil {

//    /**
//     * Creates a executor service with a fixed number of threads
//     * @param number the number of threads to create in the thread pool
//     * @return the excutor service (thread pool)
//     */
//    public static ExecutorService threadPool(int number) {
//        ExecutorService executor  = Executors.newFixedThreadPool(number);
//        return executor;
//    }

    /**
     * ensures code is executed on the main thread.
     *
     * @param runnable
     */
    public static void executeOnMainThread(Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(runnable);
        }
    }

    /**
     * executes code after specified delay on the main thread.
     *
     * @param runnable
     * @param delay
     */
    public static void executeOnMainThreadDelayed(Runnable runnable, long delay) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(runnable, delay);
    }

    /**
     * Remove a runnable which is waiting to be executed from the main thread
     *
     * @param runnable
     */
    public static void removeDelayedRunnableFromMainThread(Runnable runnable) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.removeCallbacks(runnable);
    }

    /**
     * executes code on a new background thread.
     *
     * @param runnable
     */
    public static void executeOnNewThread(final Runnable runnable) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                runnable.run();
            }
        }).start();
    }

    /**
     * executes code on a new background thread after the specified amount of time (delay)
     *
     * @param runnable
     * @param delay
     */
    public static void executeOnNewThreadDelayed(final Runnable runnable, long delay) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        runnable.run();
                    }
                }).start();
            }
        }, delay);
    }

}
