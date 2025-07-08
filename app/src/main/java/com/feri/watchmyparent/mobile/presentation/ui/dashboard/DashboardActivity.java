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
import com.feri.watchmyparent.mobile.infrastructure.utils.HealthConnectChecker;
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

    private DashboardViewModel viewModel;
    private Button connectWatchButton;
    private Button collectDataButton;
    private Button healthConnectButton;
    private TextView connectionStatusText;
    private TextView locationStatusText;
    private TextView healthConnectStatusText;
    private RecyclerView latestSensorsRecyclerView;
    private SensorDataAdapter sensorAdapter;
    private BottomNavigationView bottomNavigation;

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
        checkHealthConnectStatus();
        initializeDemoDataIfNeeded();
        startBackgroundService();

        // Load initial data
        viewModel.loadDashboardData();
    }

    private void initializeViews() {
        connectWatchButton = findViewById(R.id.btn_connect_watch);
        collectDataButton = findViewById(R.id.btn_collect_data);
        healthConnectButton = findViewById(R.id.btn_health_connect);
        connectionStatusText = findViewById(R.id.tv_connection_status);
        locationStatusText = findViewById(R.id.tv_location_status);
        healthConnectStatusText = findViewById(R.id.tv_health_connect_status);
        latestSensorsRecyclerView = findViewById(R.id.rv_latest_sensors);
        bottomNavigation = findViewById(R.id.bottom_navigation);

        connectWatchButton.setOnClickListener(v -> {
            if (viewModel.isWatchConnected()) {
                viewModel.disconnectWatch();
            } else {
                viewModel.connectWatch();
            }
        });

        collectDataButton.setOnClickListener(v -> viewModel.collectSensorData());

        healthConnectButton.setOnClickListener(v -> openHealthConnectSettings());
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

    private void checkHealthConnectStatus() {
        try {
            HealthConnectChecker.HealthConnectStatus status =
                    HealthConnectChecker.checkHealthConnectAvailability(this);

            updateHealthConnectUI(status);

        } catch (Exception e) {
            healthConnectStatusText.setText("❌ Error checking Health Connect");
            healthConnectButton.setVisibility(View.GONE);
        }
    }

    private void updateHealthConnectUI(HealthConnectChecker.HealthConnectStatus status) {
        healthConnectStatusText.setText(status.statusMessage);

        if (!status.isAvailable) {
            if (!status.isInstalled) {
                healthConnectButton.setText("Install Health Connect");
                healthConnectButton.setVisibility(View.VISIBLE);
            } else {
                healthConnectButton.setText("Check Health Connect");
                healthConnectButton.setVisibility(View.VISIBLE);
            }
        } else {
            healthConnectButton.setVisibility(View.GONE);
        }
    }

    private void openHealthConnectSettings() {
        try {
            Intent intent = HealthConnectChecker.getHealthConnectInstallIntent();
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Cannot open Health Connect", Toast.LENGTH_SHORT).show();
        }
    }

    private void initializeDemoDataIfNeeded() {
        if (demoDataInitializer != null) {
            demoDataInitializer.createDemoUserIfNeeded()
                    .thenAccept(success -> {
                        runOnUiThread(() -> {
                            if (success) {
                                showSuccess("Demo user initialized");
                                viewModel.refreshData(); // Refresh data after user creation
                            } else {
                                showError("Failed to initialize demo user");
                            }
                        });
                    });
        } else {
            // Try to get it from application
            WatchMyParentApplication app = (WatchMyParentApplication) getApplication();
            app.retryDemoDataInitialization();
        }
    }

    private void startBackgroundService() {
        Intent serviceIntent = new Intent(this, WatchDataCollectionService.class);
        startService(serviceIntent);
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
            connectWatchButton.setText("Disconnect Watch");
            connectWatchButton.setBackgroundTintList(getColorStateList(R.color.disconnect_red));
            connectionStatusText.setText("Connected: " + status.getDeviceName() + " (Simulated)");
            connectionStatusText.setTextColor(getColor(R.color.connected_green));
            collectDataButton.setEnabled(true);
        } else {
            connectWatchButton.setText("Connect Watch");
            connectWatchButton.setBackgroundTintList(getColorStateList(R.color.connect_blue));
            connectionStatusText.setText("Disconnected");
            connectionStatusText.setTextColor(getColor(R.color.disconnected_red));
            collectDataButton.setEnabled(false);
        }
    }

    private void updateLocationUI(com.feri.watchmyparent.mobile.application.dto.LocationDataDTO location) {
        if (location.isAtHome()) {
            locationStatusText.setText("Status: HOME\n" + location.getAddress());
            locationStatusText.setTextColor(getColor(R.color.home_green));
        } else {
            locationStatusText.setText("Status: " + location.getStatus() + "\n" +
                    location.getFormattedCoordinates() + "\n" + location.getAddress());
            locationStatusText.setTextColor(getColor(R.color.away_orange));
        }
    }

    @Override
    protected void showLoading(boolean show) {
        View progressBar = findViewById(R.id.progress_bar);
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }

        // Disable buttons during loading
        connectWatchButton.setEnabled(!show);
        if (collectDataButton != null) {
            collectDataButton.setEnabled(!show && viewModel.isWatchConnected());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.refreshData();
        checkHealthConnectStatus(); // Check again in case user installed Health Connect
    }
}