package com.onsite.onsitefaulttracker.connectivity;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.onsite.onsitefaulttracker.model.notifcation_events.UsbConnectedNotification;
import com.onsite.onsitefaulttracker.model.notifcation_events.UsbDisconnectedNotification;
import com.onsite.onsitefaulttracker.model.notifcation_events.TCPStartRecordingEvent;
import com.onsite.onsitefaulttracker.model.notifcation_events.TCPStopRecordingEvent;
import com.onsite.onsitefaulttracker.util.BatteryUtil;
import com.onsite.onsitefaulttracker.util.BitmapSaveUtil;
import com.onsite.onsitefaulttracker.util.BusNotificationUtil;
import com.onsite.onsitefaulttracker.util.CalculationUtil;
import com.onsite.onsitefaulttracker.util.ThreadUtil;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.SocketAddress;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiInfo;
import java.util.*;
import java.math.BigInteger;
import org.apache.commons.lang3.ArrayUtils;

/**
 * TcpConnection class handles socket communication with a client.
 *
 * Used for communicating via USB once a Tcp connection has been
 * established via the Android adb tool
 */
public class TcpConnection implements Runnable {

    // Tag name for this class
    private static final String TAG = TcpConnection.class.getSimpleName();

    // Shared instance of TCP connection
    private static TcpConnection sharedInstance;

    private static final int TIMEOUT=1000;
    private String connectionStatus=null;
    private Handler mHandler;
    private ServerSocket server=null;
    private Context context;
    private Socket client=null;
    private Socket socket=null;
    //DataOutputStream out = null;
    private String line="";
    private BufferedReader socketIn;
    private PrintWriter socketOut;
    private boolean mStarted;
    private boolean mRecording;
    private Thread mTcpThread;
    private Thread mReadThread;
    private InetAddress address;
    //private String address;
    private static boolean DEBUG = true;

    /**
     * Returns the shared instance of TcpConnection
     */
    public static TcpConnection getSharedInstance() {
        if (sharedInstance != null) {
            return sharedInstance;
        } else {
            throw new RuntimeException("TcpConnection must be initialized");
        }
    }
    /**
     * Initializes the tcp connection
     *
     * @param context
     */
    public static void initialize(Context context) {
        sharedInstance = new TcpConnection(context);
    }

    private TcpConnection(Context c) {
        // TODO Auto-generated constructor stub
        Log.i(TAG, "(TCP) TcpConnection creator");
        context=c;
        mStarted = false;
        mRecording = false;
        mHandler=new Handler();
    }

    /**
     * Start the tcp connection
     */
    public void startTcpConnection() {
        if (mStarted || mTcpThread != null) {
            return;
        }

        mStarted = true;
        mTcpThread = new Thread(this);
        mTcpThread.setPriority(Thread.MAX_PRIORITY);
        mTcpThread.start();
        Log.i(TAG, "Started Tcp Thread");
    }

    /**
     * Stop the tcp connection
     */
    private void stopTcpConnection() {
        if (!mStarted) {
            return;
        }
        mStarted = false;
        if (mTcpThread != null) {
            try {
                mTcpThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        mTcpThread = null;
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
     * Send the recording status
     */
    private void sendRecordingStatus() {
        ThreadUtil.executeOnNewThread(new Runnable() {
            @Override
            public void run() {
                if (socketOut != null) {
                    socketOut.println(mRecording ? "RECORDING\n" : "NOTRECORDING\n");
                    socketOut.flush();
                }
            }
        });
    }
    /**
     * Send the photo being saved
     */
    public void sendMessage(final String message) {
        ThreadUtil.executeOnNewThread(new Runnable() {
            @Override
            public void run() {
                if (socketOut != null) {
                    socketOut.println(message);
                    socketOut.flush();
                }
            }
        });
    }
    /**
     * Send the window status
     */
    public void sendHomeWindowStatus(final String status) {
        ThreadUtil.executeOnNewThread(new Runnable() {
            @Override
            public void run() {
                if (socketOut != null) {
                    socketOut.println(status);
                    socketOut.flush();
                }
            }
        });
    }
    /**
     * Send the connection status
     */
    private void sendConnectionStatus() {
        ThreadUtil.executeOnNewThread(new Runnable() {
            @Override
            public void run() {
                if (socketOut != null) {
                    socketOut.println(connectionStatus);
                    socketOut.flush();
                }
            }
        });
    }

//    /**
//     * TODO Fix for sending photos via sockets
//     */
//    public void sendPhoto(final byte[] array) {
//        ThreadUtil.executeOnNewThread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    DataOutputStream dataOut = new DataOutputStream(out);
//                    dataOut.writeInt(array.length);
//                    dataOut.write(array, 0, array.length);
//                    dataOut.flush();
//                    Log.i(TAG, "Sent bytes");
//                    client.close();
//
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        });
//    }
//

    private void sendMemoryStatus() {

        long availableSpace = CalculationUtil.sharedInstance().getAvailableStorageSpaceKB();
        long calculateNeededSpaceKB = CalculationUtil.sharedInstance().calculateNeededSpaceKB();

        if (availableSpace <= 1024) {
            sendMessage("M: Not enough disk space");
        } else if ((availableSpace - calculateNeededSpaceKB) < CalculationUtil.sharedInstance().getLowStorageThreshold()) {
            sendMessage("M: Low disk space");
        } else {
            sendMessage("M: Disk space ok");
        }
    }

    private void sendBatteryStatus() {
        float currentBatteryLevel = BatteryUtil.sharedInstance().getBatteryLevel();
        int batteryLevel = Math.round(currentBatteryLevel);
        if (TcpConnection.getSharedInstance().isConnected()) {
            String msg = "B: " + Integer.toString(batteryLevel) + "%";
            TcpConnection.getSharedInstance().sendMessage(msg);
        }
    }

    /**
     * Returns true if connected
     *
     * @return
     */
    public boolean isConnected() {
        return socketOut != null;
    }

    /**
     * Converts ip address in int form to bytes
     * @param hex
     * @return
     */
    private static byte[] int32toBytes(int hex) {
        byte[] b = new byte[4];
        b[3] = (byte) ((hex & 0xFF000000) >> 24);
        b[2] = (byte) ((hex & 0x00FF0000) >> 16);
        b[1] = (byte) ((hex & 0x0000FF00) >> 8);
        b[0] = (byte) (hex & 0x000000FF);
        return b;
    }

    /**
     * Opens a port and listens for a socket connections
     */
    @Override
    public void run() {
        // TODO Auto-generated method stub
        // initialize server socket
        Log.i(TAG, "(TCP) TcpConnection run");
        try {
            Log.i(TAG, "(TCP) create server socket");
            if (!DEBUG) {
                ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                WifiInfo connectionInfo = null;
                try {
                    connectionInfo = wm.getConnectionInfo();
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }

                int ipAddress = connectionInfo.getIpAddress();
                byte[] bytes = int32toBytes(ipAddress);

                address = InetAddress.getByAddress(bytes);
                Log.i(TAG, address.toString());
                server = new ServerSocket(38300, 0, address);
                Log.i(TAG, "(TCP) added server socket on " + address.toString() + ":38300");
                server.setSoTimeout(TIMEOUT * 1000);
            } else {
                server = new ServerSocket(38300);
                //Log.i(TAG, "(TCP) added server socket on " + address.toString() + ":38500");
                //out = new DataOutputStream(socket.getOutputStream());
                String address = server.getInetAddress().toString();
                Log.i(TAG, "(TCP) added server socket on " + address + ":38300");
                server.setSoTimeout(TIMEOUT * 1000);

            }
        } catch (IOException e1) {
            Log.i(TAG, "(TCP) server create exception");
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        //attempt to accept a connection
        try{
            Log.i(TAG, "(TCP) will call server.accept");
            client = server.accept();
            Log.i(TAG, "(TCP) did accept");
            socketOut = new PrintWriter(client.getOutputStream(), true);
            socketOut.println("CONNECTED\n");
            socketOut.flush();

            BusNotificationUtil.sharedInstance().postNotification(new UsbConnectedNotification());

            mReadThread = new Thread(readFromClient);
            mReadThread.setPriority(Thread.MAX_PRIORITY);
            mReadThread.start();
            Log.e(TAG, "Sent");
        }
        catch (SocketTimeoutException e) {
            // print out TIMEOUT
            connectionStatus="Connection has timed out! Please try again";
            mHandler.post(showConnectionStatus);
            try {
                server.close();
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }
        catch (IOException e) {
            Log.e(TAG, ""+e);
        }

        if (client!=null) {
            try{
                // print out success
                connectionStatus="Connection successful!";
                Log.e(TAG, connectionStatus);
                mHandler.post(showConnectionStatus);
                sendBatteryStatus();
                sendMemoryStatus();
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    /**
     * To run in the background,  reads in comming data
     * from the client
     */
    private Runnable readFromClient = new Runnable() {

        @Override
        public void run() {
            // TODO Auto-generated method stub
            try {
                Log.e(TAG, "Reading from client");
                socketIn = new BufferedReader(new InputStreamReader(client.getInputStream()));
                while ((line = socketIn.readLine()) != null) {
                    Log.d("ServerActivity", line);
                    //Do something with line
                    if (line.contains("START")) {
                        if (!mRecording) {
                            BusNotificationUtil.sharedInstance().postNotification(new TCPStartRecordingEvent());
                        }
                    } else if (line.contains("STOP")) {
                        if (mRecording) {
                            BusNotificationUtil.sharedInstance().postNotification(new TCPStopRecordingEvent());
                        }
                    }
                }
                socketIn.close();
                closeAll();
                Log.e(TAG, "OUT OF WHILE");
            }
            catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    };

    /**
     * Close the socket connection if open and restart listening for a connection
     */
    private void closeAll() {
        // TODO Auto-generated method stub
        try {
            BusNotificationUtil.sharedInstance().postNotification(new UsbDisconnectedNotification());
            BusNotificationUtil.sharedInstance().postNotification(new TCPStopRecordingEvent());
            Log.e(TAG, "Closing All");
            socketOut.close();
            socketOut = null;
            client.close();
            server.close();

            stopTcpConnection();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        restartTcpConnection();
    }

    /**
     * Restarts listening for a socket connection
     * after a one second wait
     */
    private void restartTcpConnection() {
        ThreadUtil.executeOnMainThreadDelayed(new Runnable() {
            @Override
            public void run() {
                startTcpConnection();
            }
        }, 1000);
    }

    /**
     * Show the connection status in a Toast and send
     * the current status through the open socket if a
     * connection is established
     */
    private Runnable showConnectionStatus = new Runnable() {
        public void run() {
            try
            {
                Toast.makeText(context, connectionStatus, Toast.LENGTH_SHORT).show();
                sendRecordingStatus();
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
    };

}