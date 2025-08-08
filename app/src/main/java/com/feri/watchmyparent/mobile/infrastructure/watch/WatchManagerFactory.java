package com.feri.watchmyparent.mobile.infrastructure.watch;

import android.content.Context;
import android.util.Log;

import com.feri.watchmyparent.mobile.infrastructure.services.SamsungHealthDataService;

// ✅ UPDATED Factory pentru Samsung Galaxy Watch 7 - DOAR RealSamsungHealthManager
public class WatchManagerFactory {
    private static final String TAG = "WatchManagerFactory";

    // ✅ SIMPLIFIED: Creează DOAR RealSamsungHealthManager
    public static WatchManager createWatchManager(Context context) {
        Log.d(TAG, "🏭 Creating REAL Samsung Galaxy Watch 7 manager...");
        return createRealSamsungHealthManager(context);
    }

    // ✅ Creates REAL Samsung Health Manager with full Samsung Galaxy Watch 7 support
    public static WatchManager createRealSamsungHealthManager(Context context) {
        try {
            Log.d(TAG, "🚀 Creating REAL Samsung Health Manager for Galaxy Watch 7...");

            // ✅ FIXED: Creează SamsungHealthDataService separat
            SamsungHealthDataService healthDataService = new SamsungHealthDataService(context);
            RealSamsungHealthManager manager = new RealSamsungHealthManager(context, healthDataService);

            Log.d(TAG, "✅ Successfully created REAL Samsung Health Manager");
            Log.d(TAG, "   Device ID: " + manager.getDeviceId());
            Log.d(TAG, "   Implementation: REAL Health Connect + Hardware Sensors + Samsung Health SDK");

            return manager;

        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to create REAL Samsung Health Manager", e);
            throw new RuntimeException("Unable to create Samsung Health Manager - implementation error", e);
        }
    }

    // ✅ REMOVED: Nu mai avem nevoie de createSamsungHealthManager - folosim doar REAL

    // Future implementations for other watch types
    public static WatchManager createAppleWatchManager(Context context) {
        throw new UnsupportedOperationException("Apple Watch not supported - focus on Samsung Galaxy Watch 7");
    }

    public static WatchManager createWearOSManager(Context context) {
        throw new UnsupportedOperationException("Generic WearOS not supported - using Samsung Galaxy Watch 7");
    }

    public static WatchManager createFitbitManager(Context context) {
        throw new UnsupportedOperationException("Fitbit not supported - focus on Samsung Galaxy Watch 7");
    }

    // ✅ SIMPLIFIED: Always recommend REAL implementation
    public static String getRecommendedManagerType(Context context) {
        return "RealSamsungHealthManager";
    }

    // ✅ Check if REAL implementation is available
    public static boolean isRealImplementationAvailable(Context context) {
        try {
            SamsungHealthDataService healthDataService = new SamsungHealthDataService(context);
            RealSamsungHealthManager testManager = new RealSamsungHealthManager(context, healthDataService);

            boolean available = testManager.isHealthConnectReady() ||
                    testManager.areHardwareSensorsReady() ||
                    testManager.isSamsungHealthDataConnected();

            Log.d(TAG, "📊 REAL implementation available: " + available);
            return available;

        } catch (Exception e) {
            Log.e(TAG, "❌ Error checking REAL implementation availability", e);
            return false;
        }
    }

    // ✅ Enhanced implementation info for debugging
    public static String getImplementationInfo(WatchManager manager) {
        if (manager instanceof RealSamsungHealthManager) {
            RealSamsungHealthManager realManager = (RealSamsungHealthManager) manager;
            return String.format("REAL Samsung Health Manager - " +
                            "Health Connect: %s, Hardware: %s, Samsung Health: %s, Sensors: %d",
                    realManager.isHealthConnectReady() ? "✅" : "❌",
                    realManager.areHardwareSensorsReady() ? "✅" : "❌",
                    realManager.isSamsungHealthDataConnected() ? "✅" : "❌",
                    realManager.getRegisteredSensorCount());
        } else {
            return "Unknown implementation: " + manager.getClass().getSimpleName();
        }
    }

    // ✅ NEW: Health status check
    public static String getHealthStatus(WatchManager manager) {
        if (manager instanceof RealSamsungHealthManager) {
            RealSamsungHealthManager realManager = (RealSamsungHealthManager) manager;

            boolean anyConnectionReady = realManager.isHealthConnectReady() ||
                    realManager.areHardwareSensorsReady() ||
                    realManager.isSamsungHealthDataConnected();

            if (anyConnectionReady) {
                return "✅ Samsung Galaxy Watch 7 ready for REAL data collection";
            } else {
                return "⚠️ Samsung Galaxy Watch 7 connections not ready";
            }
        }
        return "❌ Invalid watch manager implementation";
    }
}