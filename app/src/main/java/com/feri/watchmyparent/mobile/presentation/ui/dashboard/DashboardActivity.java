package com.feri.watchmyparent.mobile.presentation.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.feri.watchmyparent.mobile.WatchMyParentApplication;
import com.feri.watchmyparent.mobile.infrastructure.services.WatchDataCollectionService;
import com.feri.watchmyparent.mobile.infrastructure.utils.DemoDataInitializer;
import com.feri.watchmyparent.mobile.infrastructure.utils.SamsungWatchPermissions;
import com.feri.watchmyparent.mobile.infrastructure.utils.SamsungWatchSetupChecker;
import com.feri.watchmyparent.mobile.presentation.ui.setup.RealWatchSetupActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.feri.watchmyparent.mobile.R;
import com.feri.watchmyparent.mobile.presentation.ui.common.BaseActivity;
import com.feri.watchmyparent.mobile.presentation.ui.sensors.SensorDataActivity;
import com.feri.watchmyparent.mobile.presentation.ui.profile.PersonalDataActivity;
import com.feri.watchmyparent.mobile.presentation.ui.profile.MedicalProfileActivity;
import com.feri.watchmyparent.mobile.presentation.ui.contacts.EmergencyContactsActivity;
import com.feri.watchmyparent.mobile.presentation.adapters.SensorDataAdapter;
import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class DashboardActivity extends BaseActivity {

    private static final String TAG = "DashboardActivity";

    private DashboardViewModel viewModel;

    // UI Components
    private Button connectWatchButton;
    private Button collectDataButton;
    private Button testKafkaButton;
    private Button testPostgreSQLButton;
    private Button watchSetupButton; // ✅ NEW: Setup button
    private TextView connectionStatusText;
    private TextView locationStatusText;
    private TextView kafkaStatusText;
    private TextView postgresStatusText;
    private TextView setupStatusText; // ✅ NEW: Setup status
    private RecyclerView latestSensorsRecyclerView;
    private SensorDataAdapter sensorAdapter;
    private BottomNavigationView bottomNavigation;
    private FloatingActionButton fabWatchSetup; // ✅ NEW: Quick setup FAB

    // Real Watch Integration
    private SamsungWatchSetupChecker.WatchSetupStatus setupStatus;
    private SamsungWatchPermissions.PermissionStatus permissionStatus;

    @Inject
    DemoDataInitializer demoDataInitializer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        initializeViews();
        setupRecyclerView();
        setupBottomNavigation();
        observeViewModel();

        // ✅ NEW: Check Samsung Galaxy Watch 7 setup first
        checkSamsungWatchSetup();

        initializeDemoDataIfNeeded();
        startBackgroundServiceIfReady();

        // Load initial data
        viewModel.loadDashboardData();
    }

    private void initializeViews() {
        connectWatchButton = findViewById(R.id.btn_connect_watch);
        collectDataButton = findViewById(R.id.btn_collect_data);
        testKafkaButton = findViewById(R.id.btn_test_kafka);
        testPostgreSQLButton = findViewById(R.id.btn_test_postgresql);
        watchSetupButton = findViewById(R.id.btn_watch_setup); // ✅ NEW
        connectionStatusText = findViewById(R.id.tv_connection_status);
        locationStatusText = findViewById(R.id.tv_location_status);
        kafkaStatusText = findViewById(R.id.tv_kafka_status);
        postgresStatusText = findViewById(R.id.tv_postgres_status);
        setupStatusText = findViewById(R.id.tv_setup_status); // ✅ NEW
        latestSensorsRecyclerView = findViewById(R.id.rv_latest_sensors);
        bottomNavigation = findViewById(R.id.bottom_navigation);
        fabWatchSetup = findViewById(R.id.fab_watch_setup); // ✅ NEW

        // Setup click listeners
        connectWatchButton.setOnClickListener(v -> {
            if (viewModel.isWatchConnected()) {
                viewModel.disconnectWatch();
            } else {
                connectToSamsungWatch();
            }
        });

        collectDataButton.setOnClickListener(v -> viewModel.collectSensorData());
        testKafkaButton.setOnClickListener(v -> viewModel.testKafkaConnection());
        testPostgreSQLButton.setOnClickListener(v -> viewModel.testPostgreSQLConnection());

        // ✅ NEW: Setup button click listener
        watchSetupButton.setOnClickListener(v -> openWatchSetup());
        fabWatchSetup.setOnClickListener(v -> openWatchSetup());
    }

    private void setupRecyclerView() {
        sensorAdapter = new SensorDataAdapter();
        latestSensorsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        latestSensorsRecyclerView.setAdapter(sensorAdapter);
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_personal_data) {
                startActivity(new Intent(this, PersonalDataActivity.class));
                return true;
            } else if (itemId == R.id.nav_medical_profile) {
                startActivity(new Intent(this, MedicalProfileActivity.class));
                return true;
            } else if (itemId == R.id.nav_emergency_contacts) {
                startActivity(new Intent(this, EmergencyContactsActivity.class));
                return true;
            } else if (itemId == R.id.nav_sensor_data) {
                startActivity(new Intent(this, SensorDataActivity.class));
                return true;
            }
            return false;
        });
    }

    // ✅ NEW: Check Samsung Galaxy Watch 7 setup
    private void checkSamsungWatchSetup() {
        setupStatusText.setText("🔍 Checking Samsung Galaxy Watch 7 setup...");

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
                        setupStatusText.setText("❌ Setup check failed");
                        setupStatusText.setTextColor(getColor(R.color.disconnected_red));
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
                        updateOverallReadiness();
                    });
                })
                .exceptionally(throwable -> {
                    runOnUiThread(() -> {
                        showError("Failed to check permissions");
                    });
                    return null;
                });
    }

    private void updateSetupStatusUI(SamsungWatchSetupChecker.WatchSetupStatus status) {
        if (status.isFullyReady) {
            setupStatusText.setText("✅ Samsung Galaxy Watch 7 setup complete");
            setupStatusText.setTextColor(getColor(R.color.connected_green));
        } else {
            int missingCount = status.missingComponents.size();
            int actionCount = status.requiredActions.size();
            setupStatusText.setText("⚠️ Setup incomplete: " + missingCount + " missing, " + actionCount + " actions needed");
            setupStatusText.setTextColor(getColor(R.color.away_orange));
        }
    }

    private void updatePermissionStatusUI(SamsungWatchPermissions.PermissionStatus status) {
        // Update UI based on permission status
        // This is handled in updateOverallReadiness()
    }

    private void updateOverallReadiness() {
        boolean setupReady = setupStatus != null && setupStatus.isFullyReady;
        boolean permissionsReady = permissionStatus != null && permissionStatus.allGranted;
        boolean fullyReady = setupReady && permissionsReady;

        if (fullyReady) {
            connectWatchButton.setEnabled(true);
            connectWatchButton.setText("Connect Samsung Galaxy Watch 7");
            watchSetupButton.setVisibility(View.GONE);
            fabWatchSetup.setVisibility(View.GONE);
            showSuccess("🎉 Samsung Galaxy Watch 7 ready for REAL data collection!");
        } else {
            connectWatchButton.setEnabled(false);
            connectWatchButton.setText("Setup Required");
            watchSetupButton.setVisibility(View.VISIBLE);
            fabWatchSetup.setVisibility(View.VISIBLE);

            if (!permissionsReady) {
                watchSetupButton.setText("Grant Permissions & Setup");
            } else {
                watchSetupButton.setText("Complete Setup");
            }
        }
    }

    private void connectToSamsungWatch() {
        // Check readiness first
        if (setupStatus == null || !setupStatus.isFullyReady ||
                permissionStatus == null || !permissionStatus.allGranted) {

            Toast.makeText(this, "⚠️ Please complete Samsung Galaxy Watch 7 setup first", Toast.LENGTH_LONG).show();
            openWatchSetup();
            return;
        }

        // Proceed with connection
        viewModel.connectWatch();
    }

    private void openWatchSetup() {
        Intent intent = new Intent(this, RealWatchSetupActivity.class);
        startActivity(intent);
    }

    private void initializeDemoDataIfNeeded() {
        if (demoDataInitializer != null) {
            demoDataInitializer.createDemoUserIfNeeded()
                    .thenAccept(success -> {
                        runOnUiThread(() -> {
                            if (success) {
                                showSuccess("Demo user initialized");
                                viewModel.refreshData();
                            } else {
                                showError("Failed to initialize demo user");
                            }
                        });
                    });
        } else {
            WatchMyParentApplication app = (WatchMyParentApplication) getApplication();
            app.retryDemoDataInitialization();
        }
    }

    private void startBackgroundServiceIfReady() {
        // Only start background service if Samsung Galaxy Watch 7 is ready
        if (setupStatus != null && setupStatus.isFullyReady &&
                permissionStatus != null && permissionStatus.allGranted) {

            Intent serviceIntent = new Intent(this, WatchDataCollectionService.class);
            startService(serviceIntent);
            showSuccess("✅ REAL data collection service started");
        } else {
            Toast.makeText(this, "⚠️ Background service disabled - complete setup first", Toast.LENGTH_LONG).show();
        }
    }

    private void observeViewModel() {
        viewModel.getConnectionStatus().observe(this, status -> {
            if (status != null) {
                updateConnectionUI(status);
            }
        });

        viewModel.getLocationStatus().observe(this, location -> {
            if (location != null) {
                updateLocationUI(location);
            }
        });

        viewModel.getLatestSensorData().observe(this, sensorData -> {
            if (sensorData != null) {
                sensorAdapter.updateData(sensorData);
            }
        });

        viewModel.getKafkaStatus().observe(this, status -> {
            if (status != null) {
                updateKafkaUI(status);
            }
        });

        viewModel.getPostgreSQLStatus().observe(this, status -> {
            if (status != null) {
                updatePostgreSQLUI(status);
            }
        });

        viewModel.getIsLoading().observe(this, this::showLoading);
        viewModel.getError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                showError(error);
            }
        });
        viewModel.getSuccess().observe(this, success -> {
            if (success != null && !success.isEmpty()) {
                showSuccess(success);
            }
        });
    }

    private void updateConnectionUI(com.feri.watchmyparent.mobile.application.dto.WatchConnectionStatusDTO status) {
        if (status.isConnected()) {
            connectWatchButton.setText("Disconnect Samsung Galaxy Watch 7");
            connectWatchButton.setBackgroundTintList(getColorStateList(R.color.disconnect_red));
            connectionStatusText.setText("✅ Connected: " + status.getDeviceName() + " (REAL Samsung Galaxy Watch 7)");
            connectionStatusText.setTextColor(getColor(R.color.connected_green));
            collectDataButton.setEnabled(true);

            // Hide setup components when connected
            watchSetupButton.setVisibility(View.GONE);
            fabWatchSetup.setVisibility(View.GONE);
        } else {
            connectWatchButton.setText("Connect Samsung Galaxy Watch 7");
            connectWatchButton.setBackgroundTintList(getColorStateList(R.color.connect_blue));
            connectionStatusText.setText("❌ Disconnected from Samsung Galaxy Watch 7");
            connectionStatusText.setTextColor(getColor(R.color.disconnected_red));
            collectDataButton.setEnabled(false);

            // Show setup components when disconnected
            if (setupStatus == null || !setupStatus.isFullyReady ||
                    permissionStatus == null || !permissionStatus.allGranted) {
                watchSetupButton.setVisibility(View.VISIBLE);
                fabWatchSetup.setVisibility(View.VISIBLE);
            }
        }
    }

    private void updateLocationUI(com.feri.watchmyparent.mobile.application.dto.LocationDataDTO location) {
        if (location.isAtHome()) {
            locationStatusText.setText("🏠 Status: HOME\n" + location.getAddress());
            locationStatusText.setTextColor(getColor(R.color.home_green));
        } else {
            locationStatusText.setText("📍 Status: " + location.getStatus() + "\n" +
                    location.getFormattedCoordinates() + "\n" + location.getAddress());
            locationStatusText.setTextColor(getColor(R.color.away_orange));
        }
    }

    private void updateKafkaUI(String status) {
        kafkaStatusText.setText(status);
        if (status.contains("✅")) {
            kafkaStatusText.setTextColor(getColor(R.color.connected_green));
        } else {
            kafkaStatusText.setTextColor(getColor(R.color.disconnected_red));
        }
    }

    private void updatePostgreSQLUI(String status) {
        postgresStatusText.setText(status);
        if (status.contains("✅")) {
            postgresStatusText.setTextColor(getColor(R.color.connected_green));
        } else {
            postgresStatusText.setTextColor(getColor(R.color.disconnected_red));
        }
    }

    @Override
    protected void showLoading(boolean show) {
        View progressBar = findViewById(R.id.progress_bar);
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }

        // Disable buttons during loading
        connectWatchButton.setEnabled(!show && (setupStatus != null && setupStatus.isFullyReady &&
                permissionStatus != null && permissionStatus.allGranted));
        if (collectDataButton != null) {
            collectDataButton.setEnabled(!show && viewModel.isWatchConnected());
        }
        if (testKafkaButton != null) {
            testKafkaButton.setEnabled(!show);
        }
        if (testPostgreSQLButton != null) {
            testPostgreSQLButton.setEnabled(!show);
        }
        if (watchSetupButton != null) {
            watchSetupButton.setEnabled(!show);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Re-check setup status when returning to activity
        checkSamsungWatchSetup();
        viewModel.refreshData();
    }

    @Override
    protected void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Handle permission results
        boolean granted = SamsungWatchPermissions.handlePermissionResult(requestCode, permissions, grantResults);

        if (granted) {
            showSuccess("Permissions granted!");
            checkPermissions(); // Refresh permission status
        } else {
            showError("Some permissions were denied. Samsung Galaxy Watch 7 functionality may be limited.");
        }
    }
}