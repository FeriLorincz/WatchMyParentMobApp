package com.feri.watchmyparent.mobile.infrastructure.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import androidx.health.connect.client.HealthConnectClient;

public class HealthConnectChecker {

    private static final String TAG = "HealthConnectChecker";
    private static final String HEALTH_CONNECT_PACKAGE = "com.google.android.apps.healthdata";

    public static class HealthConnectStatus {
        public final boolean isAvailable;
        public final boolean isInstalled;
        public final String statusMessage;
        public final int sdkStatus;

        public HealthConnectStatus(boolean isAvailable, boolean isInstalled, String statusMessage, int sdkStatus) {
            this.isAvailable = isAvailable;
            this.isInstalled = isInstalled;
            this.statusMessage = statusMessage;
            this.sdkStatus = sdkStatus;
        }
    }

    public static HealthConnectStatus checkHealthConnectAvailability(Context context) {
        try {
            // Check SDK status
            int sdkStatus = HealthConnectClient.getSdkStatus(context);
            Log.d(TAG, "Health Connect SDK Status: " + sdkStatus);

            // Check if Health Connect app is installed
            boolean isInstalled = isHealthConnectInstalled(context);
            Log.d(TAG, "Health Connect app installed: " + isInstalled);

            // Check Android version
            boolean androidSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE; // Android 14+
            Log.d(TAG, "Android version supported: " + androidSupported + " (API " + Build.VERSION.SDK_INT + ")");

            String statusMessage = getStatusMessage(sdkStatus, isInstalled, androidSupported);
            boolean isAvailable = (sdkStatus == HealthConnectClient.SDK_AVAILABLE);

            return new HealthConnectStatus(isAvailable, isInstalled, statusMessage, sdkStatus);

        } catch (Exception e) {
            Log.e(TAG, "Error checking Health Connect availability", e);
            return new HealthConnectStatus(false, false, "Error: " + e.getMessage(), -1);
        }
    }

    private static boolean isHealthConnectInstalled(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            pm.getPackageInfo(HEALTH_CONNECT_PACKAGE, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private static String getStatusMessage(int sdkStatus, boolean isInstalled, boolean androidSupported) {
        switch (sdkStatus) {
            case HealthConnectClient.SDK_AVAILABLE:
                return "✅ Health Connect is available and ready to use";

            case HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED:
                return "⚠️ Health Connect update required. Please update from Play Store.";

            case HealthConnectClient.SDK_UNAVAILABLE:
                if (!androidSupported) {
                    return "❌ Your Android version does not support Health Connect (requires Android 14+)";
                } else if (!isInstalled) {
                    return "❌ Health Connect not installed. Please install from Play Store.";
                } else {
                    return "❌ Health Connect is not available on this device";
                }

            default:
                return "❓ Unknown Health Connect status: " + sdkStatus;
        }
    }

    public static Intent getHealthConnectInstallIntent() {
        return new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=" + HEALTH_CONNECT_PACKAGE));
    }
}
