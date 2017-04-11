package com.fermanis.volumebuddy;

import android.Manifest;
import android.app.DialogFragment;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class LocateMe extends AppCompatActivity implements GoogleMap.OnMyLocationButtonClickListener, OnMapReadyCallback, ActivityCompat.OnRequestPermissionsResultCallback, NewLocationDialog.NewLocationDialogListener {

    private GoogleMap mMap;
    private static final String TAG = "LocateMe";

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private boolean mPermissionDenied = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "Inside onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_locate_me);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setOnMyLocationButtonClickListener(this);
        loadLocations();
        enableMyLocation();
    }

    private void loadLocations() {
        // Load the Database
        LocationDbHelper locationDbHelper = new LocationDbHelper(getApplicationContext());
        SQLiteDatabase db = locationDbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery("select * from locations", null);

        if (cursor.moveToFirst()) {
            while(cursor.isAfterLast() == false) {
                double lat = cursor.getDouble(cursor.getColumnIndex(LocationContract.LocationEntry.COLUMN_NAME_LATITUDE));
                double longitude = cursor.getDouble(cursor.getColumnIndex(LocationContract.LocationEntry.COLUMN_NAME_LONGITUDE));
                String name = cursor.getString(cursor.getColumnIndex(LocationContract.LocationEntry.COLUMN_NAME_NAME));
                LatLng point = new LatLng(lat, longitude);
                // Add a new Marker
                mMap.addMarker(new MarkerOptions().position(point).title(name));
                cursor.moveToNext();
            }
        }
    }

    // Home location: -70.893576, 43.273181
    // Check for Permissions to ACCESS_FINE_LOCATION. If permission are not granted, ask for them. If they are granted, set LocationContract Enabled = true
    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE, Manifest.permission.ACCESS_FINE_LOCATION, true);
        } else if (mMap != null) {
            mMap.setMyLocationEnabled(true);

            mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                @Override
                public void onMapClick(LatLng point) {
                    NewLocationDialog newLocationDialog = new NewLocationDialog();
                    Bundle args = new Bundle();
                    args.putDouble("lat", point.latitude);
                    args.putDouble("long", point.longitude);
                    newLocationDialog.setArguments(args);
                    newLocationDialog.show(getFragmentManager(), new String().concat("NewLocationDialog"));
                }
            });
        }
    }

    // Callback from dialog, to handle new LocationContract Save
    @Override
    public void onDialogPositiveClick(DialogFragment dialog, LatLng point, Editable text) {
        // Add a new Marker
        mMap.addMarker(new MarkerOptions().position(point).title(text.toString()));
        Toast.makeText(getApplicationContext(), point.toString(), Toast.LENGTH_SHORT).show();

        // Create the Database
        LocationDbHelper locationDbHelper = new LocationDbHelper(getApplicationContext());
        SQLiteDatabase db = locationDbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(LocationContract.LocationEntry.COLUMN_NAME_LATITUDE, point.latitude);
        values.put(LocationContract.LocationEntry.COLUMN_NAME_LONGITUDE, point.longitude);
        values.put(LocationContract.LocationEntry.COLUMN_NAME_NAME, text.toString());

        long newRowId = db.insert(LocationContract.LocationEntry.TABLE_NAME, null, values);
    }

    // Handler for MyLocationButton - Will be replicated, and used to "save" location as office. Currently shows a toast for validation
    @Override
    public boolean onMyLocationButtonClick() {
        Toast.makeText(this, "MyLocation button clicked", Toast.LENGTH_SHORT).show();
        return false;
    }


    // Callback for Permission solicitation
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return;
        }

        if (PermissionUtils.isPermissionGranted(permissions, grantResults, Manifest.permission.ACCESS_FINE_LOCATION)) {
            enableMyLocation();
        } else {
            mPermissionDenied = true;
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        if (mPermissionDenied) {
            // Permission was not granted, display error dialog.
            showMissingPermissionError();
            mPermissionDenied = false;
        }
    }

    /**
     * Displays a dialog with error message explaining that the location permission is missing.
     */
    private void showMissingPermissionError() {
        PermissionUtils.PermissionDeniedDialog
                .newInstance(true).show(getSupportFragmentManager(), "dialog");
    }
}
