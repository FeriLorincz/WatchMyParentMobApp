package com.feri.watchmyparent.mobile.infrastructure.watch;

import android.content.Context;
import android.util.Log;

import com.feri.watchmyparent.mobile.infrastructure.services.SamsungHealthDataService;

//UPDATED Factory pentru Samsung Galaxy Watch 7 - REAL Implementation

public class WatchManagerFactory {
    private static final String TAG = "WatchManagerFactory";

    //Creates REAL Samsung Galaxy Watch 7 manager
    public static WatchManager createWatchManager(Context context) {
        Log.d(TAG, "🏭 Creating REAL Samsung Galaxy Watch 7 manager...");
        return createRealSamsungHealthManager(context);
    }

    //✅ Creates REAL Samsung Health Manager with full Samsung Galaxy Watch 7 support
    public static WatchManager createRealSamsungHealthManager(Context context) {
        try {
            Log.d(TAG, "🚀 Creating REAL Samsung Health Manager for Galaxy Watch 7...");

            RealSamsungHealthManager manager = new RealSamsungHealthManager(context, new SamsungHealthDataService(context));

            Log.d(TAG, "✅ Successfully created REAL Samsung Health Manager");
            Log.d(TAG, "   Device ID: " + manager.getDeviceId());
            Log.d(TAG, "   Implementation: REAL Health Connect + Hardware Sensors");

            return manager;

        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to create REAL Samsung Health Manager", e);

            // Fallback to previous implementation for compatibility
            Log.w(TAG, "🔄 Falling back to SamsungHealthManager (legacy)");
            return createSamsungHealthManagerFallback(context);
        }
    }

    //✅ Fallback implementation using the previous SamsungHealthManager
    @Deprecated
    public static WatchManager createSamsungHealthManagerFallback(Context context) {
        try {
            Log.d(TAG, "⚠️ Creating Samsung Health Manager (FALLBACK)...");

            SamsungHealthManager manager = new SamsungHealthManager(context);

            Log.d(TAG, "✅ Created Samsung Health Manager fallback");
            Log.d(TAG, "   Note: This uses simulated data with realistic patterns");

            return manager;

        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to create Samsung Health Manager fallback", e);

            // Final fallback to basic SamsungWatchManager
            Log.w(TAG, "🔄 Final fallback to SamsungWatchManager");
            return createSamsungWatchManagerFinalFallback(context);
        }
    }

    //✅ Final fallback using SamsungWatchManager
    @Deprecated
    public static WatchManager createSamsungWatchManagerFinalFallback(Context context) {
        try {
            SamsungWatchManager manager = new SamsungWatchManager(context);
            Log.d(TAG, "⚠️ Created Samsung Watch Manager (FINAL FALLBACK - simulated data)");
            return manager;
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to create Samsung Watch Manager final fallback", e);
            throw new RuntimeException("Unable to create any Samsung Watch Manager", e);
        }
    }

    // Future implementations for other watch types
    public static WatchManager createAppleWatchManager(Context context) {
        throw new UnsupportedOperationException("Apple Watch not supported yet - focus on Samsung Galaxy Watch 7");
    }

    public static WatchManager createWearOSManager(Context context) {
        throw new UnsupportedOperationException("Generic WearOS not supported yet - using Samsung Galaxy Watch 7 implementation");
    }

    public static WatchManager createFitbitManager(Context context) {
        throw new UnsupportedOperationException("Fitbit not supported yet - focus on Samsung Galaxy Watch 7");
    }

    //Get recommended manager type based on device capabilities
    public static String getRecommendedManagerType(Context context) {
        // For now, always recommend Samsung Galaxy Watch 7 implementation
        return "RealSamsungHealthManager";
    }

    //Check if REAL implementation is available
    public static boolean isRealImplementationAvailable(Context context) {
        try {
            // Quick check for essential components
            RealSamsungHealthManager testManager = new RealSamsungHealthManager(context, new SamsungHealthDataService(context));
            boolean available = testManager.isHealthConnectReady() || testManager.areHardwareSensorsReady();

            Log.d(TAG, "📊 REAL implementation available: " + available);
            return available;

        } catch (Exception e) {
            Log.e(TAG, "❌ Error checking REAL implementation availability", e);
            return false;
        }
    }

    //Get implementation info for debugging
    public static String getImplementationInfo(WatchManager manager) {
        if (manager instanceof RealSamsungHealthManager) {
            RealSamsungHealthManager realManager = (RealSamsungHealthManager) manager;
            return String.format("REAL Samsung Health Manager - Health Connect: %s, Hardware: %s, Sensors: %d",
                    realManager.isHealthConnectReady() ? "✅" : "❌",
                    realManager.areHardwareSensorsReady() ? "✅" : "❌",
                    realManager.getRegisteredSensorCount());
        } else if (manager instanceof SamsungHealthManager) {
            return "Samsung Health Manager (FALLBACK) - Realistic simulation with Health Connect";
        } else if (manager instanceof SamsungWatchManager) {
            return "Samsung Watch Manager (LEGACY) - Basic simulation";
        } else {
            return "Unknown implementation: " + manager.getClass().getSimpleName();
        }
    }
}

