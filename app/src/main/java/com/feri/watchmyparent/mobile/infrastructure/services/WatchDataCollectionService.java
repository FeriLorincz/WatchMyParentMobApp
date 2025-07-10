package com.feri.watchmyparent.mobile.infrastructure.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.Nullable;

import com.feri.watchmyparent.mobile.application.dto.SensorDataDTO;
import com.feri.watchmyparent.mobile.application.services.HealthDataApplicationService;
import com.feri.watchmyparent.mobile.application.services.WatchConnectionApplicationService;
import com.feri.watchmyparent.mobile.application.services.LocationApplicationService;
import com.feri.watchmyparent.mobile.domain.enums.SensorType;
import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.Arrays;
import java.util.List;

@AndroidEntryPoint
public class WatchDataCollectionService extends Service {

    private static final String TAG = "WatchDataCollectionService";

    @Inject
    WatchConnectionApplicationService watchConnectionService;

    @Inject
    HealthDataApplicationService healthDataService;

    @Inject
    LocationApplicationService locationService;

    private Handler handler;
    private String currentUserId = "demo-user-id"; // Get from session in real app

    // Runnable tasks for periodic execution - REAL DATA
    private Runnable criticalSensorTask;
    private Runnable importantSensorTask;
    private Runnable regularSensorTask;
    private Runnable locationUpdateTask;
    private Runnable longTermSensorTask;

    // ✅ REAL intervals based on sensor criticality
    private static final long CRITICAL_INTERVAL = 30000; // 30 seconds - vital signs
    private static final long IMPORTANT_INTERVAL = 120000; // 2 minutes - movement
    private static final long REGULAR_INTERVAL = 300000; // 5 minutes - environment
    private static final long LOCATION_INTERVAL = 600000; // 10 minutes - location
    private static final long LONG_TERM_INTERVAL = 900000; // 15 minutes - sleep, BIA

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        setupRealDataCollectionTasks();
        Log.d(TAG, "✅ REAL WatchDataCollectionService created");
    }

    private void setupRealDataCollectionTasks() {
        // ✅ CRITICAL sensors task - REAL Samsung Health data
        final List<SensorType> criticalSensors = Arrays.asList(
                SensorType.HEART_RATE,
                SensorType.BLOOD_OXYGEN,
                SensorType.BLOOD_PRESSURE,
                SensorType.BODY_TEMPERATURE,
                SensorType.STRESS,
                SensorType.FALL_DETECTION
        );

        criticalSensorTask = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "🔴 Collecting CRITICAL sensor data (REAL)");
                collectRealSensorData(criticalSensors);
                handler.postDelayed(this, CRITICAL_INTERVAL);
            }
        };

        // ✅ IMPORTANT sensors task - REAL movement data
        final List<SensorType> importantSensors = Arrays.asList(
                SensorType.STEP_COUNT,
                SensorType.ACCELEROMETER,
                SensorType.GYROSCOPE
        );

        importantSensorTask = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "🟡 Collecting IMPORTANT sensor data (REAL)");
                collectRealSensorData(importantSensors);
                handler.postDelayed(this, IMPORTANT_INTERVAL);
            }
        };

        // ✅ REGULAR sensors task - REAL environment data
        final List<SensorType> regularSensors = Arrays.asList(
                SensorType.HUMIDITY,
                SensorType.LIGHT,
                SensorType.PROXIMITY
        );

        regularSensorTask = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "🟢 Collecting REGULAR sensor data (REAL)");
                collectRealSensorData(regularSensors);
                handler.postDelayed(this, REGULAR_INTERVAL);
            }
        };

        // ✅ REAL Location update task
        locationUpdateTask = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "📍 Updating REAL location data");
                updateRealLocation();
                handler.postDelayed(this, LOCATION_INTERVAL);
            }
        };

        // ✅ LONG-TERM sensors task - REAL long-term data
        final List<SensorType> longTermSensors = Arrays.asList(
                SensorType.SLEEP,
                SensorType.BIA
        );

        longTermSensorTask = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "🔵 Collecting LONG-TERM sensor data (REAL)");
                collectRealSensorData(longTermSensors);
                handler.postDelayed(this, LONG_TERM_INTERVAL);
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "🚀 onStartCommand called - starting REAL data collection");

        if (watchConnectionService.isConnected()) {
            startRealDataCollection();
        } else {
            // Try to connect first
            connectAndStartRealCollection();
        }

        return START_STICKY; // Restart service if killed
    }

    private void connectAndStartRealCollection() {
        Log.d(TAG, "🔄 Attempting to connect to Samsung Health SDK...");

        watchConnectionService.connectWatch()
                .thenAccept(status -> {
                    if (status.isConnected()) {
                        Log.d(TAG, "✅ Samsung Health connected, starting REAL data collection");
                        startRealDataCollection();
                    } else {
                        Log.w(TAG, "❌ Failed to connect Samsung Health, will retry");
                        scheduleRetryConnection();
                    }
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "❌ Error connecting Samsung Health, will retry: " + throwable.getMessage());
                    scheduleRetryConnection();
                    return null;
                });
    }

    private void scheduleRetryConnection() {
        Log.d(TAG, "⏰ Scheduling Samsung Health connection retry in 30 seconds...");
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                connectAndStartRealCollection();
            }
        }, 30000); // Retry after 30 seconds
    }

    private void startRealDataCollection() {
        Log.d(TAG, "🚀 Starting REAL data collection with Samsung Health SDK");

        // Start all periodic tasks for REAL data collection
        handler.post(criticalSensorTask);
        handler.post(importantSensorTask);
        handler.post(regularSensorTask);
        handler.post(locationUpdateTask);
        handler.post(longTermSensorTask);

        Log.d(TAG, "✅ All REAL data collection tasks started successfully");
    }

    private void stopRealDataCollection() {
        Log.d(TAG, "🛑 Stopping REAL data collection");

        // Remove all callbacks
        handler.removeCallbacks(criticalSensorTask);
        handler.removeCallbacks(importantSensorTask);
        handler.removeCallbacks(regularSensorTask);
        handler.removeCallbacks(locationUpdateTask);
        handler.removeCallbacks(longTermSensorTask);

        Log.d(TAG, "✅ All REAL data collection tasks stopped");
    }

    private void collectRealSensorData(List<SensorType> sensorTypes) {
        if (!watchConnectionService.isConnected()) {
            Log.w(TAG, "⚠️ Samsung Health not connected, skipping data collection");
            return;
        }

        Log.d(TAG, "📊 Collecting REAL data for " + sensorTypes.size() + " sensors: " + sensorTypes);

        healthDataService.collectSensorData(currentUserId, sensorTypes)
                .thenAccept(data -> {
                    Log.d(TAG, "✅ Successfully collected " + data.size() + " REAL sensor readings");

                    // ✅ FIXED: Replace 'var' with explicit type 'SensorDataDTO' for Java 8
                    for (SensorDataDTO sensorData : data) {
                        Log.d(TAG, "📊 REAL DATA: " + sensorData.getSensorType() + " = " +
                                sensorData.getValue() + " " + sensorData.getUnit() +
                                " (transmitted: " + sensorData.isTransmitted() + ")");
                    }
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "❌ Error collecting REAL sensor data: " + throwable.getMessage());
                    return null;
                });
    }

    private void updateRealLocation() {
        Log.d(TAG, "📍 Updating REAL location with GPS and sending via Kafka + PostgreSQL");

        locationService.updateUserLocation(currentUserId)
                .thenAccept(location -> {
                    if (location != null) {
                        Log.d(TAG, "✅ REAL location updated: " + location.getStatus() +
                                " at " + location.getFormattedCoordinates());
                        Log.d(TAG, "📤 Location sent to REAL Kafka and PostgreSQL");
                    }
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "❌ Error updating REAL location: " + throwable.getMessage());
                    return null;
                });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopRealDataCollection();
        Log.d(TAG, "🔚 REAL WatchDataCollectionService destroyed");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not a bound service
    }
}