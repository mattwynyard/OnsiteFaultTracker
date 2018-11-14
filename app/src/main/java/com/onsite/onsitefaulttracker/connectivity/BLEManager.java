package com.onsite.onsitefaulttracker.connectivity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.ParcelUuid;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.onsite.onsitefaulttracker.model.notifcation_events.BLEStartRecordingEvent;
import com.onsite.onsitefaulttracker.model.notifcation_events.BLEStopRecordingEvent;
import com.onsite.onsitefaulttracker.util.BusNotificationUtil;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Bluetooth LE Device manager,
 *
 * Manages Bluetooth LE devices,
 * Provides an interface for communicating with Bluetooth LE Devices
 */
public class BLEManager {

    // Tag name for this class
    private static final String TAG = BLEManager.class.getSimpleName();

    // Adapter name for this android device when advertising
    private static final String BLUETOOTH_ADAPTER_NAME = "OnSite_BLE_Adapter";

    // BLE Command Constants
    private static final int RECORDING_VALUE_STOP = 0;
    private static final int RECORDING_VALUE_START = 1;
    private static final int RECORDING_VALUE_IGNORE = 2;

    // Shared Instance, to be initialized once and used throughout the application
    private static BLEManager sSharedInstance;

    // List of service UUIDs
    private List<ParcelUuid> mServiceUuids;

    // Is it currently advertising
    private boolean mAdvertising;

    // Android Bluetooth Manager
    private BluetoothManager mBluetoothManager;

    // Android Bluetooth Adapter
    private BluetoothAdapter mBluetoothAdapter;

    // Bluetooth LE Advertiser
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;

    // Bluetooth Gatt Server
    private BluetoothGattServer mGattServer;

    // Store the last connected device
    private BluetoothDevice mLastConnectedDevice;

    // Advertising Services
    private ArrayList<BluetoothGattService> mAdvertisingServices;

    // Application context
    private Application mApplicationContext;

    // Recording service
    private BluetoothGattService mRecordService;

    // Recording characteristic
    private BluetoothGattCharacteristic mRecordServiceCharacteristic;

    /**
     * initialize the BLEManager class,  to be called once from the application class
     *
     * @param applicationContext The application context
     */
    public static void initialize(final Application applicationContext) {
        sSharedInstance = new BLEManager(applicationContext);
    }

    /**
     * returns the shared instance of BLEManager
     *
     * @return
     */
    public static BLEManager sharedInstance() {
        if (sSharedInstance != null) {
            return sSharedInstance;
        } else {
            throw new RuntimeException("BLEManager must be initialized before use");
        }
    }

    /**
     * Constructor, called privately through the initialize function
     *
     * @param applicationContext
     */
    private BLEManager(final Application applicationContext) {
        mApplicationContext = applicationContext;

        setupBluetooth();

    }

    /**
     * Sets up bluetooth devices and adapters
     */
    private void setupBluetooth() {
        mAdvertising = false;
        mBluetoothManager = (BluetoothManager) mApplicationContext.getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.setName(BLUETOOTH_ADAPTER_NAME);
            mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
            mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
            mAdvertisingServices = new ArrayList<BluetoothGattService>();
            mServiceUuids = new ArrayList<ParcelUuid>();

            addServicesToGattServer();
//            if (isBluetoothEnabled()) {
//                startAdvertising();
//            } else {
//                //TODO turn on bluetooth
//                Log.i(TAG, "Bluetooth not enabled");
//            }
        }
    }

    /**
     * @return true if bluetooth is enabled
     */
    public boolean isBluetoothEnabled() {
        return mBluetoothAdapter.isEnabled();
    }

    /**
     * Start the Gatt Server
     */
    private void startGattServer() {
        mGattServer = mBluetoothManager.openGattServer(mApplicationContext, mGattServerCallback);

        for(int i = 0; i < mAdvertisingServices.size(); i++) {
            mGattServer.addService(mAdvertisingServices.get(i));
        }
    }

    /**
     * Start advertising as bluetooth device
     */
    public void startAdvertising() {
        if (mAdvertising) {
            return;
        }

        startGattServer();

        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder();

        dataBuilder.setIncludeTxPowerLevel(false); //necessity to fit in 31 byte advertisement

        for (ParcelUuid eachServiceUuid : mServiceUuids) {
            dataBuilder.addServiceUuid(eachServiceUuid);
        }
        settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);
        settingsBuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);
        settingsBuilder.setConnectable(true);
        settingsBuilder.setTimeout(60000);

        mBluetoothLeAdvertiser.startAdvertising(settingsBuilder.build(), dataBuilder.build(), mAdvertiseCallback);
        mAdvertising = true;
    }

    /**
     * Stop advertising as a bluetooth device
     */
    public void stopAdvertising() {
        if (!mAdvertising) {
            return;
        }
        mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
        mGattServer.clearServices();
        mGattServer.close();
        mAdvertising = false;
    }

    /**
     * Add OnSite Services
     */
    private void addServicesToGattServer() {
        mRecordService = new BluetoothGattService(
            UUID.fromString(BLEOnsiteConstants.SERVICE_RECORD_CONTROL),
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        ); // What is Primary vs Secondary?


        mRecordServiceCharacteristic = new BluetoothGattCharacteristic(
            UUID.fromString(BLEOnsiteConstants.CHAR_RECORD),
            BluetoothGattCharacteristic.PROPERTY_READ |
            BluetoothGattCharacteristic.PROPERTY_WRITE |
            BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ |
            BluetoothGattCharacteristic.PERMISSION_WRITE
        );
        mRecordServiceCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);

        mRecordService.addCharacteristic(mRecordServiceCharacteristic);

        mAdvertisingServices.add(mRecordService);
        mServiceUuids.add(new ParcelUuid(mRecordService.getUuid()));
    }

    /**
     * Updates the recording characteristic
     *
     * @param recording
     */
    public void updateRecordingCharacteristic(boolean recording) {
        byte[] val = new byte[4];
        val[3] = recording ? (byte)0x01 : (byte)0x00;
        mRecordServiceCharacteristic.setValue(val);
        if (mLastConnectedDevice != null) {
            mGattServer.notifyCharacteristicChanged(mLastConnectedDevice, mRecordServiceCharacteristic, false);
        }
    }

    /**
     * Action when a characteristic is written to
     *
     * @param value
     */
    private void handleCharacteristicWrite(int value) {
        if (value == RECORDING_VALUE_STOP) {
            BusNotificationUtil.sharedInstance().postNotification(new BLEStopRecordingEvent());
        } else if (value == RECORDING_VALUE_START) {
            BusNotificationUtil.sharedInstance().postNotification(new BLEStartRecordingEvent());
        }
    }

    /**
     * Gatt Server callback, called when read or write
     */
    public BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            Log.d(TAG, "onConnectionStateChange status=" + status + "->" + newState);
            mLastConnectedDevice = device;
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            super.onServiceAdded(status, service);
            Log.i(TAG, "onServiceAdded");
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            Log.d(TAG, "onCharacteristicReadRequest requestId=" + requestId + " offset=" + offset);
            mLastConnectedDevice = device;

            if (characteristic.getUuid().equals(UUID.fromString(BLEOnsiteConstants.CHAR_RECORD))) {
                Log.d(TAG, "CHAR_RECORD UUID");
                mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset,
                        characteristic.getValue());
            }
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
            Log.d(TAG, "onCharacteristicWriteRequest requestId=" + requestId + " preparedWrite="
                    + Boolean.toString(preparedWrite) + " responseNeeded="
                    + Boolean.toString(responseNeeded) + " offset=" + offset);

            mLastConnectedDevice = device;
            if (value == null || value.length < 4) {
                Log.i(TAG, "Recieved Invalid BLE Data");
                return;
            }

            handleCharacteristicWrite((int)value[3]);

            if (responseNeeded) {
                mGattServer.sendResponse(device,
                    requestId,
                    BluetoothGatt.GATT_SUCCESS,
                    0,
                    value);
            }
        }
    };

    /**
     * Advertise Callback
     */
    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings advertiseSettings) {
            String successMsg = "Advertisement attempt successful";
            Log.d(TAG, successMsg);
            Log.d(TAG, advertiseSettings.toString());
        }

        @Override
        public void onStartFailure(int i) {
            String failMsg = "Advertisement attempt failed: " + i;
            Log.e(TAG, failMsg);
        }
    };



    // ******************************************************************
    //  Scanning functions
    // ******************************************************************
    private BluetoothLeScanner mBluetoothLeScanner; // TODO:TEMPHACK HERE FOR TESTING

    private BluetoothAdapter.LeScanCallback mLeScanCallback; // TODO:TEMPHACK FOR TESTING

    private ScanCallback mScanCallback;

    public Activity tmpActivity; // TODO:TEMPHACK FOR TESTING


    /**
     * Start scanning for BLE Advertisements (& set it up to stop after a set period of time).
     */
    public void startScanning(Activity activity) {
        tmpActivity = activity;

        if (Build.VERSION.SDK_INT >= 23) {
            if (tmpActivity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(tmpActivity,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        1);
            }
        }

        if (mScanCallback == null) {
            Log.d(TAG, "Starting Scanning");

            // Will stop the scanning after a set time.
            /*
            ThreadUtil.executeOnMainThreadDelayed(
                    new Runnable() {
                        @Override
                        public void run() {
                            stopScanning();
                        }
                    }, 10000);
                    */

            // Kick off a new scan.

            mScanCallback = new SampleScanCallback();
            mBluetoothLeScanner.startScan(null, buildScanSettings(), mScanCallback);

        } else {

        }
        /*
        if (mLeScanCallback == null) {

            mLeScanCallback = new SimpleLeScanCallback();
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        }*/
    }

    /**
     * Stop scanning for BLE Advertisements.
     */
    public void stopScanning() {
        Log.d(TAG, "Stopping Scanning");

        // Stop the scan, wipe the callback.
        mBluetoothLeScanner.stopScan(mScanCallback);
        mScanCallback = null;
    }

    /**
     * Return a List of {@link ScanFilter} objects to filter by Service UUID.
     */
    /*
    private List<ScanFilter> buildScanFilters() {
        List<ScanFilter> scanFilters = new ArrayList<>();

        ScanFilter.Builder builder = new ScanFilter.Builder();
        // Comment out the below line to see all BLE devices around you
        builder.setServiceUuid(Constants.Service_UUID);
        scanFilters.add(builder.build());

        return scanFilters;
    }*/

    /**
     * Return a {@link ScanSettings} object set to use low power (to preserve battery life).
     */
    private ScanSettings buildScanSettings() {
        ScanSettings.Builder builder = new ScanSettings.Builder();
//        builder.setScanMode(ScanSettings.SCAN_MODE_LOW_POWER);
        builder.setScanMode(ScanSettings.SCAN_MODE_BALANCED);
        return builder.build();
    }


    private class SimpleLeScanCallback implements BluetoothAdapter.LeScanCallback {
        /**
         * Callback reporting an LE device found during a device scan initiated
         * by the {@link BluetoothAdapter#startLeScan} function.
         *
         * @param device Identifies the remote device
         * @param rssi The RSSI value for the remote device as reported by the
         *             Bluetooth hardware. 0 if no RSSI value is available.
         * @param scanRecord The content of the advertisement record offered by
         *                   the remote device.
         */
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord)
        {
            boolean trypair = device.createBond();
            Log.i(TAG, "(PAIR_DEBUG) trypair result is " + (trypair ? "true" : "false"));
            if (trypair) {

                final BleAdvertisedData badata = BleUtil.parseAdertisedData(scanRecord);
                String deviceName = device.getName();
                if( deviceName == null ){
                    deviceName = badata.getName();
                }
                Log.i(TAG, "(PAIR_DEBUG), processed device name = " + deviceName);


            }
        }


    }


    /**
     * Custom ScanCallback object - adds to adapter on success, displays error on failure.
     */
    private class SampleScanCallback extends ScanCallback {

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);

            for (final ScanResult eachResult : results) {
                new AlertDialog.Builder(tmpActivity).setMessage(eachResult.getDevice().getName())
                        .setPositiveButton("Try Pair", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                boolean bondRes = eachResult.getDevice().createBond();
                            }
                        }).show();
            }
        }

        @Override
        public void onScanResult(int callbackType, final ScanResult result) {
            super.onScanResult(callbackType, result);

            ScanRecord record = result.getScanRecord();
            BluetoothDevice device = result.getDevice();
            Log.i(TAG, "(PAIR_DEBUG), found record, device is " + (device == null ? "null" : "not null"));
            Log.i(TAG, "(PAIR_DEBUG), found record,  " + (device != null && device.getName() != null ? device.getName() : "<null>"));

            ParcelUuid[] uuids = device != null ? device.getUuids() : null;
            if (uuids != null) {
                for (ParcelUuid eachUUID : uuids) {
                    Log.i(TAG, "(PAIR_DEBUG), found record uuid,  " + eachUUID.toString());
                }
            } else {
                Log.i(TAG, "(PAIR_DEBUG) device uuids are null");
            }

            if (record != null) {
                Log.i(TAG, "(PAIR_DEBUG), scan record device name = " + record.getDeviceName());
            } else {
                Log.i(TAG, "(PAIR_DEBUG), scan record device name = <null>");
            }

            final BleAdvertisedData badata = BleUtil.parseAdertisedData(record.getBytes());
            String deviceName = device.getName();
            if( deviceName == null ){
                deviceName = badata.getName();
            }
            Log.i(TAG, "(PAIR_DEBUG), processed device name = " + deviceName);

            new AlertDialog.Builder(tmpActivity).setMessage(result.getDevice().getName())
                    .setPositiveButton("Try Pair", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            boolean bondRes = result.getDevice().createBond();
                        }
                    }).show();
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);

            new AlertDialog.Builder(tmpActivity).setMessage("Scan Failed").show();
        }
    }

}


final class BleUtil {
    private final static String TAG=BleUtil.class.getSimpleName();
    public static BleAdvertisedData parseAdertisedData(byte[] advertisedData) {
        List<UUID> uuids = new ArrayList<UUID>();
        String name = null;
        if( advertisedData == null ){
            return new BleAdvertisedData(uuids, name);
        }

        ByteBuffer buffer = ByteBuffer.wrap(advertisedData).order(ByteOrder.LITTLE_ENDIAN);
        while (buffer.remaining() > 2) {
            byte length = buffer.get();
            if (length == 0) break;

            byte type = buffer.get();
            switch (type) {
                case 0x02: // Partial list of 16-bit UUIDs
                case 0x03: // Complete list of 16-bit UUIDs
                    while (length >= 2) {
                        uuids.add(UUID.fromString(String.format(
                                "%08x-0000-1000-8000-00805f9b34fb", buffer.getShort())));
                        length -= 2;
                    }
                    break;
                case 0x06: // Partial list of 128-bit UUIDs
                case 0x07: // Complete list of 128-bit UUIDs
                    while (length >= 16) {
                        long lsb = buffer.getLong();
                        long msb = buffer.getLong();
                        uuids.add(new UUID(msb, lsb));
                        length -= 16;
                    }
                    break;
                case 0x09:
                    byte[] nameBytes = new byte[length-1];
                    buffer.get(nameBytes);
                    try {
                        name = new String(nameBytes, "utf-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    break;
                default:
                    buffer.position(buffer.position() + length - 1);
                    break;
            }
        }
        return new BleAdvertisedData(uuids, name);
    }
}


class BleAdvertisedData {
    private List<UUID> mUuids;
    private String mName;
    public BleAdvertisedData(List<UUID> uuids, String name){
        mUuids = uuids;
        mName = name;
    }

    public List<UUID> getUuids(){
        return mUuids;
    }

    public String getName(){
        return mName;
    }
}