package com.feri.watchmyparent.mobile;

import android.app.Application;
import com.squareup.leakcanary.core.BuildConfig;
import android.util.Log;
import dagger.hilt.android.HiltAndroidApp;
//import com.feri.watchmyparent.mobile.BuildConfig;

import timber.log.Timber;

@HiltAndroidApp
public class WatchMyParentApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        } else {
            // Pentru production, poți adăuga un custom tree
            Timber.plant(new ProductionTree());
        }

        Timber.d("WatchMyParentApplication initialized");
    }

    // Custom tree pentru production (opțional)
    private static class ProductionTree extends Timber.Tree {
        @Override
        protected void log(int priority, String tag, String message, Throwable t) {
            // În production, poți loga doar error-urile sau trimite la crashlytics
            if (priority >= android.util.Log.WARN) {
                // Log doar warning-uri și error-uri
                //android.util.Log.println(priority, tag, message);
                Log.println(priority, tag, message);
            }
        }
    }
}
