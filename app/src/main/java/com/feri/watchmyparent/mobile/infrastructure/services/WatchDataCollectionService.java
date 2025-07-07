package com.feri.watchmyparent.mobile.infrastructure.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.Nullable;
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

    // Runnable tasks for periodic execution
    private Runnable criticalSensorTask;
    private Runnable importantSensorTask;
    private Runnable regularSensorTask;
    private Runnable locationUpdateTask;
    private Runnable longTermSensorTask;

    // Intervals in milliseconds
    private static final long CRITICAL_INTERVAL = 30000; // 30 seconds
    private static final long IMPORTANT_INTERVAL = 120000; // 2 minutes
    private static final long REGULAR_INTERVAL = 300000; // 5 minutes
    private static final long LOCATION_INTERVAL = 600000; // 10 minutes
    private static final long LONG_TERM_INTERVAL = 900000; // 15 minutes

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        setupTasks();
        Log.d(TAG, "WatchDataCollectionService created");
    }

    private void setupTasks() {
        // Critical sensors task
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
                collectSensorData(criticalSensors);
                handler.postDelayed(this, CRITICAL_INTERVAL);
            }
        };

        // Important sensors task
        final List<SensorType> importantSensors = Arrays.asList(
                SensorType.STEP_COUNT,
                SensorType.ACCELEROMETER,
                SensorType.GYROSCOPE
        );

        importantSensorTask = new Runnable() {
            @Override
            public void run() {
                collectSensorData(importantSensors);
                handler.postDelayed(this, IMPORTANT_INTERVAL);
            }
        };

        // Regular sensors task
        final List<SensorType> regularSensors = Arrays.asList(
                SensorType.HUMIDITY,
                SensorType.LIGHT,
                SensorType.PROXIMITY
        );

        regularSensorTask = new Runnable() {
            @Override
            public void run() {
                collectSensorData(regularSensors);
                handler.postDelayed(this, REGULAR_INTERVAL);
            }
        };

        // Location update task
        locationUpdateTask = new Runnable() {
            @Override
            public void run() {
                updateLocation();
                handler.postDelayed(this, LOCATION_INTERVAL);
            }
        };

        // Long-term sensors task
        final List<SensorType> longTermSensors = Arrays.asList(
                SensorType.SLEEP,
                SensorType.BIA
        );

        longTermSensorTask = new Runnable() {
            @Override
            public void run() {
                collectSensorData(longTermSensors);
                handler.postDelayed(this, LONG_TERM_INTERVAL);
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand called");

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
                        Log.d(TAG, "Watch connected, starting data collection");
                        startDataCollection();
                    } else {
                        Log.w(TAG, "Failed to connect watch, will retry");
                        scheduleRetryConnection();
                    }
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "Error connecting watch, will retry: " + throwable.getMessage());
                    scheduleRetryConnection();
                    return null;
                });
    }

    private void scheduleRetryConnection() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                connectAndStartCollection();
            }
        }, 30000); // Retry after 30 seconds
    }

    private void startDataCollection() {
        // Start all periodic tasks
        handler.post(criticalSensorTask);
        handler.post(importantSensorTask);
        handler.post(regularSensorTask);
        handler.post(locationUpdateTask);
        handler.post(longTermSensorTask);

        Log.d(TAG, "Data collection started");
    }

    private void stopDataCollection() {
        // Remove all callbacks
        handler.removeCallbacks(criticalSensorTask);
        handler.removeCallbacks(importantSensorTask);
        handler.removeCallbacks(regularSensorTask);
        handler.removeCallbacks(locationUpdateTask);
        handler.removeCallbacks(longTermSensorTask);

        Log.d(TAG, "Data collection stopped");
    }

    private void collectSensorData(List<SensorType> sensorTypes) {
        if (!watchConnectionService.isConnected()) {
            Log.w(TAG, "Watch not connected, skipping data collection");
            return;
        }

        healthDataService.collectSensorData(currentUserId, sensorTypes)
                .thenAccept(data -> {
                    Log.d(TAG, "Collected " + data.size() + " sensor readings");
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "Error collecting sensor data: " + throwable.getMessage());
                    return null;
                });
    }

    private void updateLocation() {
        locationService.updateUserLocation(currentUserId)
                .thenAccept(location -> {
                    if (location != null) {
                        Log.d(TAG, "Location updated: " + location.getStatus());
                    }
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "Error updating location: " + throwable.getMessage());
                    return null;
                });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopDataCollection();
        Log.d(TAG, "WatchDataCollectionService destroyed");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not a bound service
    }
}