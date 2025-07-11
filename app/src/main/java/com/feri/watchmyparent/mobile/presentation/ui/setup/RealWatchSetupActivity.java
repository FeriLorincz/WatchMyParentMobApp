package com.feri.watchmyparent.mobile.presentation.ui.setup;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.feri.watchmyparent.mobile.R;
import com.feri.watchmyparent.mobile.infrastructure.utils.SamsungWatchPermissions;
import com.feri.watchmyparent.mobile.infrastructure.utils.SamsungWatchSetupChecker;
import com.feri.watchmyparent.mobile.infrastructure.watch.RealSamsungHealthManager;
import com.feri.watchmyparent.mobile.infrastructure.watch.WatchManager;
import com.feri.watchmyparent.mobile.infrastructure.watch.WatchManagerFactory;
import com.feri.watchmyparent.mobile.presentation.ui.common.BaseActivity;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * ✅ REAL Setup Activity pentru Samsung Galaxy Watch 7
 * Ghidează utilizatorul prin procesul complet de configurare
 */
@AndroidEntryPoint
public class RealWatchSetupActivity extends BaseActivity {

    private static final String TAG = "RealWatchSetupActivity";

    // UI Components
    private TextView statusSummaryText;
    private TextView implementationInfoText;
    private RecyclerView setupStatusRecyclerView;
    private RecyclerView permissionStatusRecyclerView;
    private Button checkSetupButton;
    private Button requestPermissionsButton;
    private Button connectWatchButton;
    private Button testDataButton;
    private ProgressBar progressBar;

    // Adapters
    private SetupStatusAdapter setupStatusAdapter;
    private PermissionStatusAdapter permissionStatusAdapter;

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
        setupRecyclerViews();
        setupClickListeners();

        // Start initial setup check
        checkCompleteSetup();
    }

    private void initializeViews() {
        statusSummaryText = findViewById(R.id.tv_status_summary);
        implementationInfoText = findViewById(R.id.tv_implementation_info);
        setupStatusRecyclerView = findViewById(R.id.rv_setup_status);
        permissionStatusRecyclerView = findViewById(R.id.rv_permission_status);
        checkSetupButton = findViewById(R.id.btn_check_setup);
        requestPermissionsButton = findViewById(R.id.btn_request_permissions);
        connectWatchButton = findViewById(R.id.btn_connect_watch);
        testDataButton = findViewById(R.id.btn_test_data);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void setupRecyclerViews() {
        // Setup status list
        setupStatusAdapter = new SetupStatusAdapter();
        setupStatusRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        setupStatusRecyclerView.setAdapter(setupStatusAdapter);

        // Permission status list
        permissionStatusAdapter = new PermissionStatusAdapter();
        permissionStatusRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        permissionStatusRecyclerView.setAdapter(permissionStatusAdapter);
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
                        showError("Failed to check setup: " + throwable.getMessage());
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
        // Update setup status list
        List<SetupStatusItem> setupItems = new ArrayList<>();

        for (String ready : status.readyComponents) {
            setupItems.add(new SetupStatusItem(ready, true, null));
        }

        for (String missing : status.missingComponents) {
            setupItems.add(new SetupStatusItem(missing, false, "Required for full functionality"));
        }

        for (String action : status.requiredActions) {
            setupItems.add(new SetupStatusItem("⚠️ " + action, false, "Action needed"));
        }

        setupStatusAdapter.updateItems(setupItems);
    }

    private void updatePermissionStatusUI(SamsungWatchPermissions.PermissionStatus status) {
        // Update permission status list
        List<PermissionStatusItem> permissionItems = new ArrayList<>();

        for (String granted : status.grantedPermissions) {
            permissionItems.add(new PermissionStatusItem("✅ " + granted, true));
        }

        for (String denied : status.deniedPermissions) {
            permissionItems.add(new PermissionStatusItem("❌ " + denied, false));
        }

        permissionStatusAdapter.updateItems(permissionItems);
    }

    private void updateOverallStatus() {
        boolean setupReady = setupStatus != null && setupStatus.isFullyReady;
        boolean permissionsReady = permissionStatus != null && permissionStatus.allGranted;
        boolean fullyReady = setupReady && permissionsReady;

        if (fullyReady) {
            statusSummaryText.setText("🎉 Samsung Galaxy Watch 7 is ready for REAL data collection!");
            statusSummaryText.setTextColor(getColor(R.color.connected_green));
            connectWatchButton.setEnabled(true);
            requestPermissionsButton.setEnabled(false);
        } else if (permissionsReady && !setupReady) {
            statusSummaryText.setText("⚠️ Permissions OK, but some setup components missing");
            statusSummaryText.setTextColor(getColor(R.color.away_orange));
            connectWatchButton.setEnabled(true); // Can still try to connect
            requestPermissionsButton.setEnabled(false);
        } else if (!permissionsReady) {
            statusSummaryText.setText("❌ Permissions required for Samsung Galaxy Watch 7");
            statusSummaryText.setTextColor(getColor(R.color.disconnected_red));
            connectWatchButton.setEnabled(false);
            requestPermissionsButton.setEnabled(true);
        } else {
            statusSummaryText.setText("🔄 Checking Samsung Galaxy Watch 7 status...");
            statusSummaryText.setTextColor(getColor(R.color.away_orange));
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
                    List<com.feri.watchmyparent.mobile.domain.enums.SensorType> testSensors =
                            sensors.subList(0, Math.min(5, sensors.size()));
                    return watchManager.readSensorData(testSensors);
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

    @Override
    protected void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
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
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);

        // Disable buttons during loading
        checkSetupButton.setEnabled(!show);
        requestPermissionsButton.setEnabled(!show &&
                (permissionStatus == null || !permissionStatus.allGranted));
        connectWatchButton.setEnabled(!show &&
                (permissionStatus != null && permissionStatus.allGranted));
        testDataButton.setEnabled(!show &&
                (watchManager != null && watchManager.isConnected()));
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

    // Data classes for adapters
    public static class SetupStatusItem {
        public final String title;
        public final boolean isReady;
        public final String subtitle;

        public SetupStatusItem(String title, boolean isReady, String subtitle) {
            this.title = title;
            this.isReady = isReady;
            this.subtitle = subtitle;
        }
    }

    public static class PermissionStatusItem {
        public final String title;
        public final boolean isGranted;

        public PermissionStatusItem(String title, boolean isGranted) {
            this.title = title;
            this.isGranted = isGranted;
        }
    }

    // Placeholder adapters (would need full implementation)
    private static class SetupStatusAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private List<SetupStatusItem> items = new ArrayList<>();

        void updateItems(List<SetupStatusItem> items) {
            this.items = items;
            notifyDataSetChanged();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            // Implementation needed
            return null;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            // Implementation needed
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    private static class PermissionStatusAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private List<PermissionStatusItem> items = new ArrayList<>();

        void updateItems(List<PermissionStatusItem> items) {
            this.items = items;
            notifyDataSetChanged();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            // Implementation needed
            return null;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            // Implementation needed
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }
}
