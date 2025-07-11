package com.feri.watchmyparent.mobile.presentation.ui.setup;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import com.feri.watchmyparent.mobile.R;
import com.feri.watchmyparent.mobile.infrastructure.utils.SamsungWatchPermissions;
import com.feri.watchmyparent.mobile.infrastructure.utils.SamsungWatchSetupChecker;
import com.feri.watchmyparent.mobile.infrastructure.watch.WatchManager;
import com.feri.watchmyparent.mobile.infrastructure.watch.WatchManagerFactory;
import com.feri.watchmyparent.mobile.presentation.ui.common.BaseActivity;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * ✅ SIMPLIFIED Setup Activity pentru Samsung Galaxy Watch 7
 * Versiune simplificată pentru a evita problemele de build
 */
@AndroidEntryPoint
public class RealWatchSetupActivity extends BaseActivity {

    private static final String TAG = "RealWatchSetupActivity";

    // UI Components
    private TextView statusSummaryText;
    private TextView implementationInfoText;
    private Button checkSetupButton;
    private Button requestPermissionsButton;
    private Button connectWatchButton;
    private Button testDataButton;
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

        // Start initial setup check
        checkCompleteSetup();
    }

    private void initializeViews() {
        statusSummaryText = findViewById(R.id.tv_status_summary);
        implementationInfoText = findViewById(R.id.tv_implementation_info);
        checkSetupButton = findViewById(R.id.btn_check_setup);
        requestPermissionsButton = findViewById(R.id.btn_request_permissions);
        connectWatchButton = findViewById(R.id.btn_connect_watch);
        testDataButton = findViewById(R.id.btn_test_data);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void setupClickListeners() {
        checkSetupButton.setOnClickListener(v -> checkCompleteSetup());
        requestPermissionsButton.setOnClickListener(v -> requestAllPermissions());
        connectWatchButton.setOnClickListener(v -> connectToWatch());
        testDataButton.setOnClickListener(v -> testRealDataCollection());
    }

    private void checkCompleteSetup() {
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
        new AlertDialog.Builder(this)
                .setTitle("Permissions Required")
                .setMessage("Samsung Galaxy Watch 7 needs several permissions for REAL data collection:\n\n" +
                        "• Body Sensors (heart rate, etc.)\n" +
                        "• Location (GPS tracking)\n" +
                        "• Bluetooth (watch connection)\n" +
                        "• Activity Recognition\n\n" +
                        "Grant permissions to continue.")
                .setPositiveButton("Grant Permissions", (dialog, which) -> {
                    // Request permissions in sequence
                    SamsungWatchPermissions.requestBasicPermissions(this);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void connectToWatch() {
        showLoading(true);
        statusSummaryText.setText("🔄 Connecting to Samsung Galaxy Watch 7...");

        try {
            // Create REAL Samsung Health Manager
            watchManager = WatchManagerFactory.createRealSamsungHealthManager(this);

            watchManager.connect()
                    .thenAccept(connected -> {
                        runOnUiThread(() -> {
                            showLoading(false);
                            if (connected) {
                                statusSummaryText.setText("✅ Samsung Galaxy Watch 7 connected successfully!");
                                statusSummaryText.setTextColor(getColor(R.color.connected_green));
                                connectWatchButton.setText("Disconnect Watch");
                                testDataButton.setEnabled(true);
                                showSuccess("REAL Samsung Galaxy Watch 7 connection established!");

                                // Update implementation info
                                String implInfo = WatchManagerFactory.getImplementationInfo(watchManager);
                                implementationInfoText.setText("Connected: " + implInfo);
                                implementationInfoText.setVisibility(View.VISIBLE);
                            } else {
                                statusSummaryText.setText("❌ Failed to connect to Samsung Galaxy Watch 7");
                                statusSummaryText.setTextColor(getColor(R.color.disconnected_red));
                                showError("Failed to connect to Samsung Galaxy Watch 7");
                            }
                        });
                    })
                    .exceptionally(throwable -> {
                        runOnUiThread(() -> {
                            showLoading(false);
                            statusSummaryText.setText("❌ Connection error");
                            statusSummaryText.setTextColor(getColor(R.color.disconnected_red));
                            showError("Connection failed: " + throwable.getMessage());
                        });
                        return null;
                    });

        } catch (Exception e) {
            showLoading(false);
            showError("Failed to create watch manager: " + e.getMessage());
        }
    }

    private void testRealDataCollection() {
        if (watchManager == null || !watchManager.isConnected()) {
            showError("Samsung Galaxy Watch 7 not connected");
            return;
        }

        showLoading(true);
        statusSummaryText.setText("📊 Testing REAL data collection from Samsung Galaxy Watch 7...");

        // Get supported sensors
        watchManager.getSupportedSensors()
                .thenCompose(sensors -> {
                    // Read data from first 5 sensors
                    return watchManager.readSensorData(sensors.subList(0, Math.min(5, sensors.size())));
                })
                .thenAccept(readings -> {
                    runOnUiThread(() -> {
                        showLoading(false);

                        if (readings.isEmpty()) {
                            statusSummaryText.setText("⚠️ No sensor data received");
                            showError("No sensor data available");
                        } else {
                            statusSummaryText.setText("✅ Received " + readings.size() + " REAL sensor readings!");
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

    private void showSensorReadingsDialog(java.util.List<com.feri.watchmyparent.mobile.domain.valueobjects.SensorReading> readings) {
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        boolean granted = SamsungWatchPermissions.handlePermissionResult(requestCode, permissions, grantResults);

        if (granted) {
            showSuccess("Permissions granted!");
            // Continue with location permissions if basic were granted
            if (requestCode == SamsungWatchPermissions.REQUEST_CODE_BASIC_PERMISSIONS) {
                SamsungWatchPermissions.requestLocationPermissions(this);
            } else if (requestCode == SamsungWatchPermissions.REQUEST_CODE_LOCATION_PERMISSIONS) {
                SamsungWatchPermissions.requestBluetoothPermissions(this);
            } else {
                // All permissions done, recheck status
                checkPermissions();
            }
        } else {
            showError("Some permissions were denied. Samsung Galaxy Watch 7 functionality may be limited.");
            checkPermissions(); // Update UI anyway
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
        if (testDataButton != null) {
            testDataButton.setEnabled(!show &&
                    (watchManager != null && watchManager.isConnected()));
        }
    }

    @Override
    protected void showError(String message) {
        Toast.makeText(this, "❌ " + message, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void showSuccess(String message) {
        Toast.makeText(this, "✅ " + message, Toast.LENGTH_LONG).show();
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