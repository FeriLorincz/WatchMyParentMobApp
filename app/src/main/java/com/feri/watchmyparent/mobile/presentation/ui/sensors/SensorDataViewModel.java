package com.feri.watchmyparent.mobile.presentation.ui.sensors;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.feri.watchmyparent.mobile.application.dto.SensorConfigurationDTO;
import com.feri.watchmyparent.mobile.application.dto.SensorDataDTO;
import com.feri.watchmyparent.mobile.application.services.HealthDataApplicationService;
import com.feri.watchmyparent.mobile.presentation.ui.common.BaseViewModel;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.util.List;

@HiltViewModel
public class SensorDataViewModel extends BaseViewModel {

    private static final String TAG = "SensorDataViewModel";

    private final HealthDataApplicationService healthDataService;

    private final MutableLiveData<List<SensorConfigurationDTO>> _sensorConfigurations = new MutableLiveData<>();
    private final MutableLiveData<List<SensorDataDTO>> _sensorData = new MutableLiveData<>();

    private String currentUserId = "demo-user-id";

    @Inject
    public SensorDataViewModel(HealthDataApplicationService healthDataService) {
        this.healthDataService = healthDataService;
        Log.d(TAG, "✅ SensorDataViewModel initialized");
    }

    // LiveData getters
    public LiveData<List<SensorConfigurationDTO>> getSensorConfigurations() {
        return _sensorConfigurations;
    }

    public LiveData<List<SensorDataDTO>> getSensorData() {
        return _sensorData;
    }

    // CORECTAT: Load sensor data cu error handling proper
    public void loadSensorData() {
        Log.d(TAG, "🔄 Loading sensor data for user: " + currentUserId);
        setLoading(true);

        // Load sensor configurations
        healthDataService.getUserSensorConfigurations(currentUserId)
                .thenAccept(configs -> {
                    Log.d(TAG, "✅ Loaded " + configs.size() + " sensor configurations");
                    _sensorConfigurations.postValue(configs);
                    loadLatestSensorReadings();
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "❌ Error loading sensor configurations: " + throwable.getMessage(), throwable);
                    setError("Failed to load sensor configurations: " + throwable.getMessage());
                    setLoading(false);
                    return null;
                });
    }

    //CORECTAT: Load latest sensor readings cu error handling proper
    private void loadLatestSensorReadings() {
        Log.d(TAG, "📊 Loading latest sensor readings...");

        healthDataService.getLatestSensorData(currentUserId)
                .thenAccept(data -> {
                    Log.d(TAG, "✅ Loaded " + data.size() + " sensor readings");
                    _sensorData.postValue(data);
                    setLoading(false);
                    setSuccess("Sensor data loaded successfully");
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "❌ Error loading sensor data: " + throwable.getMessage(), throwable);
                    setError("Failed to load sensor data: " + throwable.getMessage());
                    setLoading(false);
                    return null;
                });
    }

    // CORECTAT: Update sensor frequency cu validare
    public void updateSensorFrequency(SensorConfigurationDTO config, int newFrequency) {
        Log.d(TAG, "⚙️ Updating frequency for " + config.getDisplayName() + " to " + newFrequency + "s");

        // Validate frequency
        if (newFrequency < config.getMinFrequency()) {
            setError("Frequency too low. Minimum: " + config.getMinFrequency() + " seconds");
            return;
        }

        if (newFrequency > config.getMaxFrequency()) {
            setError("Frequency too high. Maximum: " + config.getMaxFrequency() + " seconds");
            return;
        }

        setLoading(true);
        config.setFrequencySeconds(newFrequency);

        healthDataService.updateSensorConfiguration(currentUserId, config)
                .thenAccept(updatedConfig -> {
                    Log.d(TAG, "✅ Frequency updated successfully for " + config.getDisplayName());
                    setSuccess("Frequency updated for " + config.getDisplayName() + " to " +
                            updatedConfig.getFormattedFrequency());
                    loadSensorData(); // Refresh data
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "❌ Error updating sensor frequency: " + throwable.getMessage(), throwable);
                    setError("Failed to update sensor frequency: " + throwable.getMessage());
                    setLoading(false);
                    return null;
                });
    }

    // CORECTAT: Toggle sensor enabled/disabled cu feedback proper
    public void toggleSensorEnabled(SensorConfigurationDTO config) {
        boolean newState = !config.isEnabled();
        String action = newState ? "Enabling" : "Disabling";

        Log.d(TAG, "🔧 " + action + " sensor: " + config.getDisplayName());

        setLoading(true);
        config.setEnabled(newState);

        healthDataService.updateSensorConfiguration(currentUserId, config)
                .thenAccept(updatedConfig -> {
                    String status = updatedConfig.isEnabled() ? "enabled" : "disabled";
                    Log.d(TAG, "✅ Sensor " + status + " successfully: " + config.getDisplayName());
                    setSuccess(config.getDisplayName() + " " + status);
                    loadSensorData(); // Refresh data
                })
                .exceptionally(throwable -> {
                    // Revert the change if failed
                    config.setEnabled(!newState);

                    Log.e(TAG, "❌ Error toggling sensor: " + throwable.getMessage(), throwable);
                    setError("Failed to update sensor: " + throwable.getMessage());
                    setLoading(false);
                    return null;
                });
    }

    // ADĂUGAT: Refresh data manual
    public void refreshData() {
        Log.d(TAG, "🔄 Manual refresh requested");
        loadSensorData();
    }

    // ADĂUGAT: Get sensor configuration by type
    public void loadSensorConfiguration(com.feri.watchmyparent.mobile.domain.enums.SensorType sensorType) {
        Log.d(TAG, "📋 Loading configuration for sensor: " + sensorType.getDisplayName());
        setLoading(true);

        healthDataService.getSensorConfiguration(currentUserId, sensorType)
                .thenAccept(config -> {
                    Log.d(TAG, "✅ Configuration loaded for " + config.getDisplayName());
                    // Add to current configurations or handle as needed
                    setLoading(false);
                    setSuccess("Configuration loaded for " + config.getDisplayName());
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "❌ Error loading sensor configuration: " + throwable.getMessage(), throwable);
                    setError("Failed to load sensor configuration: " + throwable.getMessage());
                    setLoading(false);
                    return null;
                });
    }

    // ADĂUGAT: Quick enable/disable sensor
    public void setSensorEnabled(com.feri.watchmyparent.mobile.domain.enums.SensorType sensorType, boolean enabled) {
        String action = enabled ? "Enabling" : "Disabling";
        Log.d(TAG, "🔧 " + action + " sensor: " + sensorType.getDisplayName());

        setLoading(true);

        healthDataService.setSensorEnabled(currentUserId, sensorType, enabled)
                .thenAccept(success -> {
                    if (success) {
                        String status = enabled ? "enabled" : "disabled";
                        Log.d(TAG, "✅ Sensor " + status + ": " + sensorType.getDisplayName());
                        setSuccess(sensorType.getDisplayName() + " " + status);
                        loadSensorData(); // Refresh data
                    } else {
                        Log.w(TAG, "⚠️ Failed to change sensor state: " + sensorType.getDisplayName());
                        setError("Failed to update sensor state");
                        setLoading(false);
                    }
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "❌ Error setting sensor enabled: " + throwable.getMessage(), throwable);
                    setError("Failed to update sensor: " + throwable.getMessage());
                    setLoading(false);
                    return null;
                });
    }

    // ADĂUGAT: Update frequency for specific sensor type
    public void updateSensorFrequency(com.feri.watchmyparent.mobile.domain.enums.SensorType sensorType, int frequencySeconds) {
        Log.d(TAG, "⏱️ Updating frequency for " + sensorType.getDisplayName() + " to " + frequencySeconds + "s");

        setLoading(true);

        healthDataService.updateSensorFrequency(currentUserId, sensorType, frequencySeconds)
                .thenAccept(success -> {
                    if (success) {
                        Log.d(TAG, "✅ Frequency updated for " + sensorType.getDisplayName());
                        setSuccess("Frequency updated for " + sensorType.getDisplayName());
                        loadSensorData(); // Refresh data
                    } else {
                        Log.w(TAG, "⚠️ Failed to update frequency for " + sensorType.getDisplayName());
                        setError("Failed to update sensor frequency");
                        setLoading(false);
                    }
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "❌ Error updating frequency: " + throwable.getMessage(), throwable);
                    setError("Failed to update frequency: " + throwable.getMessage());
                    setLoading(false);
                    return null;
                });
    }

    // ADĂUGAT: Clear error/success messages
    public void clearMessages() {
        _success.setValue(null);
        _error.setValue(null);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        Log.d(TAG, "🔚 SensorDataViewModel cleared");
    }
}