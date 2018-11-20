package com.onsite.onsitefaulttracker.util;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.location.Criteria;
import android.os.Bundle;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.location.Location;
import android.location.LocationListener;
import android.content.DialogInterface;

import java.lang.String;

import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.app.AlertDialog;
import android.provider.Settings;
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
    private double latitude; // latitude
    private double longitude; // longitude

    // The minimum distance to change Updates in meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 1; // 10 meters

    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 1000; // 1 minute

    public static final int PERMISSIONS_REQUEST_LOCATION = 10;

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

    /**
     * The constructor for RecordUtil, called internally
     *
     * @param context
     */
    private GPSUtil(final Context context) {
        mContext = context;
        checkGPS();
    }

    public void checkGPS() {
        mLocationManager = (LocationManager)
                mContext.getSystemService(Context.LOCATION_SERVICE);
        // getting GPS status
        isGPSEnabled = mLocationManager
                .isProviderEnabled(LocationManager.GPS_PROVIDER);

        Log.v("isGPSEnabled", "=" + isGPSEnabled);

        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setPowerRequirement(Criteria.POWER_HIGH);
        criteria.setAltitudeRequired(false);
        criteria.setSpeedRequired(false);
        //criteria.setCostAllowed(true);
        criteria.setBearingRequired(false);

//API level 9 and up
        criteria.setHorizontalAccuracy(Criteria.ACCURACY_HIGH);
        criteria.setVerticalAccuracy(Criteria.ACCURACY_HIGH);
//        if (isGPSEnabled) {
//            return true;
//        } else {
//            return false;
//        }
            //mLocation = getLocation();
            //showSettingsAlert();
        }

    public Location getLocation() {
        Location location = null;
        if (isGPSEnabled) {
            if (ActivityCompat.checkSelfPermission(mContext,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(mContext,
                            Manifest.permission.ACCESS_COARSE_LOCATION) ==
                            PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                mLocationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES, this);

                Log.d(TAG, "GPS Enabled");
                if (mLocationManager != null) {
                    location = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (location != null) {
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                        Float accuracy = location.getAccuracy();
                        Log.d("Latitude", Double.toString(latitude));
                        Log.d("Longitude", Double.toString(longitude));
                        Log.d("Accuracy", Double.toString(accuracy));
                    } else {
                        Log.d(TAG, "Location Null");
                    }
                } else {
                    Log.d(TAG, "Location Manager Null");
                }

            } else {
                Log.d(TAG, "Permissions not granted");

            }
        }
        return location;
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
                //location = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
//                if (location != null) {
//                    latitude = location.getLatitude();
//                    longitude = location.getLongitude();
//                    Log.d("Latitude", Double.toString(latitude));
//                    Log.d("Longitude", Double.toString(longitude));
//                } else {
//                    Log.d(TAG, "Location Null");
//                }
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
