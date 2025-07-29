package com.feri.watchmyparent.mobile.presentation.ui.dashboard;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.feri.watchmyparent.mobile.application.dto.LocationDataDTO;
import com.feri.watchmyparent.mobile.application.dto.SensorDataDTO;
import com.feri.watchmyparent.mobile.application.dto.WatchConnectionStatusDTO;
import com.feri.watchmyparent.mobile.application.services.HealthDataApplicationService;
import com.feri.watchmyparent.mobile.application.services.LocationApplicationService;
import com.feri.watchmyparent.mobile.application.services.WatchConnectionApplicationService;
import com.feri.watchmyparent.mobile.domain.entities.LocationData;
import com.feri.watchmyparent.mobile.domain.entities.SensorData;
import com.feri.watchmyparent.mobile.domain.enums.SensorType;
import com.feri.watchmyparent.mobile.domain.repositories.LocationDataRepository;
import com.feri.watchmyparent.mobile.domain.repositories.SensorDataRepository;
import com.feri.watchmyparent.mobile.domain.valueobjects.LocationStatus;
import com.feri.watchmyparent.mobile.infrastructure.kafka.RealHealthDataKafkaProducer;
import com.feri.watchmyparent.mobile.infrastructure.services.WatchConnectionService;
import com.feri.watchmyparent.mobile.infrastructure.database.PostgreSQLConfig;
import com.feri.watchmyparent.mobile.presentation.ui.common.BaseViewModel;

import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@HiltViewModel
public class DashboardViewModel extends BaseViewModel {

    private static final String TAG = "DashboardViewModel";

    // ✅ DOAR servicii de aplicație - respectă DDD
    private final WatchConnectionApplicationService watchConnectionService;
    private final HealthDataApplicationService healthDataService;
    private final LocationApplicationService locationService;

    // ✅ Infrastructure pentru testare
    private final PostgreSQLConfig postgreSQLConfig;
    private final RealHealthDataKafkaProducer kafkaProducer;

    // ✅ DOAR DTO-uri pentru prezentare - respectă separation of concerns
    private final MutableLiveData<WatchConnectionStatusDTO> _connectionStatus = new MutableLiveData<>();
    private final MutableLiveData<LocationDataDTO> _locationStatus = new MutableLiveData<>();
    private final MutableLiveData<List<SensorDataDTO>> _latestSensorData = new MutableLiveData<>();
    private final MutableLiveData<String> _kafkaStatus = new MutableLiveData<>();
    private final MutableLiveData<String> _postgreSQLStatus = new MutableLiveData<>();

    private final String currentUserId = "demo-user-id";

    @Inject
    public DashboardViewModel(
            WatchConnectionApplicationService watchConnectionService,
            HealthDataApplicationService healthDataService,
            LocationApplicationService locationService,
            PostgreSQLConfig postgreSQLConfig,
            RealHealthDataKafkaProducer kafkaProducer) {
        this.watchConnectionService = watchConnectionService;
        this.healthDataService = healthDataService;
        this.locationService = locationService;
        this.postgreSQLConfig = postgreSQLConfig;
        this.kafkaProducer = kafkaProducer;
    }

    // ✅ Getters pentru DTO-uri - clean interface
    public LiveData<WatchConnectionStatusDTO> getConnectionStatus() { return _connectionStatus; }
    public LiveData<LocationDataDTO> getLocationStatus() { return _locationStatus; }
    public LiveData<List<SensorDataDTO>> getLatestSensorData() { return _latestSensorData; }
    public LiveData<String> getKafkaStatus() { return _kafkaStatus; }
    public LiveData<String> getPostgreSQLStatus() { return _postgreSQLStatus; }

    public void loadDashboardData() {
        setLoading(true);
        loadConnectionStatus();
        loadLocationStatus();
        loadLatestSensorData();
        testInfrastructureStatus();
    }

    private void loadConnectionStatus() {
        WatchConnectionStatusDTO status = watchConnectionService.getCurrentStatus();
        _connectionStatus.setValue(status);
    }

    private void loadLocationStatus() {
        locationService.getLastLocation(currentUserId)
                .thenAccept(locationOpt -> {
                    if (locationOpt.isPresent()) {
                        // Convert LocationData to LocationDataDTO
                        LocationDataDTO dto = convertLocationToDTO(locationOpt.get());
                        _locationStatus.postValue(dto);
                    } else {
                        // Create a default LocationDataDTO when no location is found
                        LocationDataDTO defaultDto = createDefaultLocationDTO();
                        _locationStatus.postValue(defaultDto);
                    }
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "Error loading location status", throwable);
                    // Use postValue for background thread
                    LocationDataDTO errorDto = createDefaultLocationDTO();
                    errorDto.setStatus("ERROR");
                    errorDto.setAddress("Failed to load location");
                    _locationStatus.postValue(errorDto);
                    return null;
                });
    }

    private void loadLatestSensorData() {
        healthDataService.getLatestSensorData(currentUserId)
                .thenAccept(sensorData -> {
                    // ✅ FIXED: Use postValue instead of setValue for background thread
                    _latestSensorData.postValue(sensorData);
                    // ✅ FIXED: Use postValue for setLoading too
                    post(() -> setLoading(false));
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "Error loading sensor data", throwable);
                    // ✅ FIXED: Use postValue for background thread
                    _latestSensorData.postValue(new ArrayList<>());
                    post(() -> setError("Failed to load sensor data"));
                    return null;
                });
    }

    // ✅ Test infrastructure status
    private void testInfrastructureStatus() {
        // Test Kafka connection
        CompletableFuture.runAsync(() -> {
            try {
                boolean kafkaConnected = kafkaProducer.isConnected();
                String kafkaStatus = kafkaConnected
                        ? "✅ Kafka: Connected (" + kafkaProducer.getClass().getSimpleName() + ")"
                        : "❌ Kafka: Disconnected";
                _kafkaStatus.postValue(kafkaStatus);
            } catch (Exception e) {
                _kafkaStatus.postValue("❌ Kafka: Error - " + e.getMessage());
            }
        });

        // Test PostgreSQL connection
        postgreSQLConfig.testConnection()
                .thenAccept(connected -> {
                    String pgStatus = connected
                            ? "✅ PostgreSQL: Connected (" + postgreSQLConfig.getConnectionInfo() + ")"
                            : "❌ PostgreSQL: Disconnected or offline";
                    _postgreSQLStatus.postValue(pgStatus);
                })
                .exceptionally(throwable -> {
                    _postgreSQLStatus.postValue("❌ PostgreSQL: Error - " + throwable.getMessage());
                    return null;
                });
    }

    public void connectWatch() {
        setLoading(true);
        watchConnectionService.connectWatch()
                .thenAccept(status -> {
                    // ✅ FIXED: Use postValue for background thread
                    _connectionStatus.postValue(status);
                    if (status.isConnected()) {
                        post(() -> {
                            setSuccess("✅ Watch connected successfully (REAL Samsung Health SDK)");
                            startDataCollection();
                        });
                    } else {
                        post(() -> setError("❌ Failed to connect to watch"));
                    }
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "Error connecting watch", throwable);
                    post(() -> setError("❌ Error connecting to watch: " + throwable.getMessage()));
                    return null;
                });
    }

    public void disconnectWatch() {
        setLoading(true);
        watchConnectionService.disconnectWatch()
                .thenAccept(status -> {
                    // ✅ FIXED: Use postValue for background thread
                    _connectionStatus.postValue(status);
                    if (!status.isConnected()) {
                        post(() -> setSuccess("🔌 Watch disconnected successfully"));
                    } else {
                        post(() -> setError("❌ Failed to disconnect watch"));
                    }
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "Error disconnecting watch", throwable);
                    post(() -> setError("❌ Error disconnecting watch"));
                    return null;
                });
    }

    public boolean isWatchConnected() {
        WatchConnectionStatusDTO status = _connectionStatus.getValue();
        return status != null && status.isConnected();
    }

    public void refreshData() {
        loadDashboardData();
    }

    // ✅ Trigger manual data collection from watch - REAL DATA
    public void collectSensorData() {
        if (!isWatchConnected()) {
            setError("❌ Watch not connected. Please connect first.");
            return;
        }

        setLoading(true);
        healthDataService.collectSensorData(currentUserId, List.of(SensorType.values()))
                .thenAccept(sensorDataList -> {
                    Log.d(TAG, "✅ Collected " + sensorDataList.size() + " REAL sensor readings");
                    // Refresh the display after collecting new data
                    loadLatestSensorData();
                    post(() -> setSuccess("📊 REAL sensor data collected successfully (" + sensorDataList.size() + " readings)"));
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "❌ Error collecting REAL sensor data", throwable);
                    post(() -> setError("❌ Failed to collect REAL sensor data"));
                    return null;
                });
    }

    // ✅ Test Kafka connection manually
    public void testKafkaConnection() {
        setLoading(true);
        CompletableFuture.runAsync(() -> {
            try {
                Log.d(TAG, "🧪 Testing Kafka connection...");

                // Perform health check
                boolean connected = kafkaProducer.healthCheck().join();

                String status = connected
                        ? "✅ Kafka: Connection test PASSED"
                        : "❌ Kafka: Connection test FAILED";

                _kafkaStatus.postValue(status);

                post(() -> {
                    setLoading(false);
                    if (connected) {
                        setSuccess("✅ Kafka connection test successful");
                    } else {
                        setError("❌ Kafka connection test failed");
                    }
                });

            } catch (Exception e) {
                String status = "❌ Kafka: Test error - " + e.getMessage();
                _kafkaStatus.postValue(status);
                post(() -> {
                    setLoading(false);
                    setError("❌ Kafka test failed: " + e.getMessage());
                });
            }
        });
    }

    // ✅ Test PostgreSQL connection manually
    public void testPostgreSQLConnection() {
        setLoading(true);
        postgreSQLConfig.testConnection()
                .thenAccept(connected -> {
                    String status = connected
                            ? "✅ PostgreSQL: Connection test PASSED"
                            : "❌ PostgreSQL: Connection test FAILED";
                    _postgreSQLStatus.postValue(status);

                    post(() -> {
                        setLoading(false);
                        if (connected) {
                            setSuccess("✅ PostgreSQL connection test successful");
                        } else {
                            setError("❌ PostgreSQL connection test failed");
                        }
                    });
                })
                .exceptionally(throwable -> {
                    String status = "❌ PostgreSQL: Test error - " + throwable.getMessage();
                    _postgreSQLStatus.postValue(status);
                    post(() -> {
                        setLoading(false);
                        setError("❌ PostgreSQL test failed: " + throwable.getMessage());
                    });
                    return null;
                });
    }

    // ✅ Start automatic data collection (calls service layer)
    private void startDataCollection() {
        Log.d(TAG, "🔄 Starting automatic REAL data collection...");
        // The actual periodic collection is handled by WatchDataCollectionService
        // This just ensures we have fresh data
        loadLatestSensorData();
    }

    // ✅ Helper method to post to main thread
    private void post(Runnable runnable) {
        android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        mainHandler.post(runnable);
    }

    // ✅ Helper methods for DTO conversion - keeps domain entities private
    private LocationDataDTO convertLocationToDTO(com.feri.watchmyparent.mobile.domain.entities.LocationData locationData) {
        LocationDataDTO dto = new LocationDataDTO();
        dto.setUserId(locationData.getUser().getIdUser());

        if (locationData.getLocationStatus() != null) {
            dto.setStatus(locationData.getLocationStatus().getStatus());
            dto.setLatitude(locationData.getLocationStatus().getLatitude());
            dto.setLongitude(locationData.getLocationStatus().getLongitude());
            dto.setAddress(locationData.getLocationStatus().getAddress());
            dto.setTimestamp(locationData.getLocationStatus().getTimestamp());
        } else {
            dto.setStatus("UNKNOWN");
            dto.setLatitude(0.0);
            dto.setLongitude(0.0);
            dto.setAddress("No location available");
        }

        dto.setAtHome(locationData.isAtHome());
        return dto;
    }

    private LocationDataDTO createDefaultLocationDTO() {
        LocationDataDTO defaultDto = new LocationDataDTO();
        defaultDto.setUserId(currentUserId);
        defaultDto.setStatus("UNKNOWN");
        defaultDto.setLatitude(0.0);
        defaultDto.setLongitude(0.0);
        defaultDto.setAddress("No location data available");
        defaultDto.setAtHome(false);
        return defaultDto;
    }
}