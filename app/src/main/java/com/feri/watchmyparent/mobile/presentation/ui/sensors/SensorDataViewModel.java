package com.feri.watchmyparent.mobile.presentation.ui.sensors;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.feri.watchmyparent.mobile.application.dto.SensorConfigurationDTO;
import com.feri.watchmyparent.mobile.application.dto.SensorDataDTO;
import com.feri.watchmyparent.mobile.application.services.HealthDataApplicationService;
import com.feri.watchmyparent.mobile.presentation.ui.common.BaseViewModel;
import timber.log.Timber;

import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.util.List;

@HiltViewModel
public class SensorDataViewModel extends BaseViewModel {

    private final HealthDataApplicationService healthDataService;

    private final MutableLiveData<List<SensorConfigurationDTO>> _sensorConfigurations = new MutableLiveData<>();
    private final MutableLiveData<List<SensorDataDTO>> _sensorData = new MutableLiveData<>();

    private String currentUserId = "demo-user-id";

    @Inject
    public SensorDataViewModel(HealthDataApplicationService healthDataService) {
        this.healthDataService = healthDataService;
    }

    public LiveData<List<SensorConfigurationDTO>> getSensorConfigurations() { return _sensorConfigurations; }
    public LiveData<List<SensorDataDTO>> getSensorData() { return _sensorData; }

    public void loadSensorData() {
        setLoading(true);

        // Load sensor configurations
        healthDataService.getUserSensorConfigurations(currentUserId)
                .thenAccept(configs -> {
                    _sensorConfigurations.postValue(configs);
                    loadLatestSensorReadings();
                })
                .exceptionally(throwable -> {
                    Timber.e(throwable, "Error loading sensor configurations");
                    setError("Failed to load sensor configurations");
                    return null;
                });
    }

    private void loadLatestSensorReadings() {
        healthDataService.getLatestSensorData(currentUserId)
                .thenAccept(data -> {
                    _sensorData.postValue(data);
                    setLoading(false);
                })
                .exceptionally(throwable -> {
                    Timber.e(throwable, "Error loading sensor data");
                    setError("Failed to load sensor data");
                    return null;
                });
    }

    public void updateSensorFrequency(SensorConfigurationDTO config, int newFrequency) {
        config.setFrequencySeconds(newFrequency);

        healthDataService.updateSensorConfiguration(currentUserId, config)
                .thenAccept(updatedConfig -> {
                    setSuccess("Frequency updated for " + config.getDisplayName());
                    loadSensorData(); // Refresh data
                })
                .exceptionally(throwable -> {
                    Timber.e(throwable, "Error updating sensor frequency");
                    setError("Failed to update sensor frequency");
                    return null;
                });
    }

    public void toggleSensorEnabled(SensorConfigurationDTO config) {
        config.setEnabled(!config.isEnabled());

        healthDataService.updateSensorConfiguration(currentUserId, config)
                .thenAccept(updatedConfig -> {
                    String message = config.isEnabled() ? "enabled" : "disabled";
                    setSuccess(config.getDisplayName() + " " + message);
                    loadSensorData(); // Refresh data
                })
                .exceptionally(throwable -> {
                    Timber.e(throwable, "Error toggling sensor");
                    setError("Failed to update sensor");
                    return null;
                });
    }
}
