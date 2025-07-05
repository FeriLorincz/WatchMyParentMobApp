package com.feri.watchmyparent.mobile.infrastructure.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.Nullable;
import com.feri.watchmyparent.mobile.application.services.HealthDataApplicationService;
import com.feri.watchmyparent.mobile.application.services.WatchConnectionApplicationService;
import com.feri.watchmyparent.mobile.application.services.LocationApplicationService;
import com.feri.watchmyparent.mobile.domain.enums.SensorType;
import timber.log.Timber;

import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@AndroidEntryPoint
public class WatchDataCollectionService extends Service{

    @Inject
    WatchConnectionApplicationService watchConnectionService;

    @Inject
    HealthDataApplicationService healthDataService;

    @Inject
    LocationApplicationService locationService;

    private ScheduledExecutorService scheduler;
    private Handler mainHandler;

    private String currentUserId = "demo-user-id"; // Get from session in real app

    @Override
    public void onCreate() {
        super.onCreate();
        scheduler = Executors.newScheduledThreadPool(4);
        mainHandler = new Handler(Looper.getMainLooper());
        Timber.d("WatchDataCollectionService created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Timber.d("Starting data collection service");

        if (watchConnectionService.isConnected()) {
            startDataCollection();
        } else {
            // Try to connect first
            connectAndStartCollection();
        }

        return START_STICKY; // Restart service if killed
    }

    private void connectAndStartCollection() {
        watchConnectionService.connectWatch()
                .thenAccept(status -> {
                    if (status.isConnected()) {
                        Timber.d("Watch connected, starting data collection");
                        startDataCollection();
                    } else {
                        Timber.w("Failed to connect watch, will retry");
                        scheduleRetryConnection();
                    }
                })
                .exceptionally(throwable -> {
                    Timber.e(throwable, "Error connecting watch");
                    scheduleRetryConnection();
                    return null;
                });
    }

    private void scheduleRetryConnection() {
        scheduler.schedule(this::connectAndStartCollection, 30, TimeUnit.SECONDS);
    }

    private void startDataCollection() {
        // Schedule critical sensor data collection (every 30 seconds)
        List<SensorType> criticalSensors = Arrays.asList(
                SensorType.HEART_RATE,
                SensorType.BLOOD_OXYGEN,
                SensorType.BLOOD_PRESSURE,
                SensorType.BODY_TEMPERATURE,
                SensorType.STRESS,
                SensorType.FALL_DETECTION
        );

        scheduler.scheduleAtFixedRate(() -> {
            collectSensorData(criticalSensors);
        }, 0, 30, TimeUnit.SECONDS);

        // Schedule important sensor data collection (every 2 minutes)
        List<SensorType> importantSensors = Arrays.asList(
                SensorType.STEP_COUNT,
                SensorType.ACCELEROMETER,
                SensorType.GYROSCOPE
        );

        scheduler.scheduleAtFixedRate(() -> {
            collectSensorData(importantSensors);
        }, 0, 120, TimeUnit.SECONDS);

        // Schedule regular sensor data collection (every 5 minutes)
        List<SensorType> regularSensors = Arrays.asList(
                SensorType.HUMIDITY,
                SensorType.LIGHT,
                SensorType.PROXIMITY
        );

        scheduler.scheduleAtFixedRate(() -> {
            collectSensorData(regularSensors);
        }, 0, 300, TimeUnit.SECONDS);

        // Schedule location updates (every 10 minutes)
        scheduler.scheduleAtFixedRate(() -> {
            updateLocation();
        }, 0, 600, TimeUnit.SECONDS);

        // Schedule long-term sensor data collection (every 15 minutes)
        List<SensorType> longTermSensors = Arrays.asList(
                SensorType.SLEEP,
                SensorType.BIA
        );

        scheduler.scheduleAtFixedRate(() -> {
            collectSensorData(longTermSensors);
        }, 0, 900, TimeUnit.SECONDS);
    }

    private void collectSensorData(List<SensorType> sensorTypes) {
        if (!watchConnectionService.isConnected()) {
            Timber.w("Watch not connected, skipping data collection");
            return;
        }

        healthDataService.collectSensorData(currentUserId, sensorTypes)
                .thenAccept(data -> {
                    Timber.d("Collected %d sensor readings", data.size());
                })
                .exceptionally(throwable -> {
                    Timber.e(throwable, "Error collecting sensor data");
                    return null;
                });
    }

    private void updateLocation() {
        locationService.updateUserLocation(currentUserId)
                .thenAccept(location -> {
                    if (location != null) {
                        Timber.d("Location updated: %s", location.getStatus());
                    }
                })
                .exceptionally(throwable -> {
                    Timber.e(throwable, "Error updating location");
                    return null;
                });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (scheduler != null) {
            scheduler.shutdown();
        }
        Timber.d("WatchDataCollectionService destroyed");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not a bound service
    }
}
