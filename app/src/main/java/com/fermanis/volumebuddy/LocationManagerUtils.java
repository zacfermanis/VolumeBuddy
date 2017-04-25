package com.fermanis.volumebuddy;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by zacfe on 4/19/2017.
 */

public class LocationManagerUtils implements ActivityCompat.OnRequestPermissionsResultCallback{

    private static LocationManagerUtils locationManagerUtils;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    public static LocationManagerUtils getInstance(Activity activity) {
        if (locationManagerUtils != null) {
            return locationManagerUtils;
        } else {
            return new LocationManagerUtils(activity);
        }
    }

    public LocationManagerUtils(Activity activity) {
        LocationManager locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        configureListener(activity, locationManager);
    }

    public static void configureListener(final Activity activity, LocationManager locationManager) {
        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                checkForMatchAndAdjustVolume(activity, location);
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


        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        }
    }

    //TODO - I'm sure theres a better way to optimize this. Implement some sort of cacheing so we don't have to load the DB every location change
    private static void checkForMatchAndAdjustVolume(Activity activity, Location location) {
        // Load the Database
        LocationDbHelper locationDbHelper = new LocationDbHelper(activity.getApplicationContext());
        SQLiteDatabase db = locationDbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("select * from locations", null);

        if (cursor.moveToFirst()) {
            while (cursor.isAfterLast() == false) {
                double lat = cursor.getDouble(cursor.getColumnIndex(LocationContract.LocationEntry.COLUMN_NAME_LATITUDE));
                double longitude = cursor.getDouble(cursor.getColumnIndex(LocationContract.LocationEntry.COLUMN_NAME_LONGITUDE));
                String name = cursor.getString(cursor.getColumnIndex(LocationContract.LocationEntry.COLUMN_NAME_NAME));
                LatLng point = new LatLng(lat, longitude);
                //TODO - Make the RADIUS a configurable value to the user
                int radius = 100; // 100 Meters
                if (locationIsWithinRange(location, lat, longitude, radius)) {
                    Toast.makeText(activity, "Within Range: " + name, Toast.LENGTH_SHORT).show();
                    //TODO - SET THE VOLUMES!!!
                }
            }
        }
    }

    private static boolean locationIsWithinRange(Location location, double lat, double longitude, int radius) {
        float[] results = new float[1];
        Location.distanceBetween(location.getLatitude(), location.getLongitude(), lat, longitude, results);
        if (results[0] <= radius) {
            return true;
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return;
        }
        if (PermissionUtils.isPermissionGranted(permissions, grantResults, Manifest.permission.ACCESS_FINE_LOCATION)) {
           configureListener(new Activity(), (LocationManager) VolumeBuddy.getAppContext().getSystemService(Context.LOCATION_SERVICE));
        }
    }

}
