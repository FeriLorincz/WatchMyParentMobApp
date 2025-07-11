package com.feri.watchmyparent.mobile.infrastructure.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.feri.watchmyparent.mobile.R;
import com.feri.watchmyparent.mobile.application.dto.SensorDataDTO;
import com.feri.watchmyparent.mobile.application.services.HealthDataApplicationService;
import com.feri.watchmyparent.mobile.application.services.WatchConnectionApplicationService;
import com.feri.watchmyparent.mobile.application.services.LocationApplicationService;
import com.feri.watchmyparent.mobile.domain.enums.SensorType;
import com.feri.watchmyparent.mobile.infrastructure.utils.SamsungWatchPermissions;
import com.feri.watchmyparent.mobile.infrastructure.utils.SamsungWatchSetupChecker;
import com.feri.watchmyparent.mobile.presentation.ui.dashboard.DashboardActivity;

import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

//✅ REAL Watch Data Collection Service pentru Samsung Galaxy Watch 7
//Colectează date reale în background cu frecvențe diferite pentru fiecare tip de senzor

@AndroidEntryPoint
public class WatchDataCollectionService extends Service {

    private static final String TAG = "RealWatchDataService";
    private static final String CHANNEL_ID = "watch_data_collection";
    private static final int NOTIFICATION_ID = 1001;

    @Inject
    WatchConnectionApplicationService watchConnectionService;

    @Inject
    HealthDataApplicationService healthDataService;

    @Inject
    LocationApplicationService locationService;

    private Handler handler;
    private String currentUserId = "demo-user-id";

    // ✅ REAL periodic tasks for Samsung Galaxy Watch 7 data collection
    private Runnable criticalSensorTask;
    private Runnable importantSensorTask;
    private Runnable regularSensorTask;
    private Runnable locationUpdateTask;
    private Runnable longTermSensorTask;
    private Runnable healthCheckTask;

    // ✅ REAL intervals optimized for Samsung Galaxy Watch 7
    private static final long CRITICAL_INTERVAL = 30000; // 30 seconds - vital signs
    private static final long IMPORTANT_INTERVAL = 120000; // 2 minutes - movement
    private static final long REGULAR_INTERVAL = 300000; // 5 minutes - environment
    private static final long LOCATION_INTERVAL = 600000; // 10 minutes - GPS location
    private static final long LONG_TERM_INTERVAL = 900000; // 15 minutes - sleep, BIA
    private static final long HEALTH_CHECK_INTERVAL = 60000; // 1 minute - connection check

    // Service state
    private boolean isServiceRunning = false;
    private boolean isWatchConnected = false;
    private int dataCollectionCount = 0;
    private long serviceStartTime;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        serviceStartTime = System.currentTimeMillis();

        Log.d(TAG, "🚀 REAL Samsung Galaxy Watch 7 Data Collection Service created");

        createNotificationChannel();
        setupRealDataCollectionTasks();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "🔄 Starting REAL data collection from Samsung Galaxy Watch 7...");

        if (isServiceRunning) {
            Log.d(TAG, "✅ Service already running, continuing data collection");
            return START_STICKY;
        }

        // Start as foreground service
        startForeground(NOTIFICATION_ID, createServiceNotification());

        // Check if Samsung Galaxy Watch 7 is ready before starting
        checkSamsungWatchReadiness()
                .thenAccept(ready -> {
                    if (ready) {
                        startRealDataCollection();
                    } else {
                        Log.w(TAG, "⚠️ Samsung Galaxy Watch 7 not ready, will retry periodically");
                        scheduleReadinessCheck();
                    }
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "❌ Error checking Samsung Galaxy Watch 7 readiness", throwable);
                    scheduleReadinessCheck();
                    return null;
                });

        return START_STICKY; // Restart service if killed
    }

    private void setupRealDataCollectionTasks() {
        Log.d(TAG, "⚙️ Setting up REAL data collection tasks for Samsung Galaxy Watch 7...");

        // ✅ CRITICAL sensors - Samsung Galaxy Watch 7 vital signs
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
                if (isWatchConnected) {
                    Log.d(TAG, "🔴 Collecting CRITICAL sensor data from Samsung Galaxy Watch 7");
                    collectRealSensorDataAsync(criticalSensors, "CRITICAL");
                }
                if (isServiceRunning) {
                    handler.postDelayed(this, CRITICAL_INTERVAL);
                }
            }
        };

        // ✅ IMPORTANT sensors - Samsung Galaxy Watch 7 movement tracking
        final List<SensorType> importantSensors = Arrays.asList(
                SensorType.STEP_COUNT,
                SensorType.ACCELEROMETER,
                SensorType.GYROSCOPE
        );

        importantSensorTask = new Runnable() {
            @Override
            public void run() {
                if (isWatchConnected) {
                    Log.d(TAG, "🟡 Collecting IMPORTANT sensor data from Samsung Galaxy Watch 7");
                    collectRealSensorDataAsync(importantSensors, "IMPORTANT");
                }
                if (isServiceRunning) {
                    handler.postDelayed(this, IMPORTANT_INTERVAL);
                }
            }
        };

        // ✅ REGULAR sensors - Environmental data
        final List<SensorType> regularSensors = Arrays.asList(
                SensorType.HUMIDITY,
                SensorType.LIGHT,
                SensorType.PROXIMITY
        );

        regularSensorTask = new Runnable() {
            @Override
            public void run() {
                if (isWatchConnected) {
                    Log.d(TAG, "🟢 Collecting REGULAR sensor data from Samsung Galaxy Watch 7");
                    collectRealSensorDataAsync(regularSensors, "REGULAR");
                }
                if (isServiceRunning) {
                    handler.postDelayed(this, REGULAR_INTERVAL);
                }
            }
        };

        // ✅ REAL GPS Location tracking
        locationUpdateTask = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "📍 Updating REAL GPS location");
                updateRealLocationAsync();
                if (isServiceRunning) {
                    handler.postDelayed(this, LOCATION_INTERVAL);
                }
            }
        };

        // ✅ LONG-TERM sensors - Samsung Galaxy Watch 7 sleep and BIA
        final List<SensorType> longTermSensors = Arrays.asList(
                SensorType.SLEEP,
                SensorType.BIA
        );

        longTermSensorTask = new Runnable() {
            @Override
            public void run() {
                if (isWatchConnected) {
                    Log.d(TAG, "🔵 Collecting LONG-TERM sensor data from Samsung Galaxy Watch 7");
                    collectRealSensorDataAsync(longTermSensors, "LONG_TERM");
                }
                if (isServiceRunning) {
                    handler.postDelayed(this, LONG_TERM_INTERVAL);
                }
            }
        };

        // ✅ Health check task - Monitor Samsung Galaxy Watch 7 connection
        healthCheckTask = new Runnable() {
            @Override
            public void run() {
                performHealthCheck();
                if (isServiceRunning) {
                    handler.postDelayed(this, HEALTH_CHECK_INTERVAL);
                }
            }
        };

        Log.d(TAG, "✅ All REAL data collection tasks configured for Samsung Galaxy Watch 7");
    }

    private CompletableFuture<Boolean> checkSamsungWatchReadiness() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Check setup status
                SamsungWatchSetupChecker.WatchSetupStatus setupStatus =
                        SamsungWatchSetupChecker.checkCompleteSetup(this).join();

                // Check permissions
                SamsungWatchPermissions.PermissionStatus permissionStatus =
                        SamsungWatchPermissions.checkAllPermissions(this).join();

                boolean ready = setupStatus.isFullyReady && permissionStatus.allGranted;

                Log.d(TAG, "📊 Samsung Galaxy Watch 7 readiness check:");
                Log.d(TAG, "   Setup complete: " + setupStatus.isFullyReady);
                Log.d(TAG, "   Permissions granted: " + permissionStatus.allGranted);
                Log.d(TAG, "   Overall ready: " + ready);

                return ready;

            } catch (Exception e) {
                Log.e(TAG, "❌ Error checking Samsung Galaxy Watch 7 readiness", e);
                return false;
            }
        });
    }

    private void scheduleReadinessCheck() {
        Log.d(TAG, "⏰ Scheduling Samsung Galaxy Watch 7 readiness check in 30 seconds...");
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                checkSamsungWatchReadiness()
                        .thenAccept(ready -> {
                            if (ready) {
                                Log.d(TAG, "✅ Samsung Galaxy Watch 7 now ready, starting data collection");
                                startRealDataCollection();
                            } else {
                                Log.w(TAG, "⚠️ Samsung Galaxy Watch 7 still not ready, will retry");
                                scheduleReadinessCheck();
                            }
                        });
            }
        }, 30000); // Retry every 30 seconds
    }

    private void startRealDataCollection() {
        if (isServiceRunning) {
            Log.d(TAG, "✅ Real data collection already running");
            return;
        }

        Log.d(TAG, "🚀 Starting REAL data collection from Samsung Galaxy Watch 7");

        // Connect to watch first
        watchConnectionService.connectWatch()
                .thenAccept(status -> {
                    isWatchConnected = status.isConnected();

                    if (isWatchConnected) {
                        Log.d(TAG, "✅ Samsung Galaxy Watch 7 connected, starting all data collection tasks");

                        isServiceRunning = true;
                        dataCollectionCount = 0;

                        // Start all periodic tasks
                        handler.post(criticalSensorTask);
                        handler.post(importantSensorTask);
                        handler.post(regularSensorTask);
                        handler.post(locationUpdateTask);
                        handler.post(longTermSensorTask);
                        handler.post(healthCheckTask);

                        updateServiceNotification("✅ Collecting REAL data from Samsung Galaxy Watch 7");
                        Log.d(TAG, "🎉 All REAL data collection tasks started successfully");

                    } else {
                        Log.e(TAG, "❌ Failed to connect to Samsung Galaxy Watch 7");
                        updateServiceNotification("❌ Samsung Galaxy Watch 7 connection failed");
                        scheduleReadinessCheck(); // Retry
                    }
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "❌ Error connecting to Samsung Galaxy Watch 7", throwable);
                    updateServiceNotification("❌ Connection error");
                    scheduleReadinessCheck();
                    return null;
                });
    }

    private void collectRealSensorDataAsync(List<SensorType> sensorTypes, String priority) {
        CompletableFuture.runAsync(() -> {
            try {
                Log.d(TAG, "📊 [" + priority + "] Collecting REAL data for " + sensorTypes.size() + " sensors");

                healthDataService.collectSensorData(currentUserId, sensorTypes)
                        .thenAccept(data -> {
                            dataCollectionCount += data.size();

                            Log.d(TAG, "✅ [" + priority + "] Successfully collected " + data.size() + " REAL sensor readings");
                            Log.d(TAG, "📈 Total readings collected: " + dataCollectionCount);

                            // Update notification with latest stats
                            long uptime = (System.currentTimeMillis() - serviceStartTime) / 60000; // minutes
                            String statusText = String.format("📊 Collected %d readings (%d min uptime)",
                                    dataCollectionCount, uptime);
                            updateServiceNotification(statusText);

                            // Log individual readings for verification
                            for (SensorDataDTO sensorData : data) {
                                Log.d(TAG, "📊 REAL [" + priority + "]: " + sensorData.getSensorType() +
                                        " = " + sensorData.getValue() + " " + sensorData.getUnit() +
                                        " (transmitted: " + sensorData.isTransmitted() + ")");
                            }
                        })
                        .exceptionally(throwable -> {
                            Log.e(TAG, "❌ [" + priority + "] Error collecting REAL sensor data", throwable);
                            return null;
                        });

            } catch (Exception e) {
                Log.e(TAG, "❌ [" + priority + "] Exception in REAL sensor data collection", e);
            }
        });
    }

    private void updateRealLocationAsync() {
        CompletableFuture.runAsync(() -> {
            try {
                Log.d(TAG, "📍 Updating REAL GPS location and sending via Kafka + PostgreSQL");

                locationService.updateUserLocation(currentUserId)
                        .thenAccept(location -> {
                            if (location != null) {
                                Log.d(TAG, "✅ REAL location updated: " + location.getStatus() +
                                        " at " + location.getFormattedCoordinates());
                                Log.d(TAG, "📤 Location sent to REAL Kafka and PostgreSQL");
                            }
                        })
                        .exceptionally(throwable -> {
                            Log.e(TAG, "❌ Error updating REAL GPS location", throwable);
                            return null;
                        });

            } catch (Exception e) {
                Log.e(TAG, "❌ Exception in REAL location update", e);
            }
        });
    }

    private void performHealthCheck() {
        try {
            // Check if watch is still connected
            boolean watchStillConnected = watchConnectionService.getCurrentStatus().isConnected();

            if (watchStillConnected != isWatchConnected) {
                isWatchConnected = watchStillConnected;

                if (isWatchConnected) {
                    Log.d(TAG, "✅ Samsung Galaxy Watch 7 reconnected");
                    updateServiceNotification("✅ Samsung Galaxy Watch 7 reconnected");
                } else {
                    Log.w(TAG, "⚠️ Samsung Galaxy Watch 7 disconnected");
                    updateServiceNotification("⚠️ Samsung Galaxy Watch 7 disconnected");

                    // Try to reconnect
                    watchConnectionService.connectWatch();
                }
            }

            // Log periodic status
            long uptime = (System.currentTimeMillis() - serviceStartTime) / 60000;
            Log.d(TAG, "💗 Health check - Watch: " + (isWatchConnected ? "✅" : "❌") +
                    ", Uptime: " + uptime + " min, Readings: " + dataCollectionCount);

        } catch (Exception e) {
            Log.e(TAG, "❌ Error in health check", e);
        }
    }

    private void stopRealDataCollection() {
        Log.d(TAG, "🛑 Stopping REAL data collection from Samsung Galaxy Watch 7");

        isServiceRunning = false;
        isWatchConnected = false;

        // Remove all callbacks
        if (handler != null) {
            handler.removeCallbacks(criticalSensorTask);
            handler.removeCallbacks(importantSensorTask);
            handler.removeCallbacks(regularSensorTask);
            handler.removeCallbacks(locationUpdateTask);
            handler.removeCallbacks(longTermSensorTask);
            handler.removeCallbacks(healthCheckTask);
        }

        // Disconnect from watch
        if (watchConnectionService != null) {
            watchConnectionService.disconnectWatch();
        }

        Log.d(TAG, "✅ All REAL data collection tasks stopped");
        Log.d(TAG, "📊 Final statistics: " + dataCollectionCount + " total sensor readings collected");
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Samsung Galaxy Watch 7 Data Collection",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Collects REAL health data from Samsung Galaxy Watch 7");
            channel.setShowBadge(false);

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification createServiceNotification() {
        Intent notificationIntent = new Intent(this, DashboardActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Samsung Galaxy Watch 7 Data Collection")
                .setContentText("🔄 Collecting REAL health data...")
                .setSmallIcon(R.drawable.ic_watch_notification)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();
    }

    private void updateServiceNotification(String statusText) {
        try {
            Intent notificationIntent = new Intent(this, DashboardActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this, 0, notificationIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Samsung Galaxy Watch 7 REAL Data")
                    .setContentText(statusText)
                    .setSmallIcon(R.drawable.ic_watch_notification)
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setCategory(NotificationCompat.CATEGORY_SERVICE)
                    .build();

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.notify(NOTIFICATION_ID, notification);

        } catch (Exception e) {
            Log.e(TAG, "❌ Error updating notification", e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopRealDataCollection();

        Log.d(TAG, "🔚 REAL Samsung Galaxy Watch 7 Data Collection Service destroyed");
        Log.d(TAG, "📊 Service ran for " + ((System.currentTimeMillis() - serviceStartTime) / 60000) + " minutes");
        Log.d(TAG, "📈 Total data points collected: " + dataCollectionCount);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not a bound service
    }
}