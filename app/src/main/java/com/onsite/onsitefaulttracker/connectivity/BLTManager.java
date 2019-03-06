package com.onsite.onsitefaulttracker.connectivity;

import android.app.Application;
//Bluetooth
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;

import java.io.File;
import java.io.FileInputStream;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.HardwarePropertiesManager;
import android.util.Log;
//Notifications
import com.onsite.onsitefaulttracker.activity.home.HomeFragment;
import com.onsite.onsitefaulttracker.model.notifcation_events.BLTStartRecordingEvent;
import com.onsite.onsitefaulttracker.model.notifcation_events.BLTStopRecordingEvent;
import com.onsite.onsitefaulttracker.model.notifcation_events.BLTConnectedNotification;
import com.onsite.onsitefaulttracker.model.notifcation_events.BLTNotConnectedNotification;
import com.onsite.onsitefaulttracker.model.notifcation_events.BLTListeningNotification;
import com.onsite.onsitefaulttracker.util.BusNotificationUtil;

import java.io.InputStream;
import java.io.PrintWriter;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


import com.onsite.onsitefaulttracker.util.CalculationUtil;
import com.onsite.onsitefaulttracker.util.ThreadUtil;

import static android.os.HardwarePropertiesManager.DEVICE_TEMPERATURE_CPU;
import static android.os.HardwarePropertiesManager.TEMPERATURE_CURRENT;

/**
 * Bluetooth Classic Device manager,
 *
 * Manages Bluetooth Classic devices,
 * Provides an interface for communicating with Bluetooth Classic Devices
 */

public class BLTManager extends Activity {

    // Tag name for this class
    private static final String TAG = BLTManager.class.getSimpleName();
    // Adapter name for this android device when advertising
    //private static final String BLUETOOTH_ADAPTER_NAME = "OnSite_BLT_Adapter_99"; //"OnSite_BLT_Adapter_99"
    private static String BLUETOOTH_ADAPTER_NAME;
    // 128Bit Unique identifier for RFCOMM service
    private static final UUID UUID_UNSECURE = UUID.fromString("00030000-0000-1000-8000-00805F9B34FB");
    private static final String NAME = "OnsiteBluetoothserver";
    private int mState;
    final Object lock = new Object();
    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device
    public static final int STATE_TIMEOUT = 4;  // now connected to a remote device
    public static final int STATE_NOTENABLED = 9;  // bluetooth not enabled on phone
    // Shared Instance, to be initialized once and used throughout the application
    private static BLTManager sSharedInstance;
    // Application context
    private Application mApplicationContext;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothSocket mSocket;
    private AcceptThread mAcceptThread;
    private Thread mReadThread;
    private InputStream in;
    private PrintWriter mWriterOut;

    private boolean mRecording;
    private HardwarePropertiesManager mHPManager;

    private ExecutorService mThreadPool;

    /**
     * initialize the BLTManager class,  to be called once from the application class
     *
     * @param applicationContext The application context
     */
    public static void initialize(final Application applicationContext) {
        sSharedInstance = new BLTManager(applicationContext);
    }

    /**
     * returns the shared instance of BLEManager
     *
     * @return
     */
    public static BLTManager sharedInstance() {
        if (sSharedInstance != null) {
            return sSharedInstance;
        } else {
            throw new RuntimeException("BLTManager must be initialized before use");
        }
    }

    /**
     * Constructor, called privately through the initialize function
     *
     * @param applicationContext
     */
    private BLTManager(final Application applicationContext) {
        mApplicationContext = applicationContext;
        //mThreadPool = ThreadUtil.threadPool(6);
        mThreadPool  = Executors.newFixedThreadPool(10);
        setupBluetooth();
        Log.i(TAG, "Bluetooth Setup");
    }

    /**
     * Intialise the bluetooth adapter and set name and connection state
     */
    public void setupBluetooth() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;


    }
    /**
     * Update whether the app is recording or not
     *
     * @param recording
     */
    public void setRecording(boolean recording) {
        mRecording = recording;
        sendRecordingStatus();
    }
    /**
     * Return the current connection state.
     */
    public synchronized int getState() {
        return mState;
    }

    public void setBTName(String id) {
        Log.d(TAG, "Phone Id: " + id);
        //BLUETOOTH_ADAPTER_NAME = id;
        mBluetoothAdapter.setName(id);

    }
    /**
     * Set the current state of the chat connection
     *
     * @param state An integer defining the current connection state
     */
    public synchronized void setState(int state) {
        Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;

    }

    private void startBLTConnection() {
        start();
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    public synchronized void start() {
        Log.d(TAG, "start");

        //Cancel any thread attempting to make a connection
        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }
        // Cancel any thread currently running a connection
        if (mReadThread != null) {
            try {
                mReadThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mReadThread = null;
        }
        // Start the thread to listen on a BluetoothServerSocket
        if (mAcceptThread == null) {
            mAcceptThread = new AcceptThread(false);
            mAcceptThread.start();
            setState(STATE_CONNECTING);
        }
    }


//    public void sendPhoto(final String header, final Bitmap bmp) {
//        ThreadUtil.executeOnNewThread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//
//                    //Bitmap tmpBmp = Bitmap.createBitmap(bmp);
//                    byte[] ascii = header.getBytes(StandardCharsets.US_ASCII);
//
//                    ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
//                    ByteArrayOutputStream photoOut = new ByteArrayOutputStream();
//                    //Bitmap bmp = BitmapFactory.decodeFile("/storage/emulated/0/OnSite/photo.jpg");
//                    //bmp.setConfig(Bitmap.Config.ARGB_8888);
//                    //bmp.setPremultiplied(true);
//                    bmp.compress(Bitmap.CompressFormat.JPEG, 100, photoOut);
//                    int width = bmp.getWidth();
//                    int height = bmp.getHeight();
//
//                    Log.i(TAG, "BMP_HEIGHT : " + height);
//                    Log.i(TAG, "BMP_WIDTH : " + width);
//
//                    System.out.println(photoOut.size());
//                    //Bitmap immutableBmp = Bitmap.createBitmap(bmp);
//                    System.out.println("Byte count: " + bmp.getByteCount());
//                    int size = bmp.getRowBytes() * bmp.getHeight();
//
//                    System.out.println("Row bytes: " + bmp.getRowBytes());
//                    //bmp.setConfig(Bitmap.Config.ARGB_8888);
////                    ByteBuffer buffer = ByteBuffer.allocate(size);
////                    byte[] photo = new byte[size];
////                    //buffer.order(ByteOrder.nativeOrder());
////                    bmp.copyPixelsToBuffer(buffer);
////                    bmp.recycle();
////                    buffer.rewind();
////                    buffer.get(photo);
//                    //byte[] photo = buffer.array();
//
//                    //byte[] photo = new byte[photoOut.size()];
//                    //byteOut.write(photo);
//                    byte[] photo = photoOut.toByteArray();
//
//
//                    //int size = (int) f.length();
//                    String start = "PHOTO";
//                    byte[] prefix = start.getBytes(StandardCharsets.US_ASCII);
//
//                    Log.i(TAG, "BMP SIZE: " + size);
//
//                    byte [] messageLength = ByteBuffer.allocate(4).putInt(ascii.length).array();
//                    byte [] photoLength = ByteBuffer.allocate(4).putInt(photo.length).array();
//
//                    Log.i(TAG, "BYTES: " + photo.length);
//                    //reader.read(photo, 0, photo.length);
//                    //reader.close();
//
//                    byteOut.write(prefix); //ascii
//                    byteOut.write(messageLength); //int
//                    byteOut.write(ascii); //ascii
//                    byteOut.write(photoLength); //int
//                    byteOut.write(photo); //image data
//                    //byteOut.write(footer);
//
//                    //System.out.println("BYTES: " + photoLength.length);
//                    byteOut.writeTo(mSocket.getOutputStream());
//                    mSocket.getOutputStream().flush();
//
//
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        });
//    }

//    /**
//     * Send the recording status
//     */
    public void sendMessage(final String message) {

        ThreadUtil.executeOnNewThread(new Runnable() {
            @Override
            public void run() {
                if (mWriterOut != null) {
                    Log.i(TAG, "ThreadCount: " + Thread.activeCount());


                    byte[] ascii = message.getBytes(StandardCharsets.US_ASCII);
                    ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
                    try {
                        byteOut.write(ascii);
                        byteOut.write(0x0a); //newline
                        //Log.i(TAG, "PAYLOAD: " + byteOut.size());
                        byteOut.writeTo(mSocket.getOutputStream());
                        mSocket.getOutputStream().flush();
                        //byteOut.reset();
                        byteOut.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    /**
     * Send the recording status
     */
    public void sendPoolMessage(final String message) {

        Runnable task = new Runnable() {
            @Override
            public void run() {
                if (mWriterOut != null) {
                    Log.i(TAG, "ThreadCount-Message: " + Thread.activeCount());
                    System.out.println("Current thread : " + Thread.currentThread().getName());
                    byte[] ascii = message.getBytes(StandardCharsets.US_ASCII);
                    ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
                    try {
                        byteOut.write(ascii);
                        byteOut.write(0x0a); //newline
                        //Log.i(TAG, "PAYLOAD: " + byteOut.size());
                        byteOut.writeTo(mSocket.getOutputStream());
                        mSocket.getOutputStream().flush();
                        //byteOut.reset();
                        byteOut.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        mThreadPool.submit(task);
    }

    /**
     * Send the recording status
     */
//    private void sendRecordingStatus() {
//        ThreadUtil.executeOnMainThread(new Runnable() {
//            @Override
//            public void run() {
//                if ( mWriterOut != null) {
//                    sendMessage(mRecording ? "RECORDING" : "NOTRECORDING");
//                    mWriterOut.flush();
//                }
//            }
//        });
//    }

//    /**
//     * Send the recording status
//     */
    private void sendRecordingStatus() {
        sendMessage(mRecording ? "RECORDING" : "NOTRECORDING");
//        Runnable task = new Runnable() {
//            @Override
//            public void run() {
//                if ( mWriterOut != null) {
//                    sendMessage(mRecording ? "RECORDING" : "NOTRECORDING");
//                    mWriterOut.flush();
//                }
//            }
//        };
        //mThreadPool.submit(task);
    }
    /**
     * Checks if bluetooth on the adapter is enabled.
     * @return true/false if blue is enabled.
     */
    public boolean isBluetoothEnabled() {
        return mBluetoothAdapter.isEnabled();
    }

    /**
     * Class to handle the accept connection from bluetooth device behaves as server socket
     * and runs on its own thread
     */
    //SPP maximum payload capacity is 128 bytes.
    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;
        private String mSocketType;

        private AcceptThread(boolean secure) {
            // Use a temporary object that is later assigned to mmServerSocket
            // because mmServerSocket is final.
            BluetoothServerSocket tmp = null;

            mSocketType = secure ? "Secure" : "Insecure";
            // Create a new listening server socket
            Log.i(TAG, "NAME: " + NAME);
            Log.i(TAG, "NAME: " + UUID_UNSECURE);
            Log.i(TAG, "ADDRESS = " + mBluetoothAdapter.getAddress());
            try {
                if (secure) {
                    tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME,
                            UUID_UNSECURE);
                } else {
                    tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(
                            NAME, UUID_UNSECURE);
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "listen() failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            Log.d(TAG, "Socket Type: " + mSocketType +
                    " BEGIN mAcceptThread" + this);
            setName("AcceptThread" + mSocketType);
            BusNotificationUtil.sharedInstance().postNotification(new BLTListeningNotification());
            // Keep listening until exception occurs or a socket is returned.
            while (mState != STATE_CONNECTED) {
                try {
                    Log.i(TAG,  "Server socket listening");
                    mSocket = mmServerSocket.accept();
                    setState(STATE_LISTEN);
                } catch (IOException e) {
                    Log.e(TAG, "Socket's accept() method failed", e);
                    break;
                }
                if (mSocket != null) {
                    // A connection was accepted. Perform work associated with
                    // the connection in a separate thread.
                    Log.i(TAG, "Bluetooth socket accepted connection");
                    Log.i(TAG, "Connected to: " + mSocket.getRemoteDevice().getAddress());
                    setState(STATE_CONNECTED);
                    BusNotificationUtil.sharedInstance().postNotification(new BLTConnectedNotification());
                    try {
                        mWriterOut = new PrintWriter(mSocket.getOutputStream(), true);
                        //mWriterOut.println("CONNECTED");
                        sendMessage("CONNECTED");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    mReadThread = new Thread(readFromClient);
                    mReadThread.setName("ReadThread");
                    mReadThread.setPriority(Thread.MAX_PRIORITY);
                    mReadThread.start();
                }
            }
        }
        // Closes the connect socket and causes the thread to finish.
        public void cancel() {
            try {
                mmServerSocket.close();
                mAcceptThread.join();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            } catch (InterruptedException e) {
                Log.e(TAG, "Thread interupt", e);
                e.printStackTrace();
            }
        }
        /**
         * Close the socket connection if open and restart listening for a connection
         */
        private void closeAll() {
            try {
                BusNotificationUtil.sharedInstance().postNotification(new BLTStopRecordingEvent());
                Log.e(TAG, "Closing All");
                BusNotificationUtil.sharedInstance().postNotification(new BLTNotConnectedNotification());
                in.close();
                in = null;
                mSocket.close();
                mmServerSocket.close();
                mReadThread = null;
                restartBLTConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        /**
         * Restarts listening for a socket connection
         * after a one second wait
         */
        private void restartBLTConnection() {
            ThreadUtil.executeOnMainThreadDelayed(new Runnable() {
                @Override
                public void run() {
                    startBLTConnection();
                }
            }, 1000);
            Log.i(TAG, "Restarting connection");
        }

        private final Runnable readFromClient = new Runnable() {
            @Override
            public void run() {
                byte[] buffer = new byte[128];
                int length;
                Log.i(TAG, "Read thread started");
                try {
                    in = mSocket.getInputStream();
                    while ((length = in.read(buffer)) != -1) {
                        String line = new String(buffer, 0, length);
                        //System.out.println(line);
                        if (line.contains("Start")) {
                            if (!mRecording) {
                                BusNotificationUtil.sharedInstance()
                                        .postNotification(new BLTStartRecordingEvent());
                            }
                        } else if (line.contains("Stop")) {
                            if (mRecording) {
                                BusNotificationUtil.sharedInstance()
                                        .postNotification(new BLTStopRecordingEvent());
                            }
                        } else {
                            //System.out.println(line);
                        }
                    }
                } catch (IOException e) { //connection was lost
                    e.printStackTrace();
                } finally {
                    BusNotificationUtil.sharedInstance()
                            .postNotification(new BLTStopRecordingEvent());
                    setState(STATE_NONE);
                    closeAll();
                }
            }
        }; //end closure
    } //end private class
} //end class

