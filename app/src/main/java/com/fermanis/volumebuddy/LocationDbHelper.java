package com.fermanis.volumebuddy;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by zacfe on 4/10/2017.
 */

public class LocationDbHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "Locations.db";

    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + LocationContract.LocationEntry.TABLE_NAME + " (" +
                    LocationContract.LocationEntry._ID + " INTEGER PRIMARY KEY," +
                    LocationContract.LocationEntry.COLUMN_NAME_NAME + " TEXT," +
                    LocationContract.LocationEntry.COLUMN_NAME_LATITUDE + " REAL," +
                    LocationContract.LocationEntry.COLUMN_NAME_LONGITUDE + " REAL," +
                    LocationContract.LocationEntry.COLUMN_NAME_ALARM_VOLUME + " INT," +
                    LocationContract.LocationEntry.COLUMN_NAME_MEDIA_VOLUME + " INT," +
                    LocationContract.LocationEntry.COLUMN_NAME_RINGER_VOLUME + " INT," +
                    LocationContract.LocationEntry.COLUMN_NAME_NOTIFICATION_VOLUME + " INT)";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + LocationContract.LocationEntry.TABLE_NAME;

    public LocationDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }


}
