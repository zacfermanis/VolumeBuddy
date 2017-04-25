package com.fermanis.volumebuddy;

import android.app.ActionBar;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.LocationListener;
import android.media.AudioManager;
import android.os.Build;
import android.provider.SyncStateContract;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.List;

public class LocationList extends FragmentActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback<Status> {

    ListView listView;
    ArrayList<Geofence> mGeofenceList;
    PendingIntent mGeofencePendingIntent;
    protected static final String TAG = "LocationList";

    /**
     * Used to keep track of whether geofences were added.
     */
    private boolean mGeofencesAdded;

    /**
     * Used to persist application state about whether geofences were added.
     */
    private SharedPreferences mSharedPreferences;

    public static final String PACKAGE_NAME = "com.google.android.gms.location.Geofence";

    public static final String GEOFENCES_ADDED_KEY = PACKAGE_NAME + ".GEOFENCES_ADDED_KEY";

    /**
     * Provides the entry point to Google Play services.
     */
    protected GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ImageView imageView = new ImageView(this);
        imageView.setImageResource(R.drawable.ic_action_name);
        imageView.setAdjustViewBounds(true);
        imageView.setLayoutParams(new ActionBar.LayoutParams(
                ActionBar.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.addView(imageView);

        setContentView(linearLayout);
        setContentView(R.layout.activity_location_list);
        listView = (ListView) findViewById(R.id.list);
        mGeofenceList = new ArrayList<>();

        // Create the Database
        LocationDbHelper locationDbHelper = new LocationDbHelper(getApplicationContext());

        SQLiteDatabase db = locationDbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("select * from locations", null);
        List<String> names = new ArrayList<>();

        // Configure the list, and [most importantly] configure the GeoFences
        if (cursor.moveToFirst()) {
            while(cursor.isAfterLast() == false) {
                String name = cursor.getString(cursor.getColumnIndex(LocationContract.LocationEntry.COLUMN_NAME_NAME));
                names.add(name);
                double lat = cursor.getDouble(cursor.getColumnIndex(LocationContract.LocationEntry.COLUMN_NAME_LATITUDE));
                double longitude = cursor.getDouble(cursor.getColumnIndex(LocationContract.LocationEntry.COLUMN_NAME_LONGITUDE));
                //TODO - Make the RADIUS a configurable value to the user
                int radius = 100; // 100 Meters
                mGeofenceList.add(new Geofence.Builder()
                        .setRequestId(name)
                        .setCircularRegion(lat, longitude, radius)
                        .setExpirationDuration(10000000)
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                        .build());

                cursor.moveToNext();
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, names);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // ListView Clicked item index
                int itemPosition     = position;

                // ListView Clicked item value
                String  itemValue    = (String) listView.getItemAtPosition(position);

                Intent intent = new Intent();
                intent.setClass(getApplicationContext(), NewLocationActivity.class);
                intent.putExtra("ACTION", "UPDATE");
                intent.putExtra("LOC_NAME", itemValue);
                startActivity(intent);

                // Show Alert
                Toast.makeText(getApplicationContext(),
                        "Position :"+itemPosition+"  ListItem : " +itemValue , Toast.LENGTH_LONG)
                        .show();
            }
        });


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.floatingActionButton);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setClass(getApplicationContext(), LocateMe.class);
                startActivity(intent);
            }
        });

        FloatingActionButton simulate = (FloatingActionButton) findViewById(R.id.floatingActionButton2);
        simulate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GeofenceTransitionsIntentService geofenceTransitionsIntentService = new GeofenceTransitionsIntentService();
                // Create the Database
                LocationDbHelper locationDbHelper = new LocationDbHelper(getApplicationContext());

                SQLiteDatabase db = locationDbHelper.getReadableDatabase();
                Cursor cursor = db.rawQuery("select * from locations", null);
                if (cursor.moveToFirst()) {
                    String geofenceId =  cursor.getString(cursor.getColumnIndex(LocationContract.LocationEntry.COLUMN_NAME_GEOFENCE_ID));
                    updateVolumes(geofenceId);
                }
            }
        });

        // Set up Permissions for Do Not Disturb access
        NotificationManager notificationManager =
                (NotificationManager) this.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && !notificationManager.isNotificationPolicyAccessGranted()) {

            Intent intent = new Intent(
                    android.provider.Settings
                            .ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);

            startActivity(intent);
        }


        // Register the LocationManager
        //LocationManagerUtils locationManagerUtils = LocationManagerUtils.getInstance(this);
        buildGoogleApiClient();
    }

    public void updateVolumes(String geofenceId) {
        // Create the Database
        LocationDbHelper locationDbHelper = new LocationDbHelper(this.getBaseContext());

        SQLiteDatabase db = locationDbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("select * from locations where geofence = ? ", new String[] {geofenceId});
        // Configure the list, and [most importantly] configure the GeoFences
        if (cursor.moveToFirst()) {
            // Get the Volumes
            int alarm = cursor.getInt(cursor.getColumnIndex(LocationContract.LocationEntry.COLUMN_NAME_ALARM_VOLUME));
            int media = cursor.getInt(cursor.getColumnIndex(LocationContract.LocationEntry.COLUMN_NAME_MEDIA_VOLUME));
            int notification = cursor.getInt(cursor.getColumnIndex(LocationContract.LocationEntry.COLUMN_NAME_NOTIFICATION_VOLUME));
            int ringer = cursor.getInt(cursor.getColumnIndex(LocationContract.LocationEntry.COLUMN_NAME_RINGER_VOLUME));
            String name = cursor.getString(cursor.getColumnIndex(LocationContract.LocationEntry.COLUMN_NAME_NAME));
            // Get the Max Volumes
            AudioManager audioManager = (AudioManager)getApplicationContext().getSystemService(this.getBaseContext().AUDIO_SERVICE);
            int alarmMaxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
            int mediaMaxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
            int notificationMaxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
            int ringerMaxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING);

            Log.i(TAG,"******************* Setting Volumes! ****************************");
            Double alarmVolume = alarmMaxVolume * (alarm / 100.0);
            Double mediaVolume = mediaMaxVolume * (media / 100.0);
            Double notificationVolume = notificationMaxVolume * (notification / 100.0);
            Double ringerVolume = ringerMaxVolume * (ringer / 100.0);

            audioManager.setStreamVolume(AudioManager.STREAM_ALARM,alarmVolume.intValue(),alarmVolume.intValue());
            audioManager.setStreamVolume(AudioManager.STREAM_RING,ringerVolume.intValue(),ringerVolume.intValue());
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,mediaVolume.intValue(),mediaVolume.intValue());
            audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION,notificationVolume.intValue(),notificationVolume.intValue());
            Log.i(TAG,"******************* Volumes Set! *********************************");

            String direction = "into ";
            int geofenceTransition = Geofence.GEOFENCE_TRANSITION_ENTER;
            if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
                direction = "out of ";
            }
            sendNotification(name, direction);

        }

    }

    private void sendNotification(String name, String direction) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            Notification geofenceNotification = new Notification.Builder(this)
                    .setContentTitle("Geofence Crossed")
                    .setContentText("You just crossed " + direction + " " + name)
                    .setSmallIcon(R.drawable.band)
                    .build();
            final NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(1, geofenceNotification);

        }
    }


    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(mGeofenceList);
        return builder.build();
    }


    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
        // calling addGeofences() and removeGeofences().
        return PendingIntent.getService(this, 0, intent, PendingIntent.
                FLAG_UPDATE_CURRENT);
    }

    /**
     * Builds a GoogleApiClient. Uses the {@code #addApi} method to request the LocationServices API.
     */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
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

    /**
     * Runs when the result of calling addGeofences() and removeGeofences() becomes available.
     * Either method can complete successfully or with an error.
     *
     * Since this activity implements the {@link ResultCallback} interface, we are required to
     * define this method.
     *
     * @param status The Status returned through a PendingIntent when addGeofences() or
     *               removeGeofences() get called.
     */
    public void onResult(Status status) {
        if (status.isSuccess()) {
            // Update state and save in shared preferences.
            mGeofencesAdded = !mGeofencesAdded;
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.putBoolean(GEOFENCES_ADDED_KEY, mGeofencesAdded);
            editor.apply();

            Toast.makeText(
                    this,
                    getString(mGeofencesAdded ? R.string.geofences_added :
                            R.string.geofences_removed),
                    Toast.LENGTH_SHORT
            ).show();
        } else {
            // Get the status code for the error and log it using a user-friendly message.
            String errorMessage = GeofenceErrorMessages.getErrorString(this,
                    status.getStatusCode());
            Log.e(TAG, errorMessage);
        }
    }

}
