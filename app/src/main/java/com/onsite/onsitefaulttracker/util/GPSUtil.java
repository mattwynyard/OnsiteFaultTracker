package com.onsite.onsitefaulttracker.util;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.location.Criteria;
import android.location.GnssStatus;
import android.location.GpsSatellite;
import android.location.OnNmeaMessageListener;
import android.os.Build;
import android.os.Bundle;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.location.Location;
import android.location.LocationListener;
import android.content.DialogInterface;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.String;

import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.app.AlertDialog;
import android.provider.Settings;
import android.widget.Toast;

import com.onsite.onsitefaulttracker.R;
import com.onsite.onsitefaulttracker.connectivity.BLTManager;


public class GPSUtil implements LocationListener {

    // The tag name for this utility class
    private static final String TAG = GPSUtil.class.getSimpleName();

    // The application context
    private Context mContext;

    //    // The static instance of this class which will be initialized once then reused
//    // throughout the app
//    private static GPSUtil sGPSUtil;
    // Shared Instance, to be initialized once and used throughout the application
    private static GPSUtil sSharedInstance;

    // flag for GPS status
    public boolean isGPSEnabled = false;

    private LocationManager mLocationManager;
    private Location mLocation;
    //private LocationListener mLocationListener;
    private double latitude; // latitude
    private double longitude; // longitude

    private OnNmeaMessageListener mNmeaListener;

    // The minimum distance to change Updates in meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 1; // 1 meters

    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 1000; // 1 sec

    public static final int PERMISSIONS_REQUEST_LOCATION = 10;

    private boolean mFix;
    private int mSatellites;


    /**
     * initializes GPSUtil.
     *
     * @param context The application context
     */
    public static void initialize(final Context context) {
        sSharedInstance = new GPSUtil(context);
    }

    /**
     * return the shared instance of Record Util
     *
     * @return
     */
    public static GPSUtil sharedInstance() {
        if (sSharedInstance != null) {
            return sSharedInstance;
        } else {
            throw new RuntimeException("GPSUtil must be initialized " +
                    "in the Application class before use");
        }
    }

    public void addNmeaListener() {
        mNmeaListener = new OnNmeaMessageListener() {
            @Override
            public void onNmeaMessage(String message, long timestamp) {
                Log.v("NMEA String: ", "= " + message);
            }
        };
    }

    LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            //Log.v("Listener: ", "Location changed");
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            mLocation = location;
            //String msg = "New Latitude: " + latitude + "New Longitude: " + longitude;
            //Toast.makeText(getBaseContext(),msg,Toast.LENGTH_LONG).show();
        }


        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    /**
     * The constructor for GPSUtil, called internally
     *
     * @param context
     */
    public GPSUtil(Context context) {
        mContext = context;
        mLocationManager = (LocationManager)
                mContext.getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this.mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            //return;
        }
        //mLocationManager.addNmeaListener(mNmeaListener);
        checkGPS();
    }

    public void checkGPS() {
        // getting GPS status
        isGPSEnabled = mLocationManager
                .isProviderEnabled(LocationManager.GPS_PROVIDER);

        Log.v("isGPSEnabled", "= " + isGPSEnabled);

        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setPowerRequirement(Criteria.POWER_HIGH);
        criteria.setAltitudeRequired(true);
        criteria.setSpeedRequired(false);
        //criteria.setCostAllowed(true);

        criteria.setBearingRequired(true);
        GnssStatus.Callback gnssStatusCallBack = new GnssStatus.Callback() {
            @Override
            public void onSatelliteStatusChanged(GnssStatus status) {

                int satelliteCount = status.getSatelliteCount();
                Log.d(TAG, "Satellites: " + satelliteCount);
                mSatellites = 0;
                for (int i = 0; i < satelliteCount; i++) {
                    if (status.usedInFix(i)) {
                        mSatellites++;
                    }
                }
                //Log.d(TAG, "Satellites used in fix: " + mSatellites);
            }

            @Override
            public void onFirstFix(int ttffMillis) {
                super.onFirstFix(ttffMillis);
                Log.d(TAG, "First fix: " + String.valueOf(ttffMillis));
                mFix = true;
                Toast.makeText(mContext, "Succesfull satellite fix!",
                        Toast.LENGTH_LONG).show();
            }

            @Override
            public void onStarted() {
                super.onStarted();
                Log.d(TAG, "GPS_EVENT_STARTED...");
                Toast.makeText(mContext, "Acquiring satellite fix...",
                        Toast.LENGTH_LONG).show();

            }

            @Override
            public void onStopped() {
                super.onStopped();
                Log.d(TAG, "GPS_EVENT_STOPPED...");

            }
        };
        if (ActivityCompat.checkSelfPermission(this.mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mLocationManager.registerGnssStatusCallback(gnssStatusCallBack);
    }

    public boolean getStatus() {
        return mFix;
    }

    public Location getLocation() {
        //Location location = null;
        if (isGPSEnabled) {
            if (ActivityCompat.checkSelfPermission(mContext,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(mContext,
                            Manifest.permission.ACCESS_COARSE_LOCATION) ==
                            PackageManager.PERMISSION_GRANTED) {
                mLocationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES, mLocationListener);

                Log.d(TAG, "GPS Enabled");
                if (mLocationManager != null) {
                    Log.d(TAG, "GPS Provider enabled: " + mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER));
                    mLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                    if (mLocation != null) {
                        latitude = mLocation.getLatitude();
                        longitude = mLocation.getLongitude();
                        Float accuracy = mLocation.getAccuracy();
//                        Log.d("Latitude", Double.toString(latitude));
//                        Log.d("Longitude", Double.toString(longitude));
//                        Log.d("Altitude", Double.toString(mLocation.getAltitude()));
//                        Log.d("Accuracy", Double.toString(accuracy));
                    } else {
                        Log.d(TAG, "Location Null");
                        mLocation = new Location(LocationManager.GPS_PROVIDER);
                    }
                } else {
                    Log.d(TAG, "Location Manager Null");
                }

            } else {
                Log.d(TAG, "Permissions not granted");
            }
        }
        //TODO fix to handle null location
        //will be null if Location manager null or permission not granted
        return mLocation;
    }

    private boolean checkPermssion() {
        return ActivityCompat.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(mContext,
                        Manifest.permission.ACCESS_COARSE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onLocationChanged(Location location) {
        if (ActivityCompat.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(mContext,
                        Manifest.permission.ACCESS_COARSE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED) {
            if (mLocationManager != null) {
                mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            } else {
                Log.d(TAG, "Location Manager Null");
            }
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }


}
