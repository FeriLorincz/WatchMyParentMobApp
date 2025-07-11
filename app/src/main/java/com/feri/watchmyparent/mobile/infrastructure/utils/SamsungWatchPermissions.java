package com.feri.watchmyparent.mobile.infrastructure.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.health.connect.client.HealthConnectClient;
import androidx.health.connect.client.PermissionController;
import androidx.health.connect.client.permission.HealthPermission;
import androidx.health.connect.client.records.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * ✅ REAL Permissions Manager pentru Samsung Galaxy Watch 7
 * Gestionează toate permisiunile necesare pentru conectarea reală la ceas
 */
public class SamsungWatchPermissions {

    private static final String TAG = "SamsungWatchPermissions";

    // Request codes for different permission types
    public static final int REQUEST_CODE_BASIC_PERMISSIONS = 1001;
    public static final int REQUEST_CODE_HEALTH_PERMISSIONS = 1002;
    public static final int REQUEST_CODE_LOCATION_PERMISSIONS = 1003;
    public static final int REQUEST_CODE_BLUETOOTH_PERMISSIONS = 1004;

    // Essential permissions for Samsung Galaxy Watch 7
    private static final String[] BASIC_PERMISSIONS = {
            Manifest.permission.BODY_SENSORS,
            Manifest.permission.ACTIVITY_RECOGNITION,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.POST_NOTIFICATIONS
    };

    private static final String[] LOCATION_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    private static final String[] BLUETOOTH_PERMISSIONS_API31 = {
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE
    };

    private static final String[] BLUETOOTH_PERMISSIONS_LEGACY = {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN
    };

    // Health Connect permissions for Samsung Galaxy Watch 7 data
    private static final Set<String> HEALTH_CONNECT_PERMISSIONS = Set.of(
            HealthPermission.getReadPermission(HeartRateRecord.class),
            HealthPermission.getReadPermission(StepsRecord.class),
            HealthPermission.getReadPermission(SleepSessionRecord.class),
            HealthPermission.getReadPermission(DistanceRecord.class),
            HealthPermission.getReadPermission(TotalCaloriesBurnedRecord.class),
            HealthPermission.getReadPermission(ExerciseSessionRecord.class),
            HealthPermission.getReadPermission(SpeedRecord.class),
            HealthPermission.getReadPermission(PowerRecord.class)
    );

    public static class PermissionStatus {
        public final boolean allGranted;
        public final List<String> grantedPermissions;
        public final List<String> deniedPermissions;
        public final List<String> requiredActions;
        public final String summary;

        public PermissionStatus(boolean allGranted, List<String> grantedPermissions,
                                List<String> deniedPermissions, List<String> requiredActions, String summary) {
            this.allGranted = allGranted;
            this.grantedPermissions = grantedPermissions != null ? grantedPermissions : new ArrayList<>();
            this.deniedPermissions = deniedPermissions != null ? deniedPermissions : new ArrayList<>();
            this.requiredActions = requiredActions != null ? requiredActions : new ArrayList<>();
            this.summary = summary;
        }
    }

    /**
     * Check all permissions required for Samsung Galaxy Watch 7
     */
    public static CompletableFuture<PermissionStatus> checkAllPermissions(Context context) {
        return CompletableFuture.supplyAsync(() -> {
            Log.d(TAG, "🔐 Checking all permissions for Samsung Galaxy Watch 7...");

            List<String> grantedPermissions = new ArrayList<>();
            List<String> deniedPermissions = new ArrayList<>();
            List<String> requiredActions = new ArrayList<>();

            // Check basic permissions
            checkBasicPermissions(context, grantedPermissions, deniedPermissions, requiredActions);

            // Check location permissions
            checkLocationPermissions(context, grantedPermissions, deniedPermissions, requiredActions);

            // Check Bluetooth permissions
            checkBluetoothPermissions(context, grantedPermissions, deniedPermissions, requiredActions);

            boolean allGranted = deniedPermissions.isEmpty();
            String summary = generatePermissionSummary(allGranted, grantedPermissions.size(), deniedPermissions.size());

            Log.d(TAG, "📊 Permission check complete:");
            Log.d(TAG, "   ✅ Granted: " + grantedPermissions.size());
            Log.d(TAG, "   ❌ Denied: " + deniedPermissions.size());
            Log.d(TAG, "   🎯 All granted: " + allGranted);

            return new PermissionStatus(allGranted, grantedPermissions, deniedPermissions, requiredActions, summary);
        });
    }

    private static void checkBasicPermissions(Context context, List<String> grantedPermissions,
                                              List<String> deniedPermissions, List<String> requiredActions) {
        for (String permission : BASIC_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                grantedPermissions.add(getPermissionDisplayName(permission));
            } else {
                deniedPermissions.add(getPermissionDisplayName(permission));
            }
        }

        if (!deniedPermissions.isEmpty()) {
            requiredActions.add("Grant basic sensor and app permissions");
        }
    }

    private static void checkLocationPermissions(Context context, List<String> grantedPermissions,
                                                 List<String> deniedPermissions, List<String> requiredActions) {
        for (String permission : LOCATION_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                grantedPermissions.add(getPermissionDisplayName(permission));
            } else {
                deniedPermissions.add(getPermissionDisplayName(permission));
            }
        }

        // Check background location if targeting API 29+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                grantedPermissions.add("Background Location");
            } else {
                deniedPermissions.add("Background Location");
                requiredActions.add("Grant background location for continuous tracking");
            }
        }
    }

    private static void checkBluetoothPermissions(Context context, List<String> grantedPermissions,
                                                  List<String> deniedPermissions, List<String> requiredActions) {
        String[] bluetoothPermissions = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                ? BLUETOOTH_PERMISSIONS_API31
                : BLUETOOTH_PERMISSIONS_LEGACY;

        for (String permission : bluetoothPermissions) {
            if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                grantedPermissions.add(getPermissionDisplayName(permission));
            } else {
                deniedPermissions.add(getPermissionDisplayName(permission));
            }
        }

        if (!grantedPermissions.isEmpty() && deniedPermissions.isEmpty()) {
            // No action needed if all Bluetooth permissions granted
        } else if (!deniedPermissions.isEmpty()) {
            requiredActions.add("Grant Bluetooth permissions for Samsung Galaxy Watch 7 connection");
        }
    }

    /**
     * Request basic permissions required for Samsung Galaxy Watch 7
     */
    public static void requestBasicPermissions(Activity activity) {
        Log.d(TAG, "📱 Requesting basic permissions for Samsung Galaxy Watch 7...");

        List<String> permissionsToRequest = new ArrayList<>();

        for (String permission : BASIC_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(activity,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_CODE_BASIC_PERMISSIONS);
        } else {
            Log.d(TAG, "✅ All basic permissions already granted");
        }
    }

    /**
     * Request location permissions
     */
    public static void requestLocationPermissions(Activity activity) {
        Log.d(TAG, "📍 Requesting location permissions for Samsung Galaxy Watch 7...");

        List<String> permissionsToRequest = new ArrayList<>(Arrays.asList(LOCATION_PERMISSIONS));

        ActivityCompat.requestPermissions(activity,
                permissionsToRequest.toArray(new String[0]),
                REQUEST_CODE_LOCATION_PERMISSIONS);
    }

    /**
     * Request Bluetooth permissions (API level dependent)
     */
    public static void requestBluetoothPermissions(Activity activity) {
        Log.d(TAG, "📶 Requesting Bluetooth permissions for Samsung Galaxy Watch 7...");

        String[] bluetoothPermissions = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                ? BLUETOOTH_PERMISSIONS_API31
                : BLUETOOTH_PERMISSIONS_LEGACY;

        List<String> permissionsToRequest = new ArrayList<>();

        for (String permission : bluetoothPermissions) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(activity,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_CODE_BLUETOOTH_PERMISSIONS);
        } else {
            Log.d(TAG, "✅ All Bluetooth permissions already granted");
        }
    }

    /**
     * Request Health Connect permissions for Samsung Galaxy Watch 7 data
     */
    public static CompletableFuture<Boolean> requestHealthConnectPermissions(Context context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "💚 Requesting Health Connect permissions for Samsung Galaxy Watch 7...");

                if (HealthConnectClient.getSdkStatus(context) != HealthConnectClient.SDK_AVAILABLE) {
                    Log.w(TAG, "❌ Health Connect not available");
                    return false;
                }

                HealthConnectClient healthConnectClient = HealthConnectClient.getOrCreate(context);
                PermissionController permissionController = healthConnectClient.getPermissionController();

                // Check granted permissions
                Set<String> grantedPermissions = permissionController.getGrantedPermissions().join();

                // Request missing permissions
                if (!grantedPermissions.containsAll(HEALTH_CONNECT_PERMISSIONS)) {
                    // In a real app, this would open the Health Connect permission screen
                    Log.d(TAG, "⚠️ Not all Health Connect permissions granted. User intervention required.");
                    return false;
                } else {
                    Log.d(TAG, "✅ All Health Connect permissions granted");
                    return true;
                }

            } catch (Exception e) {
                Log.e(TAG, "❌ Error requesting Health Connect permissions", e);
                return false;
            }
        });
    }

    /**
     * Check if all essential permissions are granted
     */
    public static boolean areEssentialPermissionsGranted(Context context) {
        // Check basic permissions
        for (String permission : BASIC_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }

        // Check at least coarse location
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        // Check Bluetooth permissions based on API level
        String[] bluetoothPermissions = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                ? BLUETOOTH_PERMISSIONS_API31
                : BLUETOOTH_PERMISSIONS_LEGACY;

        for (String permission : bluetoothPermissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }

        return true;
    }

    /**
     * Open app settings for manual permission configuration
     */
    public static Intent getAppSettingsIntent(Context context) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", context.getPackageName(), null));
        return intent;
    }

    /**
     * Get user-friendly permission name
     */
    private static String getPermissionDisplayName(String permission) {
        switch (permission) {
            case Manifest.permission.BODY_SENSORS:
                return "Body Sensors";
            case Manifest.permission.ACTIVITY_RECOGNITION:
                return "Activity Recognition";
            case Manifest.permission.ACCESS_FINE_LOCATION:
                return "Precise Location";
            case Manifest.permission.ACCESS_COARSE_LOCATION:
                return "Approximate Location";
            case Manifest.permission.ACCESS_BACKGROUND_LOCATION:
                return "Background Location";
            case Manifest.permission.BLUETOOTH_CONNECT:
                return "Bluetooth Connect";
            case Manifest.permission.BLUETOOTH_SCAN:
                return "Bluetooth Scan";
            case Manifest.permission.BLUETOOTH:
                return "Bluetooth";
            case Manifest.permission.BLUETOOTH_ADMIN:
                return "Bluetooth Admin";
            case Manifest.permission.WAKE_LOCK:
                return "Keep Device Awake";
            case Manifest.permission.FOREGROUND_SERVICE:
                return "Background Service";
            case Manifest.permission.POST_NOTIFICATIONS:
                return "Notifications";
            default:
                return permission.replace("android.permission.", "");
        }
    }

    private static String generatePermissionSummary(boolean allGranted, int grantedCount, int deniedCount) {
        if (allGranted) {
            return "🎉 All permissions granted! Samsung Galaxy Watch 7 ready to connect.";
        } else {
            return "⚠️ " + deniedCount + " permission(s) needed for full Samsung Galaxy Watch 7 functionality.";
        }
    }

    /**
     * Handle permission request results
     */
    public static boolean handlePermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        boolean allGranted = true;

        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        switch (requestCode) {
            case REQUEST_CODE_BASIC_PERMISSIONS:
                Log.d(TAG, allGranted ? "✅ Basic permissions granted" : "❌ Some basic permissions denied");
                break;
            case REQUEST_CODE_LOCATION_PERMISSIONS:
                Log.d(TAG, allGranted ? "✅ Location permissions granted" : "❌ Some location permissions denied");
                break;
            case REQUEST_CODE_BLUETOOTH_PERMISSIONS:
                Log.d(TAG, allGranted ? "✅ Bluetooth permissions granted" : "❌ Some Bluetooth permissions denied");
                break;
        }

        return allGranted;
    }
}
