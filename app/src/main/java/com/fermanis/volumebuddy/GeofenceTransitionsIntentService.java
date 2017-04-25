package com.fermanis.volumebuddy;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.AudioManager;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class GeofenceTransitionsIntentService extends IntentService {

    protected static final String TAG = "GeofenceTransitionsIS";

    /**
     * This constructor is required, and calls the super IntentService(String)
     * constructor with the name for a worker thread.
     */
    public GeofenceTransitionsIntentService() {
        // Use the TAG to name the worker thread.
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    /**
     * Handles incoming intents.
     * @param intent sent by Location Services. This Intent is provided to Location
     *               Services (inside a PendingIntent) when addGeofences() is called.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            String errorMessage = GeofenceErrorMessages.getErrorString(this,
                    geofencingEvent.getErrorCode());
            Log.e(TAG, errorMessage);
            return;
        }

        // Get the transition type.
        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
                geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {

            // Get the geofences that were triggered. A single event can trigger multiple geofences.
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

            // Get the transition details as a String.
            String geofenceTransitionDetails = getGeofenceTransitionDetails(
                    this,
                    geofenceTransition,
                    triggeringGeofences
            );

            // Update the Volumes based on the GeoFences
            // TODO - Handle collisions better - for now, just taking the first one
            if (triggeringGeofences.size() > 1) {
                updateVolumes(triggeringGeofences.get(0).getRequestId(), geofenceTransition);
            }
            Log.i(TAG, geofenceTransitionDetails);
        } else {
            // Log the error.
            Log.e(TAG, getString(R.string.geofence_transition_invalid_type, geofenceTransition));
        }
    }

    /**
     * Gets transition details and returns them as a formatted string.
     *
     * @param context               The app context.
     * @param geofenceTransition    The ID of the geofence transition.
     * @param triggeringGeofences   The geofence(s) triggered.
     * @return                      The transition details formatted as String.
     */
    private String getGeofenceTransitionDetails(
            Context context,
            int geofenceTransition,
            List<Geofence> triggeringGeofences) {

        String geofenceTransitionString = getTransitionString(geofenceTransition);

        // Get the Ids of each geofence that was triggered.
        ArrayList triggeringGeofencesIdsList = new ArrayList();
        for (Geofence geofence : triggeringGeofences) {
            triggeringGeofencesIdsList.add(geofence.getRequestId());
        }
        String triggeringGeofencesIdsString = TextUtils.join(", ",  triggeringGeofencesIdsList);

        return geofenceTransitionString + ": " + triggeringGeofencesIdsString;
    }

    public void updateVolumes(String geofenceId, int geofenceTransition) {
        // Create the Database
        LocationDbHelper locationDbHelper = new LocationDbHelper(getApplicationContext());

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
                    .setContentText("You just crossed " + direction + name)
                    .setSmallIcon(R.drawable.band)
                    .build();
            final NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(1, geofenceNotification);

        }
    }

    /**
     * Maps geofence transition types to their human-readable equivalents.
     *
     * @param transitionType    A transition type constant defined in Geofence
     * @return                  A String indicating the type of transition
     */
    private String getTransitionString(int transitionType) {
        switch (transitionType) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                return getString(R.string.geofence_transition_entered);
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                return getString(R.string.geofence_transition_exited);
            default:
                return getString(R.string.unknown_geofence_transition);
        }
    }
}
