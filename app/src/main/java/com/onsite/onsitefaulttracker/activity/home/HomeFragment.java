package com.onsite.onsitefaulttracker.activity.home;

import android.Manifest;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.onsite.onsitefaulttracker.R;
import com.onsite.onsitefaulttracker.activity.BaseFragment;
//import com.onsite.onsitefaulttracker.connectivity.BLEManager;
import com.onsite.onsitefaulttracker.connectivity.BLTManager;
import com.onsite.onsitefaulttracker.connectivity.TcpConnection;
import com.onsite.onsitefaulttracker.model.Record;
import com.onsite.onsitefaulttracker.model.notifcation_events.UsbConnectedNotification;
import com.onsite.onsitefaulttracker.model.notifcation_events.UsbDisconnectedNotification;
import com.onsite.onsitefaulttracker.model.notifcation_events.BLTConnectedNotification;
import com.onsite.onsitefaulttracker.model.notifcation_events.BLTListeningNotification;
import com.onsite.onsitefaulttracker.model.notifcation_events.BLTNotConnectedNotification;
import com.onsite.onsitefaulttracker.model.notifcation_events.TCPStartRecordingEvent;
import com.onsite.onsitefaulttracker.model.notifcation_events.BLTStartRecordingEvent;
import com.onsite.onsitefaulttracker.model.notifcation_events.BLTStopRecordingEvent;
import com.onsite.onsitefaulttracker.util.BatteryUtil;
import com.onsite.onsitefaulttracker.util.BusNotificationUtil;
import com.onsite.onsitefaulttracker.util.GPSUtil;
import com.onsite.onsitefaulttracker.util.RecordUtil;
import com.onsite.onsitefaulttracker.util.SettingsUtil;
import com.onsite.onsitefaulttracker.util.ThreadUtil;
import com.squareup.otto.Subscribe;

import android.app.Activity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import static android.content.Context.SENSOR_SERVICE;
import static android.support.v4.content.ContextCompat.getSystemService;


/**
 * Created by hihi on 6/7/2016.
 *
 * Home Fragment is the default fragment for the Home Activity.
 * The Home screen is where the user can select to make a
 * new record,  view previous records or continue making a previous record.
 * The user can also access the settings screen from the settings button
 * in the action bar.
 */
public class HomeFragment extends BaseFragment {

    // The tag name for this fragment
    private static final String TAG = HomeFragment.class.getSimpleName();

    private Context mContext;

    // The display date format to display to the user
    private static final String DISPLAY_DATE_FORMAT = "dd MMM yyyy";

    // The current record name
    private TextView mCurrentRecordName;

    // The current record date
    private TextView mCurrentRecordDate;

    // The New Record Button
    private Button mNewRecordButton;

    // The Continue Last Record Button
    private Button mContinueRecordButton;

    // The submit button
    private Button mSubmitRecordButton;

    // The Previous Records button
    private Button mPreviousRecordsButton;

    // The connection status
    private TextView mConnectionStatusTextView;

    // Text View that displays the application version
    private TextView mAppVersion;

    // Listener for communicating with the parent activity
    private Listener mListener;

    // Tcp Connection runnable
    //private TcpConnection mTcpConnection; // TODO:TEMPHACK TEST

    private boolean mAdvertising = false;

    private boolean bluetooth = false;

    private String mCamera;
    private String mSerialNumber;

    // the request code for the camera permissions
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1;

    // the request code for the storage permissions
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 2;

    // the request code for the storage permissions
    private static final int READ_PHONE_STATE_REQUEST_CODE = 22;
    private final int PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 33;
    private final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 44;
    private int REQUEST_ENABLE_DISCOVERY = 4;
    private int DISCOVERY_REQUEST = 5;
    private static final int REQUEST_ENABLE_BT = 6;

    private final int BT_TIMEOUT = 1200; //seconds
    int PERMISSION_ALL = 9;

    String[] PERMISSIONS = {
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.READ_PHONE_STATE
    };
    /**
     * On create view, Override this in each extending fragment to implement initialization for that
     * fragment.
     *
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (view != null) {
            mNewRecordButton = view.findViewById(R.id.new_record_button);
            mNewRecordButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (GPSUtil.sharedInstance().getStatus()) {
                        onNewRecordClicked();
                    } else {
                        getFixStatus();
                    }
                }
            });

            mContinueRecordButton = view.findViewById(R.id.continue_record_button);
            mContinueRecordButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onContinueButtonClicked();
                }
            });

            mSubmitRecordButton = view.findViewById(R.id.submit_record_button);
            mSubmitRecordButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onSubmitButtonClicked();
                }
            });

            mPreviousRecordsButton = view.findViewById(R.id.previous_records_button);
            mPreviousRecordsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onPreviousRecordsClicked();
                }
            });

            mCurrentRecordName = view.findViewById(R.id.current_record_name);
            mCurrentRecordDate = view.findViewById(R.id.current_record_date);
            mConnectionStatusTextView = view.findViewById(R.id.connected_text_view);

            mAppVersion = view.findViewById(R.id.app_version_text_view);
            initAppVersionText();
            enableBluetooth();
            if(!hasPermissions(mContext, PERMISSIONS)){
                ActivityCompat.requestPermissions(getActivity(), PERMISSIONS, PERMISSION_ALL);
            }
            updateButtonStates();
        }
        return view;
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }
    /**
     * Action when the fragment gets attached to the parent activity, sets the listener
     * as the passed in context
     *
     * @param context
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Listener) {
            mListener = (Listener) context;
        }
        mContext = context;
        BusNotificationUtil.sharedInstance().getBus().register(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startGPS();
        Log.i(TAG, "HOME: CREATE");
    }

    /**
     * Action when the fragment is detached from the parent activity, nullifies the
     * listener as it is no longer valid
     */
    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        BusNotificationUtil.sharedInstance().getBus().unregister(this);
        Log.i(TAG, "HOME: DETACHED");
    }

    /**
     * Action when fragment is stopped,
     * updates the state of all the buttons
     */
    @Override
    public void onStop() {
        super.onStop();
        Log.i(TAG, "HOME: STOPPED");
        //TcpConnection.getSharedInstance().sendHomeWindowStatus("HOME: STOPPED");
        //updateButtonStates();
    }

    /**
     * Action when fragment has been destroyed,
     * updates the state of all the buttons
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "HOME: DESTROYED");
        //TcpConnection.getSharedInstance().sendHomeWindowStatus("HOME: DESTROYED");
        //updateButtonStates();
    }

    /**
     * Action when fragment is resumed,
     * updates the state of all the buttons
     */
    @Override
    public void onResume() {
        super.onResume();

        Log.i(TAG, "HOME: RESUMED");
        Log.i(TAG, "State = " + BLTManager.sharedInstance().getState());
        Log.i(TAG, "BT Enabled = " + BLTManager.sharedInstance().isBluetoothEnabled());
        Log.i(TAG, "Adverstising = " + mAdvertising);
        //TcpConnection.getSharedInstance().sendHomeWindowStatus("HOME: RESUMED");
        updateButtonStates();


    }


    /**
     * Action when fragment is paused,
     * updates the state of all the buttons
     */
    @Override
    public void onPause() {
        super.onPause();
        Log.i(TAG, "HOME: PAUSED");
        //TcpConnection.getSharedInstance().sendHomeWindowStatus("HOME PAUSED");
        updateButtonStates();
    }

    public HomeFragment getInstance() {
        return this;
    }

    /**
     * Start a tcp connection;
     */
//    private void runTcpConnection() {
//
//        TcpConnection.getSharedInstance().startTcpConnection();
//
//   }
    private void startBluetooth() {
        if (BLTManager.sharedInstance().getState() == BLTManager.STATE_NONE &&
                BLTManager.sharedInstance().isBluetoothEnabled()) {
            if (mAdvertising) {
                BLTManager.sharedInstance().start();
                Log.i(TAG, "Starting listen");
            }
        }

    }

    private void startGPS() {
        if ((getActivity().checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED)
                && (getActivity().checkSelfPermission
                (Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED)) {
            GPSUtil gps = new GPSUtil(mContext);
            Location location = gps.getLocation();
        } else {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    private void getSerial() {
        if (getActivity().checkSelfPermission(Manifest.permission.READ_PHONE_STATE)
                == PackageManager.PERMISSION_GRANTED) {
            mSerialNumber = Build.getSerial();
            Log.i(TAG, "DEVICE: " + mSerialNumber);
        } else {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.READ_PHONE_STATE},
                    READ_PHONE_STATE_REQUEST_CODE);
        }
    }

    private boolean requestPhonePermissions() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (getActivity().checkSelfPermission(Manifest.permission.READ_PHONE_STATE)
                    != PackageManager.PERMISSION_GRANTED) {
                if (getActivity().shouldShowRequestPermissionRationale(Manifest.permission.READ_PHONE_STATE)) {

                } else {
                    ActivityCompat.requestPermissions(getActivity(),
                            new String[]{Manifest.permission.READ_PHONE_STATE},
                            READ_PHONE_STATE_REQUEST_CODE);
                }
                return true;
            } else {
                String device = Build.getSerial();
                Log.i(TAG, "DEVICE: " + device);
                return true;
            }
        } else {
            return true;
        }
    }

    /**
     * Requests storage permissions if they are not already granted
     */
    private boolean requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (getActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        STORAGE_PERMISSION_REQUEST_CODE);
                return true;
            } else {
                return true;
            }
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // All good!
                    startGPS();
                    return;
                } else {
                    Log.i(TAG, "Need location permission");
                }
                return;
            case READ_PHONE_STATE_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // All good!
                    getSerial();
                    return;
                } else {
                    Log.i(TAG, "Need location permission");
                }
                return;
        }
    }

    /**
     * Creates an Intent to enable a bluetooth advert for 1000 seconds
     */
    //TODO Temp hack should be moved to BLTManager
    public void startAdvertising() {
        if (BLTManager.sharedInstance().isBluetoothEnabled()) {
            Intent discoverableIntent =
                    new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, BT_TIMEOUT);
            startActivityForResult(discoverableIntent, REQUEST_ENABLE_DISCOVERY);
        } else {
            Log.i(TAG, "Bluetooth Not Enabled");
        }
    }

    /**
     * Creates an Intent if bluetooth is not enables otherwise starts a bluetooth advert.
     */
    public void enableBluetooth() {
        //Bluetooth not enabled
        if (!BLTManager.sharedInstance().isBluetoothEnabled()) {
            Log.i(TAG, "Enabling bluetooth");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else {
            Log.i(TAG, "Bluetooth Enabled");
            Log.i(TAG, "Starting BT advertisement");
            startAdvertising();
        }
    }

    /**
     * Captures users selection results from pop-up message window
     * @param requestCode - the type of service requested
     * @param resultCode - the result from the user, usually ok/cancel or yes/no
     * @param data - a Intent carrying the result data
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "Request code " + requestCode);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                Log.i(TAG, "Starting BT advertisement");
                startAdvertising();
            } else {
                Log.i(TAG, "Bluetooth not enabled");
                BLTManager.sharedInstance().setState(BLTManager.STATE_NOTENABLED);

            }
        } else if (requestCode == REQUEST_ENABLE_DISCOVERY) {
            Log.i(TAG, "Result code " + resultCode);
            if (resultCode == BT_TIMEOUT) { //user selected OK
                Log.i(TAG, "Advertising accept");
                mAdvertising = true;
                startBluetooth();
            }
        }
    }
    /**
     * Update the state of the buttons
     */
    private void updateButtonStates() {
        Record currentRecord = RecordUtil.sharedInstance().getCurrentRecord();
        boolean hasCurrentRecord = currentRecord != null;
        boolean hasRecords = RecordUtil.sharedInstance().getCurrentRecordCount() > 0;
        boolean compressed = false;
        if (hasCurrentRecord) {
            compressed = currentRecord.fileCompressedCount > 0;
        }
        boolean result = (hasCurrentRecord && (!compressed));
        mContinueRecordButton.setEnabled(result);
        //mContinueRecordButton.setEnabled(hasCurrentRecord);
        mSubmitRecordButton.setEnabled(hasCurrentRecord);
        mPreviousRecordsButton.setEnabled(hasRecords);

        if (BLTManager.sharedInstance().getState() == 3) {
            mConnectionStatusTextView.setText(getString(R.string.BTconnected));
        } else if (BLTManager.sharedInstance().getState() == 2) {
            mConnectionStatusTextView.setText(getString(R.string.BTconnecting));
        } else if (BLTManager.sharedInstance().getState() == 1) {
            mConnectionStatusTextView.setText(getString(R.string.BTconnecting));
        } else if (BLTManager.sharedInstance().getState() == 9) {
            mConnectionStatusTextView.setText(getString(R.string.BTnotEnabled));
        } else {
            mConnectionStatusTextView.setText(getString(R.string.BTnotConnected));
        }
        updateCurrentRecordText();
    }

    /**
     * Updates the current record name text and current record date text view
     */
    private void updateCurrentRecordText() {
        Record currentRecord = RecordUtil.sharedInstance().getCurrentRecord();
        if (currentRecord == null) {
            mCurrentRecordName.setText(getString(R.string.no_current_record));
            mCurrentRecordDate.setText("");
        } else {
            Calendar now = Calendar.getInstance();
            Calendar recordCalendar = Calendar.getInstance();
            recordCalendar.setTime(currentRecord.creationDate);
            boolean isToday = now.get(Calendar.DAY_OF_YEAR) == recordCalendar.get(Calendar.DAY_OF_YEAR)
                    && now.get(Calendar.YEAR) == recordCalendar.get(Calendar.YEAR);
            boolean isYesterday = now.get(Calendar.DAY_OF_YEAR) - 1 == recordCalendar.get(Calendar.DAY_OF_YEAR)
                    && now.get(Calendar.YEAR) == recordCalendar.get(Calendar.YEAR);
            String prefixString = isToday ? "(Today) " :
                    isYesterday ? "(Yesterday)" : "";

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd MMMM yyyy, h:mm a");
            mCurrentRecordName.setText("Current record: " + currentRecord.recordName);
            mCurrentRecordDate.setText("Created on: " + prefixString + simpleDateFormat.format(currentRecord.creationDate));
        }
    }

//    /**
//     * Checks if Bluetooth is enabled, otherwise, requests permission to enable it
//     * Then enables Bluetooth if the user accepts
//     */
//    public void checkBluetoothEnabled() {
//        if (!BLEManager.sharedInstance().isBluetoothEnabled()) {
//            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
//        } else {
//            BLEManager.sharedInstance().startAdvertising();
//            // TODO:TEMPHACK BLEManager.sharedInstance().startScanning(getActivity());
//        }
//    }

    private void getFixStatus() {
        if (!GPSUtil.sharedInstance().getStatus()) {
            new AlertDialog.Builder(getActivity())
                    .setTitle("GPS Status")
                    .setMessage("The gps has not yet accquired a satellite fix. Do you wish to continue")
                    .setPositiveButton(getString(R.string.continue_anyway), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            onNewRecordClicked();
                        }
                    })
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show();
        }
    }

    /**
     * Action when the user clicks
     */
    private void onNewRecordClicked() {
        getSerial();
        if (!requestStoragePermission()) { //storage permission false
            //TcpConnection.getSharedInstance().sendMessage("M: No storage permission");
            BLTManager.sharedInstance().sendMessage("M: No storage permission");
            return;
        }
        //if (!TextUtils.isEmpty(SettingsUtil.sharedInstance().getCameraId())) {
        //requestPhonePermissions();
        //}
//        if (TcpConnection.getSharedInstance().isConnected()) {
//            checkForExistingRecords();
        if ((BLTManager.sharedInstance().getState() == 3)) {
            bluetooth = true;
            //checkForExistingRecords();
            if (!BatteryUtil.sharedInstance().isChargerConnected()) {
                BLTManager.sharedInstance().sendMessage("B: not connected");
                checkForExistingRecords();
            } else {
                checkForExistingRecords();
            }
        } else {
            checkForExistingRecords();
        }

    }

    /**
     * Action when user clicks on continue button, continue recording the current record
     */
    private void onContinueButtonClicked() {
//i        if (TcpConnection.getSharedInstance().isConnected()) {
////            if (!BatteryUtil.sharedInstance().isChargerConnected()) {
////                TcpConnection.getSharedInstance().sendMessage("B: Not charging");
////            }
////            if (mListener != null) {
////                mListener.onNewRecord();
////            }
////        } else i
        if (BLTManager.sharedInstance().getState() == 3) {
            if (!BatteryUtil.sharedInstance().isChargerConnected()) {
                BLTManager.sharedInstance().sendMessage("B: Not charging");
            }
            if (mListener != null) {
                mListener.onNewRecord();
            }
        } else {
            if (BatteryUtil.sharedInstance().isChargerConnected()) {
                if (mListener != null) {
                    mListener.onNewRecord();
                }
            } else {
                new AlertDialog.Builder(getActivity())
                        .setTitle(getString(R.string.charger_not_connected_title))
                        .setMessage(getString(R.string.charger_not_connected_message))
                        .setPositiveButton(getString(R.string.continue_anyway), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (mListener != null) {
                                    mListener.onNewRecord();
                                }
                            }
                        })
                        .setNegativeButton(getString(R.string.cancel), null)
                        .show();
            }
        }
    }
    /**
     * Action when the user clicks on the submit button
     */
    private void onSubmitButtonClicked() {
        final Record currentRecord = RecordUtil.sharedInstance().getCurrentRecord();
        if (mListener != null && currentRecord != null) {
            mListener.onSubmitRecord(currentRecord.recordId);
        }
    }

    /**
     * Check for existing records
     */
    private void checkForExistingRecords() {
//        if (TcpConnection.getSharedInstance().isConnected()) {
//            if (RecordUtil.sharedInstance().checkRecordExistsForToday()) {
//                if (mListener != null) {
//                    mListener.onNewRecord();
//                }
//            } else {
//                requestRecordName();
//            }
//        } else
        if (RecordUtil.sharedInstance().checkRecordExistsForToday()) {
            if (bluetooth) {
                if (mListener != null) {
                    updateButtonStates();
                    mListener.onNewRecord();
                }
            } else {
                updateButtonStates();
            }
        } else {
            requestRecordName();
        }
    }
    /**
     * Requests a name for the new record from the user then creates a new record
     * with that name
     */
    private void requestRecordName() {
//        if (TcpConnection.getSharedInstance().isConnected()) {
//            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
//            final String todaysDisplayDate = dateFormat.format(new Date());
//            String camera = SettingsUtil.sharedInstance().getCameraId();
//            System.out.println(camera + "_" + todaysDisplayDate);
//            createRecord(camera + "_" + todaysDisplayDate);
//            TcpConnection.getSharedInstance().sendMessage("Record created: " + camera + "_" + todaysDisplayDate);
//        } else
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        final String todaysDisplayDate = dateFormat.format(new Date());
        if (SettingsUtil.sharedInstance().getCameraId() == "") {

            mCamera = getCameraId();
            SettingsUtil.sharedInstance().setCameraId(mCamera);
        } else {
            mCamera = SettingsUtil.sharedInstance().getCameraId();
        }
        createRecord(mCamera + "_" + todaysDisplayDate);
        BLTManager.sharedInstance().sendMessage("Record created: " + mCamera + "_"
                + todaysDisplayDate);
        updateButtonStates();
        if (bluetooth) {
            mListener.onNewRecord();
        }
    }

    private String getCameraId() {
        switch (mSerialNumber) {
            case "ad07160328c52f53ed":
                return "C1";
            case "ce0416048828440503":
                return "C4";
            case "988627395552575855":
                return "C2";
            default:
                return "";
        }
    }

    /**
     * Show a dialog notifying the user that they must enter a name for the record
     */
    private void showNameMustBeEntered() {
        new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.new_record_please_enter_name_title))
                .setMessage(getString(R.string.new_record_please_enter_name_message))
                .setPositiveButton(getString(android.R.string.ok), null)
                .show();
    }

    /**
     * Creates a new record with the specified name
     *
     * @param recordName
     */
    private void createRecord(final String recordName) {
        if (RecordUtil.sharedInstance().createNewRecord(recordName)) {
            if (mListener != null) {
                updateButtonStates();
                //mListener.onNewRecord();
            }
        } else {
            new AlertDialog.Builder(getActivity())
                    .setTitle(getString(R.string.create_record_error_title))
                    .setMessage(String.format(getString(R.string.create_record_error_message), "Unknown"))
                    .setPositiveButton(getString(android.R.string.ok), null)
                    .show();
        }
    }

    /**
     * Action when a user clicks on the previous records button
     */
    private void onPreviousRecordsClicked() {
        if (mListener != null) {
            mListener.onPreviousRecords();
        }
    }

    /**
     * init the app version text box
     */
    private void initAppVersionText() {
        try {
            PackageInfo pInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
            String version = pInfo.versionName;
            mAppVersion.setText(String.format(getString(R.string.app_version), version));
        } catch (PackageManager.NameNotFoundException nex) {
            mAppVersion.setText("");
        }
    }

    // **********************************************************
    //  Notifications
    // **********************************************************
    /**
     * Notification when bluetooth is connected
     *
     * @param event
     */
    @Subscribe
    public void onBLTConnectedEvent(BLTConnectedNotification event) {
        // Set connection status text to connected
        mConnectionStatusTextView.setText(getString(R.string.BTconnected));
    }

    /**
     * Notification when bluetooth is listening
     *
     * @param event
     */
    @Subscribe
    public void onBLTListeningEvent(BLTListeningNotification event) {
        // Set connection status text to connected
        mConnectionStatusTextView.setText(getString(R.string.BTconnecting));
    }

    /**
     * Notification when bluetooth is not connected
     *
     * @param event
     */
    @Subscribe
    public void onBLTNotConnectedEvent(BLTNotConnectedNotification event) {
        // Set connection status text to connected
        mConnectionStatusTextView.setText(getString(R.string.BTnotConnected));
    }

    /**
     * Event from when user elects to pause recording
     *
     * @param event
     */
    @Subscribe
    public void onUsbConnectedEvent(UsbConnectedNotification event) {
        // Set connection status text to connected
        mConnectionStatusTextView.setText(getString(R.string.connected));
    }

    /**
     * Event from when user elects to resume recording
     *
     * @param event
     */
    @Subscribe
    public void onUsbDisconnectedEvent(UsbDisconnectedNotification event) {
        // Set connection status text to disconnected
        mConnectionStatusTextView.setText(getString(R.string.not_connected));
    }

    /**
     * Event from when Bluetooth controller elects to start recording
     *
     * @param event
     */
    @Subscribe
    public void onStartRecordingEvent(BLTStartRecordingEvent event) {
        if (mContinueRecordButton.isEnabled()) {
            onContinueButtonClicked();
        } else {
            onNewRecordClicked();
        }
    }

    /**
     * Event from when TCP controller elects to start recording
     *
     * @param event
     */
    @Subscribe
    public void onStartRecordingEvent(TCPStartRecordingEvent event) {
        if (mContinueRecordButton.isEnabled()) {
            onContinueButtonClicked();
        } else {
            onNewRecordClicked();
        }
    }

    /**
     * instantiate and return an instance of this fragment
     *
     * @return
     */
    public static HomeFragment createInstance() {
        return new HomeFragment();
    }

    /**
     * Returns the display title for this fragment
     *
     * @return
     */
    @Override
    protected String getDisplayTitle() {
        return getString(R.string.home_title);
    }

    /**
     * Returns the layout resource for this fragment
     *
     * @return
     */
    @Override
    protected int getLayoutResourceId() {
        return R.layout.fragment_home;
    }

    /**
     * Listener interface for the parent activity to implement to communicate with it
     */
    public interface Listener {
        void onNewRecord();
        void onPreviousRecords();
        void onSubmitRecord(final String recordId);
        void onOpenSettings();
    }
}

