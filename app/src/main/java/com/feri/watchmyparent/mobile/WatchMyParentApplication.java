package com.feri.watchmyparent.mobile;

import android.app.Application;
import android.util.Log;
import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class WatchMyParentApplication extends Application {

    private static final String TAG = "WatchMyParentApp";

    @Override
    public void onCreate() {
        super.onCreate();

        // Inițializare simplă folosind Log standard Android pentru moment
        initializeLogging();

        Log.d(TAG, "WatchMyParentApplication initialized");
    }

    private void initializeLogging() {
        // Verifică dacă aplicația rulează în mod debug
        boolean isDebug = (getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0;

        if (isDebug) {
            Log.d(TAG, "Running in DEBUG mode");
        } else {
            Log.d(TAG, "Running in RELEASE mode");
        }
    }
}
