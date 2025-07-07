package com.feri.watchmyparent.mobile.infrastructure.watch;

import android.content.Context;
import android.util.Log;

public class WatchManagerFactory {

    public static WatchManager createWatchManager(Context context) {
        // For MVP, we only support Samsung Galaxy Watch 7
        // In the future, we can add logic to detect different watch types
        return createSamsungWatchManager(context);
    }

    public static WatchManager createSamsungWatchManager(Context context) {
        try {
            SamsungWatchManager manager = new SamsungWatchManager(context);
            Log.d("WatchManagerFactory", "Created Samsung Watch Manager");
            return manager;
        } catch (Exception e) {
            Log.e("WatchManagerFactory", "Failed to create Samsung Watch Manager", e);
            throw new RuntimeException("Unable to create Samsung Watch Manager", e);
        }
    }

    // Future implementation for other watch types
    public static WatchManager createAppleWatchManager(Context context) {
        throw new UnsupportedOperationException("Apple Watch not supported yet");
    }

    public static WatchManager createWearOSManager(Context context) {
        throw new UnsupportedOperationException("WearOS not supported yet");
    }
}
