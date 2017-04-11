package com.fermanis.volumebuddy;

import android.provider.BaseColumns;

/**
 * Created by zacfe on 4/10/2017.
 */

public final class LocationContract {

    private LocationContract() {}

    public static class LocationEntry implements BaseColumns {
        public static final String TABLE_NAME = "locations";
        public static final String COLUMN_NAME_LATITUDE = "lat";
        public static final String COLUMN_NAME_LONGITUDE = "long";
        public static final String COLUMN_NAME_ALARM_VOLUME = "alarm";
        public static final String COLUMN_NAME_MEDIA_VOLUME = "media";
        public static final String COLUMN_NAME_RINGER_VOLUME = "ringer";
        public static final String COLUMN_NAME_NOTIFICATION_VOLUME = "notification";
        public static final String COLUMN_NAME_NAME = "name";
    }
}
