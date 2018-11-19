package com.onsite.onsitefaulttracker.util;

import android.Manifest;
import android.content.Context;
import android.os.Bundle;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.location.Location;
import android.location.LocationListener;
import android.content.DialogInterface;

import java.lang.String;

import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.app.AlertDialog;
import android.provider.Settings;
import com.onsite.onsitefaulttracker.R;


public class GPSUtil implements LocationListener {

    // The tag name for this utility class
    private static final String TAG = GPSUtil.class.getSimpleName();

    // The application context
    private Context mContext;

    // The static instance of this class which will be initialized once then reused
    // throughout the app
    private static GPSUtil sGPSUtil;

    // flag for GPS status
    public boolean isGPSEnabled = false;

    private LocationManager mLocationManager;
    private Location mLocation;
    private double latitude; // latitude
    private double longitude; // longitude

    // The minimum distance to change Updates in meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 1; // 10 meters

    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 1; // 1 minute

    public static final int PERMISSIONS_REQUEST_LOCATION = 10;

    /**
     * initializes RecordUtil.
     *
     * @param context
     */
    public static void initialize(final Context context) {

        sGPSUtil = new GPSUtil(context);
    }

    /**
     * return the shared instance of Record Util
     *
     * @return
     */
    public static GPSUtil sharedInstance() {
        if (sGPSUtil != null) {
            return sGPSUtil;
        } else {
            throw new RuntimeException("RecordUtil must be initialized in the Application class before use");
        }
    }

    /**
     * The constructor for RecordUtil, called internally
     *
     * @param context
     */
    private GPSUtil(final Context context) {
        mContext = context;
        checkPermissions();
    }

    private void checkPermissions() {
        LocationManager locationManager = (LocationManager)
                mContext.getSystemService(Context.LOCATION_SERVICE);
        // getting GPS status
        isGPSEnabled = locationManager
                .isProviderEnabled(LocationManager.GPS_PROVIDER);

        Log.v("isGPSEnabled", "=" + isGPSEnabled);

        if (isGPSEnabled) {
            mLocation = getLocation();
            //showSettingsAlert();
        }
    }


    private Location getLocation() {
        Location location = null;
        if (location == null) {
            if (ActivityCompat.checkSelfPermission(mContext,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(mContext,
                            Manifest.permission.ACCESS_COARSE_LOCATION) !=
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
                        Log.d("Latitude", Double.toString(latitude));
                        Log.d("Longitude", Double.toString(longitude));
                    }

                }
//            } else {
//                ActivityCompat.requestPermissions(Context,
//                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
//                        PERMISSIONS_REQUEST_LOCATION);
//            }

            }
        }
        return location;
    }

    public void showSettingsAlert() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
        dialog.setMessage(mContext.getResources().getString(R.string.gps_network_not_enabled));
        dialog.setPositiveButton(mContext.getResources().getString(R.string.open_location_settings), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                // TODO Auto-generated method stub
                Intent myIntent = new Intent( Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                mContext.startActivity(myIntent);
                //get gps
            }
        });
        dialog.setNegativeButton(mContext.getString(R.string.cancel), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                // TODO Auto-generated method stub

            }
        });
        dialog.show();
        mLocation = getLocation();
    }

    @Override
    public void onLocationChanged(Location location) {
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
