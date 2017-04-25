package com.fermanis.volumebuddy;

import android.Manifest;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

import static android.R.attr.name;

public class NewLocationActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    //TODO - Make the RADIUS a configurable value to the user
    int radius = 100; // 100 Meters

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    ArrayList<Geofence> mGeofenceList;
    PendingIntent mGeofencePendingIntent;
    protected static final String TAG = "LocationList";

    public static final String PACKAGE_NAME = "com.google.android.gms.location.Geofence";

    public static final String GEOFENCES_ADDED_KEY = PACKAGE_NAME + ".GEOFENCES_ADDED_KEY";

    /**
     * Provides the entry point to Google Play services.
     */
    protected GoogleApiClient mGoogleApiClient;

    /**
     * Used to keep track of whether geofences were added.
     */
    private boolean mGeofencesAdded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        final boolean isNew;
        double lat = 0.0, longitude = 0.0;
        String locationName = "";
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_location);
        mGeofenceList = new ArrayList<>();


        final String action = getIntent().getExtras().getString("ACTION");
        if (action != null && action.equalsIgnoreCase("NEW")) {
            isNew = true;
            lat = getIntent().getExtras().getDouble("LAT");
            longitude = getIntent().getExtras().getDouble("LONG");
        } else {
            isNew = false;
            locationName = getIntent().getExtras().getString("LOC_NAME");

            // Load the Database
            LocationDbHelper locationDbHelper = new LocationDbHelper(getApplicationContext());
            SQLiteDatabase db = locationDbHelper.getReadableDatabase();

            Cursor cursor = db.rawQuery("select * from locations where " + LocationContract.LocationEntry.COLUMN_NAME_NAME + "=?", new String[]{locationName});

            String nameText = null;
            int origAlarm = 0, origMedia = 0, origRinger = 0, origNotification = 0;

            if (cursor.moveToFirst()) {
                lat = cursor.getDouble(cursor.getColumnIndex(LocationContract.LocationEntry.COLUMN_NAME_LATITUDE));
                longitude = cursor.getDouble(cursor.getColumnIndex(LocationContract.LocationEntry.COLUMN_NAME_LONGITUDE));
                nameText = cursor.getString(cursor.getColumnIndex(LocationContract.LocationEntry.COLUMN_NAME_NAME));
                origAlarm = cursor.getInt(cursor.getColumnIndex(LocationContract.LocationEntry.COLUMN_NAME_ALARM_VOLUME));
                origMedia = cursor.getInt(cursor.getColumnIndex(LocationContract.LocationEntry.COLUMN_NAME_MEDIA_VOLUME));
                origRinger = cursor.getInt(cursor.getColumnIndex(LocationContract.LocationEntry.COLUMN_NAME_RINGER_VOLUME));
                origNotification = cursor.getInt(cursor.getColumnIndex(LocationContract.LocationEntry.COLUMN_NAME_NOTIFICATION_VOLUME));
            }

            EditText nameEditText = (EditText) findViewById(R.id.nameInput);
            nameEditText.setText(nameText);

            SeekBar alarmBar = (SeekBar) findViewById(R.id.alarmSeekBar);
            alarmBar.setProgress(origAlarm);

            SeekBar mediaBar = (SeekBar) findViewById(R.id.mediaSeekBar);
            mediaBar.setProgress(origMedia);

            SeekBar ringerBar = (SeekBar) findViewById(R.id.ringerSeekBar);
            ringerBar.setProgress(origRinger);

            SeekBar notifBar = (SeekBar) findViewById(R.id.notifSeekBar);
            notifBar.setProgress(origNotification);

        }
        final LatLng point = new LatLng(lat, longitude);

        TextView locationView = (TextView) findViewById(R.id.locationTextView);
        locationView.setText(point.toString());

        Button button = (Button) findViewById(R.id.saveLocation);
        final String finalLocationName = locationName;
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText nameEditText = (EditText) findViewById(R.id.nameInput);
                final String nameText = String.valueOf(nameEditText.getText());

                SeekBar alarmBar = (SeekBar) findViewById(R.id.alarmSeekBar);
                final int alarm = alarmBar.getProgress();

                SeekBar mediaBar = (SeekBar) findViewById(R.id.mediaSeekBar);
                final int media = mediaBar.getProgress();

                SeekBar ringerBar = (SeekBar) findViewById(R.id.ringerSeekBar);
                final int ringer = ringerBar.getProgress();

                SeekBar notifBar = (SeekBar) findViewById(R.id.notifSeekBar);
                final int notif = notifBar.getProgress();

                if (isNew) {
                    addLocation(point, nameText, alarm, media, ringer, notif);
                } else {
                    updateLocation(finalLocationName, nameText, alarm, media, ringer, notif);
                }

                Intent intent = new Intent();
                intent.setClass(getApplicationContext(), LocationList.class);
                startActivity(intent);

            }
        });

        Button deleteButton = (Button) findViewById(R.id.deleteButton);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteLocation(finalLocationName);
                Intent intent = new Intent();
                intent.setClass(getApplicationContext(), LocationList.class);
                startActivity(intent);
            }
        });

        buildGoogleApiClient();

    }

    /**
     * Builds a GoogleApiClient. Uses the {@code #addApi} method to request the LocationServices API.
     */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this,this)
                .addApi(LocationServices.API)
                .build();
    }


    private void updateLocation(String locationName, String nameText, int alarm, int media, int ringer, int notif) {
        // Create the Database
        LocationDbHelper locationDbHelper = new LocationDbHelper(getApplicationContext());
        SQLiteDatabase db = locationDbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(LocationContract.LocationEntry.COLUMN_NAME_NAME, nameText);
        values.put(LocationContract.LocationEntry.COLUMN_NAME_ALARM_VOLUME, alarm);
        values.put(LocationContract.LocationEntry.COLUMN_NAME_MEDIA_VOLUME, media);
        values.put(LocationContract.LocationEntry.COLUMN_NAME_RINGER_VOLUME, ringer);
        values.put(LocationContract.LocationEntry.COLUMN_NAME_NOTIFICATION_VOLUME, notif);

        db.update(LocationContract.LocationEntry.TABLE_NAME, values, LocationContract.LocationEntry.COLUMN_NAME_NAME + "=?", new String[]{locationName});
    }

    private void addLocation(LatLng point, String name, int alarm, int media, int ringer, int notif) {

        // Add the GeoFence to the List
        Geofence geofence = new Geofence.Builder()
                .setRequestId(name)
                .setCircularRegion(point.latitude, point.longitude, radius)
                .setExpirationDuration(10000000)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();
        mGeofenceList.add(geofence);


        // Create the Database
        LocationDbHelper locationDbHelper = new LocationDbHelper(getApplicationContext());
        SQLiteDatabase db = locationDbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(LocationContract.LocationEntry.COLUMN_NAME_LATITUDE, point.latitude);
        values.put(LocationContract.LocationEntry.COLUMN_NAME_LONGITUDE, point.longitude);
        values.put(LocationContract.LocationEntry.COLUMN_NAME_NAME, name);
        values.put(LocationContract.LocationEntry.COLUMN_NAME_ALARM_VOLUME, alarm);
        values.put(LocationContract.LocationEntry.COLUMN_NAME_MEDIA_VOLUME, media);
        values.put(LocationContract.LocationEntry.COLUMN_NAME_RINGER_VOLUME, ringer);
        values.put(LocationContract.LocationEntry.COLUMN_NAME_NOTIFICATION_VOLUME, notif);
        values.put(LocationContract.LocationEntry.COLUMN_NAME_GEOFENCE_ID, geofence.getRequestId());

        db.insert(LocationContract.LocationEntry.TABLE_NAME, null, values);

        Intent intent = new Intent(getApplicationContext(), NewLocationActivity.class);
        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingResult<Status> result;
        GeofencingRequest geofencingRequest = getGeofencingRequest();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            result = LocationServices.GeofencingApi.addGeofences(mGoogleApiClient, geofencingRequest, pendingIntent);
            result.setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(Status status) {
                    if (status.isSuccess()) {
                        Log.i(TAG, "Callback Saved!");
                        mGeofencesAdded = true;

                        Toast.makeText(
                                getApplicationContext(),
                                getString(R.string.geofences_added),
                                Toast.LENGTH_SHORT
                        ).show();
                    } else {
                        Log.e(TAG, "Registering geofence failed: " + status.getStatusMessage() + " : " + status.getStatusCode());
                    }
                }
            });

        }



    }

    private void deleteLocation(String locationName) {
        LocationDbHelper locationDbHelper = new LocationDbHelper(getApplicationContext());
        SQLiteDatabase db = locationDbHelper.getWritableDatabase();
        db.delete(LocationContract.LocationEntry.TABLE_NAME, LocationContract.LocationEntry.COLUMN_NAME_NAME + "=?", new String[]{locationName});
    }


    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(mGeofenceList);
        return builder.build();
    }



    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    /**
     * Runs when a GoogleApiClient object successfully connects.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "Connected to GoogleApiClient");
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason.
        Log.i(TAG, "Connection suspended");

        // onConnected() will be called again automatically when the service reconnects
    }

}
