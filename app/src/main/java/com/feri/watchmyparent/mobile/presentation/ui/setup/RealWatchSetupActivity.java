package com.feri.watchmyparent.mobile.presentation.ui.setup;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.health.connect.client.HealthConnectClient;

import com.feri.watchmyparent.mobile.R;
import com.feri.watchmyparent.mobile.infrastructure.utils.SamsungWatchPermissions;
import com.feri.watchmyparent.mobile.infrastructure.utils.SamsungWatchSetupChecker;
import com.feri.watchmyparent.mobile.infrastructure.watch.WatchManager;
import com.feri.watchmyparent.mobile.infrastructure.watch.WatchManagerFactory;
import com.feri.watchmyparent.mobile.presentation.ui.common.BaseActivity;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Setup Activity for Samsung Galaxy Watch 7
 * Guides users through permissions, required apps, and watch connection
 */
@AndroidEntryPoint
public class RealWatchSetupActivity extends BaseActivity {

    private static final String TAG = "RealWatchSetupActivity";

    // Constants for package names
    private static final String HEALTH_CONNECT_PACKAGE = "com.google.android.apps.healthdata";
    private static final String SAMSUNG_HEALTH_PACKAGE = "com.sec.android.app.shealth";
    private static final String SAMSUNG_WEARABLE_PACKAGE = "com.samsung.android.app.watchmanager";

    // Request codes
    private static final int REQUEST_CODE_ALL_PERMISSIONS = 100;
    private static final int REQUEST_CODE_BACKGROUND_LOCATION = 101;

    // UI Components
    private TextView statusSummaryText;
    private TextView permissionStatusText;
    private TextView appsStatusText;
    private TextView connectionStatusText;
    private TextView implementationInfoText;
    private Button checkSetupButton;
    private Button requestPermissionsButton;
    private Button installAppsButton;
    private Button connectWatchButton;
    private Button testSensorsButton;
    private ProgressBar progressBar;

    // State
    private SamsungWatchSetupChecker.WatchSetupStatus setupStatus;
    private SamsungWatchPermissions.PermissionStatus permissionStatus;
    private WatchManager watchManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_real_watch_setup);

        setupToolbar((Toolbar) findViewById(R.id.toolbar), "Samsung Galaxy Watch 7 Setup", true);
        initializeViews();
        setupClickListeners();

        // Check initial status
        checkInitialStatus();
    }

    private void initializeViews() {
        statusSummaryText = findViewById(R.id.tv_status_summary);
        permissionStatusText = findViewById(R.id.tv_permission_status);
        appsStatusText = findViewById(R.id.tv_apps_status);
        connectionStatusText = findViewById(R.id.tv_connection_status);
        implementationInfoText = findViewById(R.id.tv_implementation_info);
        checkSetupButton = findViewById(R.id.btn_check_setup);
        requestPermissionsButton = findViewById(R.id.btn_request_permissions);
        installAppsButton = findViewById(R.id.btn_install_apps);
        connectWatchButton = findViewById(R.id.btn_connect_watch);
        testSensorsButton = findViewById(R.id.btn_test_data);
        progressBar = findViewById(R.id.progress_bar);

        // If any view is missing, add a check to prevent NullPointerException
        if (statusSummaryText == null) {
            statusSummaryText = new TextView(this);
            Toast.makeText(this, "Layout incomplete: missing status_summary TextView",
                    Toast.LENGTH_SHORT).show();
        }

        // Do the same for other views if needed
    }

    private void setupClickListeners() {
        checkSetupButton.setOnClickListener(v -> checkWatchSetup());
        requestPermissionsButton.setOnClickListener(v -> requestAllPermissions());
        installAppsButton.setOnClickListener(v -> installRequiredApps());
        connectWatchButton.setOnClickListener(v -> connectToWatch());
        testSensorsButton.setOnClickListener(v -> testSensors());

        // Add override button listener
        Button overrideButton = findViewById(R.id.btn_override);
        if (overrideButton != null) {
            overrideButton.setOnClickListener(v -> {
                // Enable the connection button regardless of checks
                connectWatchButton.setEnabled(true);
                Toast.makeText(this, "Checks overridden. Connection enabled.",
                        Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void checkInitialStatus() {
        // Check permissions
        boolean hasAllPermissions = checkAllPermissions();

        // Check required apps
        boolean hasAllApps = checkRequiredApps();

        // Check Health Connect status
        checkHealthConnectClientStatus();

        // Update UI based on status
        updateSetupStatusUI(hasAllPermissions, hasAllApps);

        // If everything is ready, enable watch connection
        if (hasAllPermissions && hasAllApps) {
            connectWatchButton.setEnabled(true);
        }
    }

    private boolean checkAllPermissions() {
        boolean hasBodySensors = ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS)
                == PackageManager.PERMISSION_GRANTED;

        boolean hasActivityRecognition = ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                == PackageManager.PERMISSION_GRANTED;

        boolean hasLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;

        boolean hasBackgroundLocation = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            hasBackgroundLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    == PackageManager.PERMISSION_GRANTED;
        }

        boolean hasBluetooth = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH)
                == PackageManager.PERMISSION_GRANTED;

        boolean hasBluetoothAdmin = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN)
                == PackageManager.PERMISSION_GRANTED;

        boolean hasBluetoothConnect = true;
        boolean hasBluetoothScan = true;
        boolean hasBluetoothAdvertise = true;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasBluetoothConnect = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    == PackageManager.PERMISSION_GRANTED;
            hasBluetoothScan = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                    == PackageManager.PERMISSION_GRANTED;
            hasBluetoothAdvertise = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE)
                    == PackageManager.PERMISSION_GRANTED;
        }

        // Log each permission status
        Log.d("RealWatchSetupActivity", "Permission Status:");
        Log.d("RealWatchSetupActivity", "- BODY_SENSORS: " + hasBodySensors);
        Log.d("RealWatchSetupActivity", "- ACTIVITY_RECOGNITION: " + hasActivityRecognition);
        Log.d("RealWatchSetupActivity", "- ACCESS_FINE_LOCATION: " + hasLocation);
        Log.d("RealWatchSetupActivity", "- ACCESS_BACKGROUND_LOCATION: " + hasBackgroundLocation);
        Log.d("RealWatchSetupActivity", "- BLUETOOTH: " + hasBluetooth);
        Log.d("RealWatchSetupActivity", "- BLUETOOTH_ADMIN: " + hasBluetoothAdmin);
        Log.d("RealWatchSetupActivity", "- BLUETOOTH_CONNECT: " + hasBluetoothConnect);
        Log.d("RealWatchSetupActivity", "- BLUETOOTH_SCAN: " + hasBluetoothScan);
        Log.d("RealWatchSetupActivity", "- BLUETOOTH_ADVERTISE: " + hasBluetoothAdvertise);

        return hasBodySensors && hasActivityRecognition && hasLocation &&
                hasBackgroundLocation && hasBluetooth && hasBluetoothAdmin &&
                hasBluetoothConnect && hasBluetoothScan && hasBluetoothAdvertise;
    }

    private boolean checkRequiredApps() {

        // Log all installed packages that contain "health" to find the Samsung Health package
        List<ApplicationInfo> apps = getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);
        for (ApplicationInfo app : apps) {
            if (app.packageName.toLowerCase().contains("health") ||
                    app.packageName.toLowerCase().contains("samsung")) {
                Log.d("RealWatchSetupActivity", "Found health-related app: " + app.packageName);
            }
        }
        boolean healthConnectInstalled = isPackageInstalled(HEALTH_CONNECT_PACKAGE);
        boolean samsungHealthInstalled = isPackageInstalled(SAMSUNG_HEALTH_PACKAGE);

        // Log app status
        Log.d("RealWatchSetupActivity", "App Status:");
        Log.d("RealWatchSetupActivity", "- Health Connect: " + healthConnectInstalled);
        Log.d("RealWatchSetupActivity", "- Samsung Health: " + samsungHealthInstalled);

        if (!healthConnectInstalled) {
            Log.d("RealWatchSetupActivity", "Health Connect not found with package: " + HEALTH_CONNECT_PACKAGE);
        }

        if (!samsungHealthInstalled) {
            Log.d("RealWatchSetupActivity", "Samsung Health not found with package: " + SAMSUNG_HEALTH_PACKAGE);
        }

        // Try alternative package names if not found
        if (!healthConnectInstalled) {
            String[] alternativePackages = {
                    "com.google.android.apps.healthdata",
                    "com.google.android.healthconnect.service",
                    "com.android.healthconnect.controller"
            };

            for (String pkg : alternativePackages) {
                if (isPackageInstalled(pkg)) {
                    Log.d("RealWatchSetupActivity", "Found Health Connect with alternative package: " + pkg);
                    healthConnectInstalled = true;
                    break;
                }
            }
        }

        if (!samsungHealthInstalled) {
            String[] alternativePackages = {
                    "com.sec.android.app.shealth",
                    "com.samsung.health"
            };

            for (String pkg : alternativePackages) {
                if (isPackageInstalled(pkg)) {
                    Log.d("RealWatchSetupActivity", "Found Samsung Health with alternative package: " + pkg);
                    samsungHealthInstalled = true;
                    break;
                }
            }
        }

        return healthConnectInstalled && samsungHealthInstalled;
    }

    private void updateSetupStatusUI(boolean hasPermissions, boolean hasApps) {
        // Update permission status
        if (hasPermissions) {
            permissionStatusText.setText("✅ All permissions granted");
            permissionStatusText.setTextColor(getColor(R.color.connected_green));
            requestPermissionsButton.setEnabled(false);
        } else {
            permissionStatusText.setText("❌ Missing required permissions");
            permissionStatusText.setTextColor(getColor(R.color.disconnected_red));
            requestPermissionsButton.setEnabled(true);
        }

        // Update apps status
        if (hasApps) {
            appsStatusText.setText("✅ All required apps installed");
            appsStatusText.setTextColor(getColor(R.color.connected_green));
            installAppsButton.setEnabled(false);
        } else {
            appsStatusText.setText("❌ Missing required apps");
            appsStatusText.setTextColor(getColor(R.color.disconnected_red));
            installAppsButton.setEnabled(true);
        }
    }

    private void checkWatchSetup() {
        showLoading(true);
        statusSummaryText.setText("🔍 Checking Samsung Galaxy Watch 7 setup...");

        // Check setup status
        SamsungWatchSetupChecker.checkCompleteSetup(this)
                .thenAccept(status -> {
                    runOnUiThread(() -> {
                        this.setupStatus = status;
                        updateSetupStatusUI(status);
                        checkPermissions();
                    });
                })
                .exceptionally(throwable -> {
                    runOnUiThread(() -> {
                        showLoading(false);
                        statusSummaryText.setText("❌ Setup check failed");
                        statusSummaryText.setTextColor(getColor(R.color.disconnected_red));
                        showError("Failed to check Samsung Galaxy Watch 7 setup");
                    });
                    return null;
                });
    }

    private void checkPermissions() {
        SamsungWatchPermissions.checkAllPermissions(this)
                .thenAccept(status -> {
                    runOnUiThread(() -> {
                        this.permissionStatus = status;
                        updatePermissionStatusUI(status);
                        updateOverallStatus();
                        showLoading(false);
                    });
                })
                .exceptionally(throwable -> {
                    runOnUiThread(() -> {
                        showLoading(false);
                        showError("Failed to check permissions: " + throwable.getMessage());
                    });
                    return null;
                });
    }

    private void updateSetupStatusUI(SamsungWatchSetupChecker.WatchSetupStatus status) {
        StringBuilder statusText = new StringBuilder();

        if (status.isFullyReady) {
            statusText.append("✅ Samsung Galaxy Watch 7 setup complete\n\n");
            statusSummaryText.setTextColor(getColor(R.color.connected_green));
        } else {
            statusText.append("⚠️ Setup incomplete:\n");
            statusText.append("• Missing: ").append(status.missingComponents.size()).append(" components\n");
            statusText.append("• Actions needed: ").append(status.requiredActions.size()).append("\n\n");
            statusSummaryText.setTextColor(getColor(R.color.away_orange));
        }

        // Add ready components
        statusText.append("Ready components:\n");
        for (String ready : status.readyComponents) {
            statusText.append("• ").append(ready).append("\n");
        }

        if (!status.missingComponents.isEmpty()) {
            statusText.append("\nMissing components:\n");
            for (String missing : status.missingComponents) {
                statusText.append("• ").append(missing).append("\n");
            }
        }

        statusSummaryText.setText(statusText.toString());
    }

    private void updatePermissionStatusUI(SamsungWatchPermissions.PermissionStatus status) {
        // Update status summary with permission info
        String currentText = statusSummaryText.getText().toString();
        StringBuilder permissionText = new StringBuilder(currentText);

        permissionText.append("\n\nPermissions:\n");
        if (status.allGranted) {
            permissionText.append("✅ All permissions granted (").append(status.grantedPermissions.size()).append(")\n");
        } else {
            permissionText.append("❌ Missing permissions: ").append(status.deniedPermissions.size()).append("\n");
            for (String denied : status.deniedPermissions) {
                permissionText.append("• ").append(denied).append("\n");
            }
        }

        statusSummaryText.setText(permissionText.toString());
    }

    private void updateOverallStatus() {
        boolean setupReady = setupStatus != null && setupStatus.isFullyReady;
        boolean permissionsReady = permissionStatus != null && permissionStatus.allGranted;
        boolean fullyReady = setupReady && permissionsReady;

        if (fullyReady) {
            connectWatchButton.setEnabled(true);
            requestPermissionsButton.setEnabled(false);
            requestPermissionsButton.setText("✅ Permissions Granted");
        } else if (permissionsReady && !setupReady) {
            connectWatchButton.setEnabled(true); // Can still try to connect
            requestPermissionsButton.setEnabled(false);
            requestPermissionsButton.setText("✅ Permissions Granted");
        } else if (!permissionsReady) {
            connectWatchButton.setEnabled(false);
            requestPermissionsButton.setEnabled(true);
            requestPermissionsButton.setText("Grant Required Permissions");
        }

        // Update implementation info
        if (watchManager != null) {
            String implInfo = WatchManagerFactory.getImplementationInfo(watchManager);
            implementationInfoText.setText("Implementation: " + implInfo);
            implementationInfoText.setVisibility(View.VISIBLE);
        }
    }

    private void requestAllPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();

        // Check basic permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.BODY_SENSORS);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACTIVITY_RECOGNITION);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        // Add background location separately (requires special handling)
        boolean needsBackgroundLocation = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                needsBackgroundLocation = true;
            }
        }

        // Basic Bluetooth permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.BLUETOOTH);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_ADMIN);
        }

        // Android 12+ Bluetooth permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT);
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN);
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_ADVERTISE);
            }
        }

        // Request normal permissions first
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_CODE_ALL_PERMISSIONS);
        }

        // Request background location separately with explanation
        if (needsBackgroundLocation) {
            new AlertDialog.Builder(this)
                    .setTitle("Background Location Permission")
                    .setMessage("The app needs background location permission to track your " +
                            "location even when the app is not in use. This is essential for " +
                            "providing continuous monitoring.")
                    .setPositiveButton("Grant Permission", (dialog, which) -> {
                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                                REQUEST_CODE_BACKGROUND_LOCATION);
                    })
                    .setNegativeButton("Deny", null)
                    .show();
        }

        // If all permissions are already granted
        if (permissionsToRequest.isEmpty() && !needsBackgroundLocation) {
            Toast.makeText(this, "All permissions already granted!", Toast.LENGTH_SHORT).show();
            checkInitialStatus(); // Refresh the UI
        }
    }

    private void installRequiredApps() {
        // Check if Health Connect is installed and up to date
        boolean healthConnectInstalled = isPackageInstalled(HEALTH_CONNECT_PACKAGE);

        // Check if Samsung Health is installed
        boolean samsungHealthInstalled = isPackageInstalled(SAMSUNG_HEALTH_PACKAGE);

        // Build alert message based on what's missing
        StringBuilder message = new StringBuilder("To use Samsung Galaxy Watch 7, you need to:\n\n");
        boolean appsNeeded = false;

        if (!healthConnectInstalled) {
            message.append("• Install Health Connect from Play Store\n");
            appsNeeded = true;
        }

        if (!samsungHealthInstalled) {
            message.append("• Install Samsung Health from Galaxy Store\n");
            appsNeeded = true;
        }

        if (appsNeeded) {
            message.append("\nDo you want to install the missing apps now?");

            // Show alert dialog
            new AlertDialog.Builder(this)
                    .setTitle("Required Apps")
                    .setMessage(message.toString())
                    .setPositiveButton("Install Apps", (dialog, which) -> {
                        // Open Play Store for Health Connect if needed
                        if (!healthConnectInstalled) {
                            openPlayStore(HEALTH_CONNECT_PACKAGE);
                        }

                        // Delay a bit and then open Galaxy Store for Samsung Health if needed
                        if (!samsungHealthInstalled) {
                            new Handler().postDelayed(() ->
                                    openPlayStore(SAMSUNG_HEALTH_PACKAGE), 2000);
                        }
                    })
                    .setNegativeButton("Later", null)
                    .show();
        } else {
            Toast.makeText(this, "All required apps are already installed!",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void connectToWatch() {
        // First check if we have all prerequisites
        if (!checkPrerequisites()) {
            return;
        }

        // Show connecting dialog
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage("Connecting to Samsung Galaxy Watch 7...");
        dialog.setCancelable(false);
        dialog.show();

        // Create watch manager
        if (watchManager == null) {
            watchManager = WatchManagerFactory.createRealSamsungHealthManager(this);
        }

        // Connect to watch
        watchManager.connect()
                .thenAccept(connected -> {
                    runOnUiThread(() -> {
                        dialog.dismiss();
                        if (connected) {
                            Toast.makeText(this, "Watch connected successfully!",
                                    Toast.LENGTH_SHORT).show();

                            // Enable sensor testing
                            testSensorsButton.setEnabled(true);

                            // Update UI to show connected state
                            connectionStatusText.setText("✅ Connected to Samsung Galaxy Watch 7");
                            connectionStatusText.setTextColor(getColor(R.color.connected_green));

                            // Update implementation info
                            String implInfo = WatchManagerFactory.getImplementationInfo(watchManager);
                            implementationInfoText.setText("Connected: " + implInfo);
                            implementationInfoText.setVisibility(View.VISIBLE);
                        } else {
                            Toast.makeText(this, "Failed to connect to watch",
                                    Toast.LENGTH_SHORT).show();
                            connectionStatusText.setText("❌ Failed to connect to watch");
                            connectionStatusText.setTextColor(getColor(R.color.disconnected_red));
                        }
                    });
                })
                .exceptionally(e -> {
                    runOnUiThread(() -> {
                        dialog.dismiss();
                        Toast.makeText(this, "Connection error: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                        connectionStatusText.setText("❌ Connection error");
                        connectionStatusText.setTextColor(getColor(R.color.disconnected_red));
                    });
                    return null;
                });
    }

    private void checkHealthConnectClientStatus() {
        try {
            int sdkStatus = HealthConnectClient.getSdkStatus(this);
            Log.d("RealWatchSetupActivity", "Health Connect SDK Status: " + sdkStatus);

            switch (sdkStatus) {
                case HealthConnectClient.SDK_AVAILABLE:
                    Log.d("RealWatchSetupActivity", "Health Connect is AVAILABLE");
                    break;
                case HealthConnectClient.SDK_UNAVAILABLE:
                    Log.d("RealWatchSetupActivity", "Health Connect is UNAVAILABLE");
                    break;
                case HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED:
                    Log.d("RealWatchSetupActivity", "Health Connect needs UPDATE");
                    break;
                default:
                    Log.d("RealWatchSetupActivity", "Unknown Health Connect status: " + sdkStatus);
            }
        } catch (Exception e) {
            Log.e("RealWatchSetupActivity", "Error checking Health Connect status", e);
        }
    }

    private boolean checkPrerequisites() {
        // Check permissions
        boolean hasAllPermissions =
                ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) ==
                        PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) ==
                                PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                                PackageManager.PERMISSION_GRANTED;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasAllPermissions = hasAllPermissions &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) ==
                            PackageManager.PERMISSION_GRANTED;
        }

        if (!hasAllPermissions) {
            Toast.makeText(this, "Please grant all required permissions first",
                    Toast.LENGTH_LONG).show();
            requestAllPermissions();
            return false;
        }

        // Check required apps
        boolean healthConnectInstalled = isPackageInstalled(HEALTH_CONNECT_PACKAGE);
        boolean samsungHealthInstalled = isPackageInstalled(SAMSUNG_HEALTH_PACKAGE);

        if (!healthConnectInstalled || !samsungHealthInstalled) {
            Toast.makeText(this, "Please install all required apps first",
                    Toast.LENGTH_LONG).show();
            installRequiredApps();
            return false;
        }

        return true;
    }

    private void testSensors() {
        if (watchManager == null || !watchManager.isConnected()) {
            Toast.makeText(this, "Samsung Galaxy Watch 7 not connected",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);
        statusSummaryText.setText("📊 Testing REAL data collection from Samsung Galaxy Watch 7...");

        // Get supported sensors
        watchManager.getSupportedSensors()
                .thenCompose(sensors -> {
                    // Read data from first 5 sensors
                    return watchManager.readSensorData(
                            sensors.subList(0, Math.min(5, sensors.size())));
                })
                .thenAccept(readings -> {
                    runOnUiThread(() -> {
                        showLoading(false);

                        if (readings.isEmpty()) {
                            statusSummaryText.setText("⚠️ No sensor data received");
                            showError("No sensor data available");
                        } else {
                            statusSummaryText.setText("✅ Received " + readings.size() +
                                    " REAL sensor readings!");
                            statusSummaryText.setTextColor(getColor(R.color.connected_green));

                            // Show readings in dialog
                            showSensorReadingsDialog(readings);
                            showSuccess("REAL data collection successful!");
                        }
                    });
                })
                .exceptionally(throwable -> {
                    runOnUiThread(() -> {
                        showLoading(false);
                        statusSummaryText.setText("❌ Data collection failed");
                        showError("Failed to collect data: " + throwable.getMessage());
                    });
                    return null;
                });
    }

    private void showSensorReadingsDialog(List<com.feri.watchmyparent.mobile.domain.valueobjects.SensorReading> readings) {
        StringBuilder message = new StringBuilder("REAL Samsung Galaxy Watch 7 data:\n\n");

        for (com.feri.watchmyparent.mobile.domain.valueobjects.SensorReading reading : readings) {
            message.append(String.format("📊 %s: %.2f %s\n",
                    reading.getSensorType().getDisplayName(),
                    reading.getValue(),
                    reading.getSensorType().getUnit()));
        }

        new AlertDialog.Builder(this)
                .setTitle("REAL Sensor Data")
                .setMessage(message.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    // Helper methods
    private boolean isPackageInstalled(String packageName) {
        try {
            getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void openPlayStore(String packageName) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("market://details?id=" + packageName));
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // Play Store not installed, open browser
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(
                    "https://play.google.com/store/apps/details?id=" + packageName));
            startActivity(intent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_ALL_PERMISSIONS) {
            boolean allGranted = true;

            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                Toast.makeText(this, "All permissions granted!", Toast.LENGTH_SHORT).show();
                // Check watch setup again
                checkWatchSetup();
            } else {
                Toast.makeText(this, "Some permissions were denied. Some features may not work.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }

        // Disable buttons during loading
        if (checkSetupButton != null) {
            checkSetupButton.setEnabled(!show);
        }
        if (requestPermissionsButton != null) {
            requestPermissionsButton.setEnabled(!show &&
                    (permissionStatus == null || !permissionStatus.allGranted));
        }
        if (connectWatchButton != null) {
            connectWatchButton.setEnabled(!show &&
                    (permissionStatus != null && permissionStatus.allGranted));
        }
        if (testSensorsButton != null) {
            testSensorsButton.setEnabled(!show &&
                    (watchManager != null && watchManager.isConnected()));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Cleanup watch manager
        if (watchManager != null && watchManager.isConnected()) {
            watchManager.disconnect();
        }
    }
}