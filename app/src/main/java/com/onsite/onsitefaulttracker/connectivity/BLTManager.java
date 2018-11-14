package com.onsite.onsitefaulttracker.connectivity;

import android.Manifest;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import  android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.app.Service;

import android.content.IntentFilter;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.onsite.onsitefaulttracker.model.notifcation_events.TCPStartRecordingEvent;
import com.onsite.onsitefaulttracker.util.BusNotificationUtil;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.UUID;
import java.io.IOException;

import com.onsite.onsitefaulttracker.model.notifcation_events.BLTStartRecordingEvent;
import com.onsite.onsitefaulttracker.model.notifcation_events.BLTStopRecordingEvent;
import com.onsite.onsitefaulttracker.util.ThreadUtil;

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
    private static final String BLUETOOTH_ADAPTER_NAME = "OnSite_BLT_Adapter";
    // 128Bit Unique identifier for RFCOMM service
    private static final UUID UUID_UNSECURE = UUID.fromString("00030000-0000-1000-8000-00805F9B34FB");
    private static final String NAME = "OnsiteBluetoothserver";
    private int mState;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    // Shared Instance, to be initialized once and used throughout the application
    private static BLTManager sSharedInstance;
    // Application context
    private Application mApplicationContext;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mBluetoothDevice;
    //private BluetoothServerSocket mServerSocket;
    private BluetoothSocket mSocket;
    private AcceptThread mSecureAcceptThread;
    // Enable Bluetooth Request Id
    private static final int REQUEST_BLUETOOTH_ENABLE_FOR_SEND = 4;
    private static final int REQUEST_ENABLE_BT = 1;

    private Activity mActivity;
    private boolean mRecording;

    private Thread mReadThread;
    private BufferedReader mReaderIn;
    private String line="";
    private PrintWriter mWriterOut;

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
    }

    public void setupBluetooth() {

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBluetoothAdapter.setName(BLUETOOTH_ADAPTER_NAME);
        mState = STATE_NONE;
        //TelephonyManager tManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        //String uuid = tManager.getDeviceId();

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

    /**
     * Set the current state of the chat connection
     *
     * @param state An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    public synchronized void start() {
        Log.d(TAG, "start");

        // Cancel any thread attempting to make a connection
//        if (mConnectThread != null) {
//            mConnectThread.cancel();
//            mConnectThread = null;
//        }
//
//        // Cancel any thread currently running a connection
//        if (mConnectedThread != null) {
//            mConnectedThread.cancel();
//            mConnectedThread = null;
//        }

        setState(STATE_LISTEN);

        // Start the thread to listen on a BluetoothServerSocket
        if (mSecureAcceptThread == null) {
            mSecureAcceptThread = new AcceptThread(false);
            mSecureAcceptThread.start();
        }
//        if (mInsecureAcceptThread == null) {
//            mInsecureAcceptThread = new AcceptThread(false);
//            mInsecureAcceptThread.start();
//        }
    }

    /**
     * Send the recording status
     */
    private void sendRecordingStatus() {
        ThreadUtil.executeOnNewThread(new Runnable() {
            @Override
            public void run() {
                if ( mWriterOut != null) {
                    mWriterOut.println(mRecording ? "RECORDING\n" : "NOTRECORDING\n");
                    mWriterOut.flush();
                }
            }
        });
    }

    public boolean isBluetoothEnabled() {
        return mBluetoothAdapter.isEnabled();
    }

    public void startDiscovery() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        mApplicationContext.registerReceiver(mReceiver, filter);
        if (mBluetoothAdapter.isDiscovering()) {
            // Bluetooth is already in discovery mode, we cancel to restart it again
            mBluetoothAdapter.cancelDiscovery();
        }
        mBluetoothAdapter.startDiscovery();
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "onResumeCalled");
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.i(TAG, "Device name: " + device.getName());
                Log.i(TAG, "Device address: " + device.getAddress());
            }
            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                Log.i(TAG, "onResume: Discovery Started");
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.i(TAG, "onResume: Discovery Finished");
            }
//            if (BluetoothAdapter.STATE_ON.equals(action)) {
//                Log.i(TAG,"onResume: Discovery Started");
//
//            }
        }
    };

    //SPP maximum payload capacity is 128 bytes.
    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;
        private String mSocketType;



        public AcceptThread(boolean secure) {
            // Use a temporary object that is later assigned to mmServerSocket
            // because mmServerSocket is final.
            BluetoothServerSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            // Create a new listening server socket
            try {
                if (secure) {
                    tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME,
                            UUID_UNSECURE);
                } else {
                    //TODO fix up insecure connection
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

            // Keep listening until exception occurs or a socket is returned.
            while (mState != STATE_CONNECTED) {

                try {
                    Log.i(TAG,  "Server socket listening");
                    mSocket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket's accept() method failed", e);
                    break;
                }

                if (mSocket != null) {
                    // A connection was accepted. Perform work associated with
                    // the connection in a separate thread.
                    //manageMyConnectedSocket(socket);
                    Log.i(TAG, "Bluetooth socket accepted connection");
                    Log.i(TAG, "Connected to: " + mSocket.getRemoteDevice().getAddress());
                    mState = STATE_CONNECTED;
                    try {
                        mWriterOut = new PrintWriter(mSocket.getOutputStream(), true);
                        mWriterOut.println("CONNECTED\n");
                        mWriterOut.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    mReadThread = new Thread(readFromClient);
                    mReadThread.setPriority(Thread.MAX_PRIORITY);
                    mReadThread.start();
//                    try {
//                        mmServerSocket.close();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                    break;
                }

            }
        }
        // Closes the connect socket and causes the thread to finish.
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }

        private Runnable readFromClient = new Runnable() {
            @Override
            public void run() {
                byte[] buffer = new byte[1024];
                int bytes;

                Log.i(TAG, "Read thread started");
                try {
                    InputStream in = mSocket.getInputStream();
                    //mReaderIn = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
                    //while ((line = mReaderIn.readLine()) != null) {
                    while ((bytes = in.read(buffer)) > 0) {
                            String line = new String(buffer, 0, bytes);
                            System.out.println(line);
                            if (line.contains("Start")) {
                                if (!mRecording) {
                                    BusNotificationUtil.sharedInstance().postNotification(new BLTStartRecordingEvent());
                                }
                            } else if (line.contains("Stop")) {
                                if (mRecording) {
                                    BusNotificationUtil.sharedInstance().postNotification(new BLTStopRecordingEvent());
                                }
                            }
                    }
                    in.close();
                    //closeAll();
                    Log.e(TAG, "OUT OF WHILE");
                }
                catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        };

    } //end private class

} //end class

