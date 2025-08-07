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

            // Înlocuim fostul fallback cu o excepție - forțăm utilizarea implementării reale
            Log.e(TAG, "❌ No fallback available - implementation must be fixed");
            throw new RuntimeException("Unable to create Samsung Health Manager - implementation error", e);
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
        } else {
            return "Unknown implementation: " + manager.getClass().getSimpleName();
        }
    }
}