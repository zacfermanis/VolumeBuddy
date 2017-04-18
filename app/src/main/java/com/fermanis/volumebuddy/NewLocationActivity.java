package com.fermanis.volumebuddy;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import static android.R.attr.name;

public class NewLocationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        final boolean isNew;
        double lat =0.0, longitude = 0.0;
        String locationName = "";
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_location);


        final String action = getIntent().getExtras().getString("ACTION");
        if (action != null && action.equalsIgnoreCase("NEW")) {
            isNew = true;
            lat =  getIntent().getExtras().getDouble("LAT");
            longitude = getIntent().getExtras().getDouble("LONG");
        } else {
            isNew = false;
            locationName = getIntent().getExtras().getString("LOC_NAME");

            // Load the Database
            LocationDbHelper locationDbHelper = new LocationDbHelper(getApplicationContext());
            SQLiteDatabase db = locationDbHelper.getReadableDatabase();

            Cursor cursor = db.rawQuery("select * from locations where " + LocationContract.LocationEntry.COLUMN_NAME_NAME + "=?", new String[] { locationName});

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

            EditText nameEditText =  (EditText) findViewById(R.id.nameInput);
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
                EditText nameEditText =  (EditText) findViewById(R.id.nameInput);
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

        Button deleteButton  = (Button) findViewById(R.id.deleteButton);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteLocation(finalLocationName);
                Intent intent = new Intent();
                intent.setClass(getApplicationContext(), LocationList.class);
                startActivity(intent);
            }
        });

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

        db.update(LocationContract.LocationEntry.TABLE_NAME, values, LocationContract.LocationEntry.COLUMN_NAME_NAME + "=?", new String[] {locationName});
    }

    private void addLocation(LatLng point, String name, int alarm, int media, int ringer, int notif) {

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

        db.insert(LocationContract.LocationEntry.TABLE_NAME, null, values);
    }

    private void deleteLocation(String locationName) {
        LocationDbHelper locationDbHelper = new LocationDbHelper(getApplicationContext());
        SQLiteDatabase db = locationDbHelper.getWritableDatabase();
        db.delete(LocationContract.LocationEntry.TABLE_NAME, LocationContract.LocationEntry.COLUMN_NAME_NAME + "=?", new String[] { locationName});
    }
}
