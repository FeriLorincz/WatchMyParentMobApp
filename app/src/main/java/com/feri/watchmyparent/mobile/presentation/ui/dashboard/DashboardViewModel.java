package com.feri.watchmyparent.mobile.presentation.ui.dashboard;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.feri.watchmyparent.mobile.application.dto.WatchConnectionStatusDTO;
import com.feri.watchmyparent.mobile.application.dto.LocationDataDTO;
import com.feri.watchmyparent.mobile.application.dto.SensorDataDTO;
import com.feri.watchmyparent.mobile.application.services.WatchConnectionApplicationService;
import com.feri.watchmyparent.mobile.application.services.LocationApplicationService;
import com.feri.watchmyparent.mobile.application.services.HealthDataApplicationService;
import com.feri.watchmyparent.mobile.infrastructure.database.PostgreSQLConfig;
import com.feri.watchmyparent.mobile.infrastructure.services.PostgreSQLDataService;
import com.feri.watchmyparent.mobile.presentation.ui.common.BaseViewModel;
import timber.log.Timber;

import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.util.List;

@HiltViewModel
public class DashboardViewModel extends BaseViewModel {

    private final WatchConnectionApplicationService watchService;
    private final LocationApplicationService locationService;
    private final HealthDataApplicationService healthDataService;

    private final MutableLiveData<WatchConnectionStatusDTO> _connectionStatus = new MutableLiveData<>();
    private final MutableLiveData<LocationDataDTO> _locationStatus = new MutableLiveData<>();
    private final MutableLiveData<List<SensorDataDTO>> _latestSensorData = new MutableLiveData<>();

    private String currentUserId = "demo-user-id"; // In real app, get from session

    @Inject
    public DashboardViewModel(
            WatchConnectionApplicationService watchService,
            LocationApplicationService locationService,
            HealthDataApplicationService healthDataService) {
        this.watchService = watchService;
        this.locationService = locationService;
        this.healthDataService = healthDataService;
    }

    public LiveData<WatchConnectionStatusDTO> getConnectionStatus() { return _connectionStatus; }
    public LiveData<LocationDataDTO> getLocationStatus() { return _locationStatus; }
    public LiveData<List<SensorDataDTO>> getLatestSensorData() { return _latestSensorData; }

    public void loadDashboardData() {
        setLoading(true);

        // Load connection status
        _connectionStatus.setValue(watchService.getCurrentStatus());

        // Load location status
        locationService.getCurrentUserLocation(currentUserId)
                .thenAccept(location -> _locationStatus.postValue(location))
                .exceptionally(throwable -> {
                    Timber.e(throwable, "Error loading location");
                    return null;
                });

        // Load latest sensor data
        healthDataService.getLatestSensorData(currentUserId)
                .thenAccept(data -> {
                    _latestSensorData.postValue(data);
                    setLoading(false);
                })
                .exceptionally(throwable -> {
                    Timber.e(throwable, "Error loading sensor data");
                    setError("Failed to load sensor data");
                    return null;
                });

        // Test database connection
        testDatabaseConnection();
        testPostgreSQLConnection();
    }

    public void connectWatch() {
        setLoading(true);
        watchService.connectWatch()
                .thenAccept(status -> {
                    _connectionStatus.postValue(status);
                    if (status.isConnected()) {
                        setSuccess("Samsung Watch connected successfully");
                        startDataCollection();
                    } else {
                        setError("Failed to connect to Samsung Watch");
                    }
                })
                .exceptionally(throwable -> {
                    Timber.e(throwable, "Error connecting watch");
                    setError("Connection error: " + throwable.getMessage());
                    return null;
                });
    }

    public void disconnectWatch() {
        setLoading(true);
        watchService.disconnectWatch()
                .thenAccept(status -> {
                    _connectionStatus.postValue(status);
                    setSuccess("Samsung Watch disconnected");
                })
                .exceptionally(throwable -> {
                    Timber.e(throwable, "Error disconnecting watch");
                    setError("Disconnection error: " + throwable.getMessage());
                    return null;
                });
    }

    private void startDataCollection() {
        // Start collecting sensor data from all supported sensors
        if (watchService.isConnected()) {
            List<com.feri.watchmyparent.mobile.domain.enums.SensorType> allSensors =
                    java.util.Arrays.asList(com.feri.watchmyparent.mobile.domain.enums.SensorType.values());

            healthDataService.collectSensorData(currentUserId, allSensors)
                    .thenAccept(data -> {
                        _latestSensorData.postValue(data);
                        Timber.d("Collected data from %d sensors", data.size());
                    })
                    .exceptionally(throwable -> {
                        Timber.e(throwable, "Error collecting sensor data");
                        return null;
                    });
        }

        // Update location
        locationService.updateUserLocation(currentUserId)
                .thenAccept(location -> _locationStatus.postValue(location))
                .exceptionally(throwable -> {
                    Timber.e(throwable, "Error updating location");
                    return null;
                });
    }

    public void refreshData() {
        loadDashboardData();
    }

    public boolean isWatchConnected() {
        WatchConnectionStatusDTO status = _connectionStatus.getValue();
        return status != null && status.isConnected();
    }

    private void testDatabaseConnection() {
        PostgreSQLConfig.testConnection()
                .thenAccept(connected -> {
                    if (connected) {
                        Timber.d("✅ PostgreSQL connection successful!");
                        setSuccess("Database connected successfully");
                    } else {
                        Timber.e("❌ PostgreSQL connection failed!");
                        setError("Database connection failed");
                    }
                });
    }

    // Testează conexiunea PostgreSQL
    private void testPostgreSQLConnection() {
        PostgreSQLConfig.testConnection()
                .thenAccept(connected -> {
                    if (connected) {
                        Timber.d("✅ PostgreSQL connection successful!");
                        // Testează inserarea de date
                        PostgreSQLDataService service = new PostgreSQLDataService();
                        service.insertTestData()
                                .thenAccept(inserted -> {
                                    if (inserted) {
                                        setSuccess("Database connection and insert successful!");
                                    } else {
                                        setError("Database connected but insert failed");
                                    }
                                });
                    } else {
                        setError("PostgreSQL connection failed");
                    }
                });
    }
}
