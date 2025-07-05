package com.feri.watchmyparent.mobile.presentation.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.feri.watchmyparent.mobile.infrastructure.services.WatchDataCollectionService;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.feri.watchmyparent.mobile.R;
import com.feri.watchmyparent.mobile.presentation.ui.common.BaseActivity;
import com.feri.watchmyparent.mobile.presentation.ui.sensors.SensorDataActivity;
import com.feri.watchmyparent.mobile.presentation.ui.profile.PersonalDataActivity;
import com.feri.watchmyparent.mobile.presentation.ui.profile.MedicalProfileActivity;
import com.feri.watchmyparent.mobile.presentation.ui.contacts.EmergencyContactsActivity;
import com.feri.watchmyparent.mobile.presentation.adapters.SensorDataAdapter;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class DashboardActivity extends BaseActivity {

    private DashboardViewModel viewModel;
    private Button connectWatchButton;
    private TextView connectionStatusText;
    private TextView locationStatusText;
    private RecyclerView latestSensorsRecyclerView;
    private SensorDataAdapter sensorAdapter;
    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        initializeViews();
        setupRecyclerView();
        setupBottomNavigation();
        observeViewModel();
        startBackgroundService();

        // Load initial data
        viewModel.loadDashboardData();
    }

    private void initializeViews() {
        connectWatchButton = findViewById(R.id.btn_connect_watch);
        connectionStatusText = findViewById(R.id.tv_connection_status);
        locationStatusText = findViewById(R.id.tv_location_status);
        latestSensorsRecyclerView = findViewById(R.id.rv_latest_sensors);
        bottomNavigation = findViewById(R.id.bottom_navigation);

        connectWatchButton.setOnClickListener(v -> {
            if (viewModel.isWatchConnected()) {
                viewModel.disconnectWatch();
            } else {
                viewModel.connectWatch();
            }
        });
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
        viewModel.getError().observe(this, this::showError);
        viewModel.getSuccess().observe(this, this::showSuccess);
    }

    private void updateConnectionUI(com.feri.watchmyparent.mobile.application.dto.WatchConnectionStatusDTO status) {
        if (status.isConnected()) {
            connectWatchButton.setText("Disconnect Watch");
            connectWatchButton.setBackgroundColor(getColor(R.color.disconnect_red));
            connectionStatusText.setText("Connected: " + status.getDeviceName());
            connectionStatusText.setTextColor(getColor(R.color.connected_green));
        } else {
            connectWatchButton.setText("Connect Watch");
            connectWatchButton.setBackgroundColor(getColor(R.color.connect_blue));
            connectionStatusText.setText("Disconnected");
            connectionStatusText.setTextColor(getColor(R.color.disconnected_red));
        }
    }

    private void updateLocationUI(com.feri.watchmyparent.mobile.application.dto.LocationDataDTO location) {
        if (location.isAtHome()) {
            locationStatusText.setText("Status: HOME\n" + location.getAddress());
            locationStatusText.setTextColor(getColor(R.color.home_green));
        } else {
            locationStatusText.setText("Status: AWAY\n" + location.getFormattedCoordinates() + "\n" + location.getAddress());
            locationStatusText.setTextColor(getColor(R.color.away_orange));
        }
    }

    @Override
    protected void showLoading(boolean show) {
        // Show/hide loading indicator
        findViewById(R.id.progress_bar).setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.refreshData();
    }
}
