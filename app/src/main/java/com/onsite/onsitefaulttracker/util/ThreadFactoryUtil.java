package com.onsite.onsitefaulttracker.util;

import android.util.Log;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadFactoryUtil implements ThreadFactory {

    // The tag name for this class
    private static final String TAG = ThreadFactoryUtil.class.getSimpleName();

    String name;
    AtomicInteger threadNo = new AtomicInteger(0);

    public ThreadFactoryUtil(String name) {
        this.name = name;
    }

    public Thread newThread(Runnable r) {

        String threadName = name + ":" + threadNo.incrementAndGet();
        Log.i(TAG, "threadName:" + threadName);
        return new Thread(r, threadName);
    }
}
