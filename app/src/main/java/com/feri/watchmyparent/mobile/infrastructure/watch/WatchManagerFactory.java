package com.feri.watchmyparent.mobile.infrastructure.watch;

import android.content.Context;
import android.util.Log;

public class WatchManagerFactory {

    private static final String TAG = "WatchManagerFactory";

    public static WatchManager createWatchManager(Context context) {
        // Use real Samsung Health SDK implementation
        return createSamsungHealthManager(context);
    }

    public static WatchManager createSamsungHealthManager(Context context) {
        try {
            SamsungHealthManager manager = new SamsungHealthManager(context);
            Log.d(TAG, "✅ Created Samsung Health Manager (REAL SDK)");
            return manager;
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to create Samsung Health Manager", e);

            // Fallback to old SamsungWatchManager for compatibility
            Log.w(TAG, "🔄 Falling back to SamsungWatchManager");
            return createSamsungWatchManagerFallback(context);
        }
    }

    @Deprecated
    public static WatchManager createSamsungWatchManagerFallback(Context context) {
        try {
            SamsungWatchManager manager = new SamsungWatchManager(context);
            Log.d(TAG, "⚠️ Created Samsung Watch Manager (FALLBACK - simulated data)");
            return manager;
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to create Samsung Watch Manager fallback", e);
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
