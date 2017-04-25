package com.fermanis.volumebuddy;

import android.app.Application;
import android.content.Context;

/**
 * Created by zacfe on 4/20/2017.
 */

public class VolumeBuddy extends Application {

    private static Context context;

    public void onCreate() {
        super.onCreate();
        VolumeBuddy.context = getApplicationContext();
    }

    public static Context getAppContext() {
        return VolumeBuddy.context;
    }
}
