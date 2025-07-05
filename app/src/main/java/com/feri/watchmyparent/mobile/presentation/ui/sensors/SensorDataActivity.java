package com.feri.watchmyparent.mobile.presentation.ui.sensors;

import android.os.Bundle;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import com.feri.watchmyparent.mobile.R;
import com.feri.watchmyparent.mobile.presentation.ui.common.BaseActivity;
import com.feri.watchmyparent.mobile.presentation.adapters.SensorCardAdapter;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SensorDataActivity extends BaseActivity {

    private SensorDataViewModel viewModel;
    private RecyclerView sensorsRecyclerView;
    private SensorCardAdapter sensorCardAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_data);

        viewModel = new ViewModelProvider(this).get(SensorDataViewModel.class);

        setupToolbar((Toolbar) findViewById(R.id.toolbar), "Sensor Data", true);
        initializeViews();
        setupRecyclerView();
        observeViewModel();

        viewModel.loadSensorData();
    }

    private void initializeViews() {
        sensorsRecyclerView = findViewById(R.id.rv_sensors);
    }

    private void setupRecyclerView() {
        sensorCardAdapter = new SensorCardAdapter(
                sensor -> viewModel.toggleSensorEnabled(sensor),
                (sensor, frequency) -> viewModel.updateSensorFrequency(sensor, frequency)
        );

        sensorsRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        sensorsRecyclerView.setAdapter(sensorCardAdapter);
    }

    private void observeViewModel() {
        viewModel.getSensorConfigurations().observe(this, configs -> {
            if (configs != null) {
                sensorCardAdapter.updateConfigurations(configs);
            }
        });

        viewModel.getSensorData().observe(this, data -> {
            if (data != null) {
                sensorCardAdapter.updateSensorData(data);
            }
        });

        viewModel.getIsLoading().observe(this, this::showLoading);
        viewModel.getError().observe(this, this::showError);
        viewModel.getSuccess().observe(this, this::showSuccess);
    }
}
