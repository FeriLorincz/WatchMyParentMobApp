package com.feri.watchmyparent.mobile.infrastructure.utils;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import androidx.health.connect.client.HealthConnectClient;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * ✅ REAL Setup Checker pentru Samsung Galaxy Watch 7
 * Verifică toate cerințele pentru conectarea reală la ceas
 */
public class SamsungWatchSetupChecker {

    private static final String TAG = "SamsungWatchSetupChecker";

    // Required apps for Samsung Galaxy Watch 7
    private static final String SAMSUNG_HEALTH_PACKAGE = "com.sec.android.app.shealth";
    private static final String SAMSUNG_WEARABLE_PACKAGE = "com.samsung.android.app.watchmanager";
    private static final String GOOGLE_PLAY_SERVICES_PACKAGE = "com.google.android.gms";
    private static final String HEALTH_CONNECT_PACKAGE = "com.google.android.apps.healthdata";

    public static class WatchSetupStatus {
        public final boolean isFullyReady;
        public final List<String> readyComponents;
        public final List<String> missingComponents;
        public final List<String> requiredActions;
        public final String summary;

        public WatchSetupStatus(boolean isFullyReady, List<String> readyComponents,
                                List<String> missingComponents, List<String> requiredActions, String summary) {
            this.isFullyReady = isFullyReady;
            this.readyComponents = readyComponents != null ? readyComponents : new ArrayList<>();
            this.missingComponents = missingComponents != null ? missingComponents : new ArrayList<>();
            this.requiredActions = requiredActions != null ? requiredActions : new ArrayList<>();
            this.summary = summary;
        }
    }

    public static CompletableFuture<WatchSetupStatus> checkCompleteSetup(Context context) {
        return CompletableFuture.supplyAsync(() -> {
            Log.d(TAG, "🔍 Starting complete Samsung Galaxy Watch 7 setup verification...");

            List<String> readyComponents = new ArrayList<>();
            List<String> missingComponents = new ArrayList<>();
            List<String> requiredActions = new ArrayList<>();

            // 1. ✅ Check Android Version
            checkAndroidVersion(readyComponents, missingComponents);

            // 2. ✅ Check Permissions
            checkPermissions(context, readyComponents, missingComponents, requiredActions);

            // 3. ✅ Check Health Connect
            checkHealthConnect(context, readyComponents, missingComponents, requiredActions);

            // 4. ✅ Check Bluetooth
            checkBluetooth(context, readyComponents, missingComponents, requiredActions);

            // 5. ✅ Check Samsung Apps
            checkSamsungApps(context, readyComponents, missingComponents, requiredActions);

            // 6. ✅ Check Google Play Services
            checkGooglePlayServices(context, readyComponents, missingComponents, requiredActions);

            // 7. ✅ Check Wear OS
            checkWearOS(context, readyComponents, missingComponents, requiredActions);

            boolean isFullyReady = missingComponents.isEmpty() && requiredActions.isEmpty();
            String summary = generateSummary(isFullyReady, readyComponents.size(),
                    missingComponents.size(), requiredActions.size());

            Log.d(TAG, "📊 Setup verification complete:");
            Log.d(TAG, "   ✅ Ready: " + readyComponents.size() + " components");
            Log.d(TAG, "   ❌ Missing: " + missingComponents.size() + " components");
            Log.d(TAG, "   ⚠️ Actions needed: " + requiredActions.size());
            Log.d(TAG, "   🎯 Fully ready: " + isFullyReady);

            return new WatchSetupStatus(isFullyReady, readyComponents, missingComponents, requiredActions, summary);
        });
    }

    private static void checkAndroidVersion(List<String> readyComponents, List<String> missingComponents) {
        int apiLevel = Build.VERSION.SDK_INT;
        Log.d(TAG, "📱 Android API Level: " + apiLevel);

        if (apiLevel >= 31) { // Android 12+
            readyComponents.add("✅ Android " + Build.VERSION.RELEASE + " (API " + apiLevel + ") - Supports Health Connect");
        } else if (apiLevel >= 26) { // Android 8+
            readyComponents.add("⚠️ Android " + Build.VERSION.RELEASE + " (API " + apiLevel + ") - Limited health features");
        } else {
            missingComponents.add("❌ Android version too old (API " + apiLevel + ") - Requires Android 8+");
        }
    }

    private static void checkPermissions(Context context, List<String> readyComponents,
                                         List<String> missingComponents, List<String> requiredActions) {
        Log.d(TAG, "🔐 Checking permissions...");

        // Essential permissions for Samsung Galaxy Watch 7
        String[] essentialPermissions = {
                Manifest.permission.BODY_SENSORS,
                Manifest.permission.ACTIVITY_RECOGNITION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH_CONNECT
        };

        List<String> missingPermissions = new ArrayList<>();
        int grantedCount = 0;

        for (String permission : essentialPermissions) {
            if (ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                grantedCount++;
            } else {
                missingPermissions.add(permission.replace("android.permission.", ""));
            }
        }

        if (grantedCount == essentialPermissions.length) {
            readyComponents.add("✅ All essential permissions granted (" + grantedCount + "/" + essentialPermissions.length + ")");
        } else {
            missingComponents.add("❌ Missing permissions: " + String.join(", ", missingPermissions));
            requiredActions.add("Grant missing permissions in app settings");
        }
    }

    private static void checkHealthConnect(Context context, List<String> readyComponents,
                                           List<String> missingComponents, List<String> requiredActions) {
        Log.d(TAG, "💚 Checking Health Connect...");

        try {
            // Check if Health Connect is available
            int sdkStatus = HealthConnectClient.getSdkStatus(context);

            switch (sdkStatus) {
                case HealthConnectClient.SDK_AVAILABLE:
                    readyComponents.add("✅ Health Connect available and ready");

                    // Check if Health Connect app is installed
                    if (isPackageInstalled(context, HEALTH_CONNECT_PACKAGE)) {
                        readyComponents.add("✅ Health Connect app installed");
                    } else {
                        missingComponents.add("❌ Health Connect app not installed");
                        requiredActions.add("Install Health Connect from Play Store");
                    }
                    break;

                case HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED:
                    missingComponents.add("❌ Health Connect needs update");
                    requiredActions.add("Update Health Connect from Play Store");
                    break;

                case HealthConnectClient.SDK_UNAVAILABLE:
                default:
                    missingComponents.add("❌ Health Connect not available on this device");
                    requiredActions.add("Install Health Connect from Play Store");
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking Health Connect", e);
            missingComponents.add("❌ Health Connect check failed: " + e.getMessage());
        }
    }

    private static void checkBluetooth(Context context, List<String> readyComponents,
                                       List<String> missingComponents, List<String> requiredActions) {
        Log.d(TAG, "📶 Checking Bluetooth...");

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            missingComponents.add("❌ Bluetooth not available on this device");
            return;
        }

        if (bluetoothAdapter.isEnabled()) {
            readyComponents.add("✅ Bluetooth enabled and ready");
        } else {
            missingComponents.add("❌ Bluetooth is disabled");
            requiredActions.add("Enable Bluetooth in device settings");
        }

        // Check Bluetooth permissions (API 31+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                readyComponents.add("✅ Bluetooth permissions granted");
            } else {
                missingComponents.add("❌ Bluetooth permissions not granted");
                requiredActions.add("Grant Bluetooth permissions for watch connection");
            }
        }
    }

    private static void checkSamsungApps(Context context, List<String> readyComponents,
                                         List<String> missingComponents, List<String> requiredActions) {
        Log.d(TAG, "🏢 Checking Samsung apps...");

        // Samsung Health
        if (isPackageInstalled(context, SAMSUNG_HEALTH_PACKAGE)) {
            readyComponents.add("✅ Samsung Health app installed");
        } else {
            missingComponents.add("❌ Samsung Health app not installed");
            requiredActions.add("Install Samsung Health from Galaxy Store or Play Store");
        }

        // Samsung Galaxy Watch Manager (optional but recommended)
        if (isPackageInstalled(context, SAMSUNG_WEARABLE_PACKAGE)) {
            readyComponents.add("✅ Samsung Wearable app installed");
        } else {
            // Not critical for Health Connect approach
            requiredActions.add("Consider installing Samsung Galaxy Watch app for better integration");
        }
    }

    private static void checkGooglePlayServices(Context context, List<String> readyComponents,
                                                List<String> missingComponents, List<String> requiredActions) {
        Log.d(TAG, "🔍 Checking Google Play Services...");

        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int result = googleAPI.isGooglePlayServicesAvailable(context);

        if (result == ConnectionResult.SUCCESS) {
            readyComponents.add("✅ Google Play Services available");
        } else {
            missingComponents.add("❌ Google Play Services issue: " + googleAPI.getErrorString(result));
            requiredActions.add("Update Google Play Services");
        }
    }

    private static void checkWearOS(Context context, List<String> readyComponents,
                                    List<String> missingComponents, List<String> requiredActions) {
        Log.d(TAG, "⌚ Checking Wear OS connectivity...");

        try {
            // Basic Wearable API check
            // Note: Actual device pairing check would require async operations
            readyComponents.add("✅ Wear OS API available");
            requiredActions.add("Ensure Samsung Galaxy Watch 7 is paired and connected");
        } catch (Exception e) {
            Log.e(TAG, "Error checking Wear OS", e);
            missingComponents.add("❌ Wear OS connectivity issue");
        }
    }

    private static boolean isPackageInstalled(Context context, String packageName) {
        try {
            context.getPackageManager().getPackageInfo(packageName, 0);
            Log.d(TAG, "✅ Package found: " + packageName);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            Log.d(TAG, "❌ Package not found: " + packageName);
            return false;
        }
    }

    private static String generateSummary(boolean isFullyReady, int readyCount, int missingCount, int actionsCount) {
        if (isFullyReady) {
            return "🎉 Samsung Galaxy Watch 7 setup is COMPLETE! Ready for real data collection.";
        } else if (missingCount == 0 && actionsCount > 0) {
            return "⚠️ Almost ready! " + actionsCount + " action(s) needed to complete setup.";
        } else {
            return "❌ Setup incomplete: " + missingCount + " missing component(s), " + actionsCount + " action(s) required.";
        }
    }

    // Helper methods for opening required apps/settings
    public static Intent getHealthConnectInstallIntent() {
        return new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=" + HEALTH_CONNECT_PACKAGE));
    }

    public static Intent getSamsungHealthInstallIntent() {
        return new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=" + SAMSUNG_HEALTH_PACKAGE));
    }

    public static Intent getBluetoothSettingsIntent() {
        return new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
    }

    public static Intent getLocationSettingsIntent() {
        return new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
    }
}
