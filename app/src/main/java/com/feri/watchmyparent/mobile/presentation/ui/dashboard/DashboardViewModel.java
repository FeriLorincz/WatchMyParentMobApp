package com.feri.watchmyparent.mobile.presentation.ui.dashboard;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.feri.watchmyparent.mobile.domain.entities.LocationData;
import com.feri.watchmyparent.mobile.domain.entities.SensorData;
import com.feri.watchmyparent.mobile.domain.repositories.LocationDataRepository;
import com.feri.watchmyparent.mobile.domain.repositories.SensorDataRepository;
import com.feri.watchmyparent.mobile.domain.valueobjects.LocationStatus;
import com.feri.watchmyparent.mobile.infrastructure.services.WatchConnectionService;
import com.feri.watchmyparent.mobile.infrastructure.database.PostgreSQLConfig;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@HiltViewModel
public class DashboardViewModel extends ViewModel {

    private static final String TAG = "DashboardViewModel";

    private final SensorDataRepository sensorDataRepository;
    private final LocationDataRepository locationDataRepository;
    private final WatchConnectionService watchConnectionService;
    private final PostgreSQLConfig postgreSQLConfig;

    private final MutableLiveData<List<SensorData>> _sensorData = new MutableLiveData<>();
    private final MutableLiveData<LocationStatus> _locationStatus = new MutableLiveData<>();
    private final MutableLiveData<LocationData> _lastLocation = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _isWatchConnected = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> _isDbConnected = new MutableLiveData<>(false);
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();

    @Inject
    public DashboardViewModel(SensorDataRepository sensorDataRepository,
                              LocationDataRepository locationDataRepository,
                              WatchConnectionService watchConnectionService,
                              PostgreSQLConfig postgreSQLConfig) {
        this.sensorDataRepository = sensorDataRepository;
        this.locationDataRepository = locationDataRepository;
        this.watchConnectionService = watchConnectionService;
        this.postgreSQLConfig = postgreSQLConfig;

        testDatabaseConnection();
        loadSensorData();
        loadLocationData();
    }

    // Getters for LiveData objects
    public LiveData<List<SensorData>> getSensorData() {
        return _sensorData;
    }

    public LiveData<LocationStatus> getLocationStatus() {
        return _locationStatus;
    }

    public LiveData<LocationData> getLastLocation() {
        return _lastLocation;
    }

    public LiveData<Boolean> isWatchConnected() {
        return _isWatchConnected;
    }

    public LiveData<Boolean> isDbConnected() {
        return _isDbConnected;
    }

    public LiveData<String> getErrorMessage() {
        return _errorMessage;
    }

    // Data loading methods
    public void loadSensorData() {
        CompletableFuture.runAsync(() -> {
            try {
                // Adaptat la metodele de repository existente
                List<SensorData> data = sensorDataRepository.findByUserId("demo-user-id", 10).join();
                // Folosim postValue în loc de setValue pentru thread-uri de fundal
                _sensorData.postValue(data);
            } catch (Exception e) {
                Log.e(TAG, "Error loading sensor data: " + e.toString());
                _errorMessage.postValue("Failed to load sensor data: " + e.toString());
            }
        });
    }

    public void loadLocationData() {
        CompletableFuture.runAsync(() -> {
            try {
                // Încercăm să obținem ultima locație
                CompletableFuture<Optional<LocationData>> locationFuture =
                        locationDataRepository.findByUserId("demo-user-id");

                Optional<LocationData> locationOpt = locationFuture.join();

                if (locationOpt.isPresent()) {
                    LocationData location = locationOpt.get();
                    _lastLocation.postValue(location);

                    // Creăm un nou LocationStatus bazat pe date
                    LocationStatus status = location.getLocationStatus();
                    if (status != null) {
                        _locationStatus.postValue(status);
                    } else {
                        // Creăm un status default dacă nu există
                        _locationStatus.postValue(new LocationStatus(
                                "INACTIVE", 0, 0, "", LocalDateTime.now()));
                    }
                } else {
                    // Setăm un status default dacă nu există locație
                    _locationStatus.postValue(new LocationStatus(
                            "INACTIVE", 0, 0, "", LocalDateTime.now()));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading location data: " + e.toString());
                _locationStatus.postValue(new LocationStatus(
                        "ERROR", 0, 0, e.toString(), LocalDateTime.now()));
            }
        });
    }

    // Watch connection methods
    public void connectWatch() {
        CompletableFuture.runAsync(() -> {
            try {
                boolean connected = watchConnectionService.connectWatch().join();
                // Folosim postValue în loc de setValue pentru thread-uri de fundal
                _isWatchConnected.postValue(connected);
                if (!connected) {
                    _errorMessage.postValue("Could not connect to watch");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error connecting watch: " + e.toString());
                _isWatchConnected.postValue(false);
                _errorMessage.postValue("Error connecting watch: " + e.toString());
            }
        });
    }

    public void disconnectWatch() {
        CompletableFuture.runAsync(() -> {
            try {
                boolean disconnected = watchConnectionService.disconnectWatch().join();
                // Folosim postValue în loc de setValue pentru thread-uri de fundal
                _isWatchConnected.postValue(!disconnected);
                if (!disconnected) {
                    _errorMessage.postValue("Could not disconnect from watch");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error disconnecting watch: " + e.toString());
                _errorMessage.postValue("Error disconnecting watch: " + e.toString());
            }
        });
    }

    // Database connection testing
    private void testDatabaseConnection() {
        CompletableFuture.runAsync(() -> {
            try {
                boolean connected = postgreSQLConfig.testConnection().join();
                // Folosim postValue în loc de setValue pentru thread-uri de fundal
                _isDbConnected.postValue(connected);
                if (!connected) {
                    Log.e(TAG, "❌ PostgreSQL connection failed!");
                } else {
                    Log.d(TAG, "✅ PostgreSQL connection successful!");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error testing database connection: " + e.toString());
                _isDbConnected.postValue(false);
                _errorMessage.postValue("Database connection error: " + e.toString());
            }
        });
    }

    // Refresh all data
    public void refreshData() {
        loadSensorData();
        loadLocationData();
        testDatabaseConnection();
    }
}