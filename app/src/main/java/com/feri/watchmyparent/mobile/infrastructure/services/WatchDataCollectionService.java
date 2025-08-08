package com.feri.watchmyparent.mobile.infrastructure.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import com.feri.watchmyparent.mobile.R;
import com.feri.watchmyparent.mobile.application.services.HealthDataApplicationService;
import com.feri.watchmyparent.mobile.application.services.WatchConnectionApplicationService;
import com.feri.watchmyparent.mobile.application.services.LocationApplicationService;
import com.feri.watchmyparent.mobile.presentation.ui.dashboard.DashboardActivity;

import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.concurrent.CompletableFuture;

//COMPLET MODIFICAT pentru Kafka-Only Pipeline
//Colectează date reale în background și le transmite DOAR prin Kafka
@AndroidEntryPoint
public class WatchDataCollectionService extends Service {

    private static final String TAG = "WatchDataService";
    private static final String CHANNEL_ID = "watch_data_collection";
    private static final int NOTIFICATION_ID = 1001;

    @Inject
    WatchConnectionApplicationService watchConnectionService;

    @Inject
    HealthDataApplicationService healthDataService;

    @Inject
    LocationApplicationService locationService;

    @Inject
    SensorDataIntegrationService sensorDataIntegrationService;

    private Handler handler;
    private String currentUserId = "demo-user-id";

    // REAL periodic tasks pentru Samsung Galaxy Watch 7 data collection
    private Runnable criticalSensorTask;
    private Runnable importantSensorTask;
    private Runnable regularSensorTask;
    private Runnable locationUpdateTask;
    private Runnable longTermSensorTask;
    private Runnable healthCheckTask;

    // REAL intervals optimized pentru Samsung Galaxy Watch 7
    private static final long CRITICAL_INTERVAL = 30000; // 30 seconds - vital signs
    private static final long IMPORTANT_INTERVAL = 120000; // 2 minutes - movement
    private static final long REGULAR_INTERVAL = 300000; // 5 minutes - environment
    private static final long LOCATION_INTERVAL = 600000; // 10 minutes - GPS location
    private static final long LONG_TERM_INTERVAL = 900000; // 15 minutes - sleep
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

        Log.d(TAG, "🚀 Samsung Galaxy Watch 7 Data Collection Service (Kafka-Only) created");

        createNotificationChannel();
        setupKafkaOnlyDataCollectionTasks();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "🔄 Starting REAL data collection through Kafka-only pipeline...");

        if (isServiceRunning) {
            Log.d(TAG, "✅ Service already running, continuing Kafka-only data collection");
            return START_STICKY;
        }

        // Start as foreground service
        startForeground(NOTIFICATION_ID, createServiceNotification());

        // Start data collection with permissive approach
        checkWatchReadinessAndStart()
                .thenAccept(ready -> {
                    if (ready) {
                        startKafkaOnlyDataCollection();
                    } else {
                        Log.w(TAG, "⚠️ Watch not fully ready, but starting anyway with fallback");
                        startKafkaOnlyDataCollectionWithFallback();
                    }
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "❌ Error checking watch readiness", throwable);
                    startKafkaOnlyDataCollectionWithFallback();
                    return null;
                });

        return START_STICKY;
    }

    // Setup REAL data collection tasks pentru Kafka-only pipeline
    private void setupKafkaOnlyDataCollectionTasks() {
        Log.d(TAG, "⚙️ Setting up Kafka-only data collection tasks...");

        // ✅ CRITICAL sensors - Samsung Health SDK (30 seconds)
        criticalSensorTask = new Runnable() {
            @Override
            public void run() {
                if (isWatchConnected) {
                    Log.d(TAG, "🔴 Collecting CRITICAL sensors through Kafka-only pipeline");
                    collectCriticalSensorsKafkaOnly();
                }
                if (isServiceRunning) {
                    handler.postDelayed(this, CRITICAL_INTERVAL);
                }
            }
        };

        // IMPORTANT sensors - Motion and activity tracking (2 minutes)
        importantSensorTask = new Runnable() {
            @Override
            public void run() {
                if (isWatchConnected) {
                    Log.d(TAG, "🟡 Collecting IMPORTANT sensors through Kafka-only pipeline");
                    collectImportantSensorsKafkaOnly();
                }
                if (isServiceRunning) {
                    handler.postDelayed(this, IMPORTANT_INTERVAL);
                }
            }
        };

        // REGULAR sensors - Environmental data (5 minutes)
        regularSensorTask = new Runnable() {
            @Override
            public void run() {
                if (isWatchConnected) {
                    Log.d(TAG, "🟢 Collecting REGULAR sensors through Kafka-only pipeline");
                    collectRegularSensorsKafkaOnly();
                }
                if (isServiceRunning) {
                    handler.postDelayed(this, REGULAR_INTERVAL);
                }
            }
        };

        // LONG-TERM sensors - Sleep (15 minutes)
        longTermSensorTask = new Runnable() {
            @Override
            public void run() {
                if (isWatchConnected) {
                    Log.d(TAG, "🔵 Collecting LONG-TERM sensors through Kafka-only pipeline");
                    collectLongTermSensorsKafkaOnly();
                }
                if (isServiceRunning) {
                    handler.postDelayed(this, LONG_TERM_INTERVAL);
                }
            }
        };

        // GPS Location tracking (10 minutes)
        locationUpdateTask = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "📍 Updating GPS location through Kafka-only pipeline");
                updateLocationKafkaOnly();
                if (isServiceRunning) {
                    handler.postDelayed(this, LOCATION_INTERVAL);
                }
            }
        };

        // Health check task (1 minute)
        healthCheckTask = new Runnable() {
            @Override
            public void run() {
                performKafkaHealthCheck();
                if (isServiceRunning) {
                    handler.postDelayed(this, HEALTH_CHECK_INTERVAL);
                }
            }
        };

        Log.d(TAG, "✅ All Kafka-only data collection tasks configured");
    }

    // CRITICAL sensors collection prin Kafka-only pipeline
    private void collectCriticalSensorsKafkaOnly() {
        CompletableFuture.runAsync(() -> {
            try {
                Log.d(TAG, "📊 [CRITICAL] Collecting Samsung Health SDK sensors through Kafka...");

                sensorDataIntegrationService.collectCriticalSensors()
                        .thenAccept(readings -> {
                            dataCollectionCount += readings.size();

                            Log.d(TAG, "✅ [CRITICAL] Collected " + readings.size() + " readings");
                            Log.d(TAG, "📤 All critical data automatically transmitted through Kafka-only pipeline");

                            // Update notification
                            long uptime = (System.currentTimeMillis() - serviceStartTime) / 60000;
                            String statusText = String.format("📊 CRITICAL: %d readings, %d min uptime (Kafka-Only)",
                                    dataCollectionCount, uptime);
                            updateServiceNotification(statusText);

                            // Log summary (no individual readings to avoid spam)
                            Log.d(TAG, "✅ " + readings.size() + " critical sensors → Kafka pipeline");

                        })
                        .exceptionally(throwable -> {
                            Log.e(TAG, "❌ [CRITICAL] Error in Kafka-only sensor collection", throwable);
                            return null;
                        });

            } catch (Exception e) {
                Log.e(TAG, "❌ [CRITICAL] Exception in Kafka-only collection", e);
            }
        });
    }

    // IMPORTANT sensors collection prin Kafka-only pipeline
    private void collectImportantSensorsKafkaOnly() {
        CompletableFuture.runAsync(() -> {
            try {
                Log.d(TAG, "📊 [IMPORTANT] Collecting motion sensors through Kafka...");

                sensorDataIntegrationService.collectImportantSensors()
                        .thenAccept(readings -> {
                            dataCollectionCount += readings.size();

                            Log.d(TAG, "✅ [IMPORTANT] Collected " + readings.size() + " readings");
                            Log.d(TAG, "✅ " + readings.size() + " important sensors → Kafka pipeline");
                        })
                        .exceptionally(throwable -> {
                            Log.e(TAG, "❌ [IMPORTANT] Error in Kafka-only sensor collection", throwable);
                            return null;
                        });

            } catch (Exception e) {
                Log.e(TAG, "❌ [IMPORTANT] Exception in Kafka-only collection", e);
            }
        });
    }

    //REGULAR sensors collection prin Kafka-only pipeline
    private void collectRegularSensorsKafkaOnly() {
        CompletableFuture.runAsync(() -> {
            try {
                Log.d(TAG, "📊 [REGULAR] Collecting environmental sensors through Kafka...");

                sensorDataIntegrationService.collectRegularSensors()
                        .thenAccept(readings -> {
                            dataCollectionCount += readings.size();

                            Log.d(TAG, "✅ [REGULAR] Collected " + readings.size() + " readings");
                            Log.d(TAG, "✅ " + readings.size() + " regular sensors → Kafka pipeline");
                        })
                        .exceptionally(throwable -> {
                            Log.e(TAG, "❌ [REGULAR] Error in Kafka-only sensor collection", throwable);
                            return null;
                        });

            } catch (Exception e) {
                Log.e(TAG, "❌ [REGULAR] Exception in Kafka-only collection", e);
            }
        });
    }

    // LONG-TERM sensors collection prin Kafka-only pipeline
    private void collectLongTermSensorsKafkaOnly() {
        CompletableFuture.runAsync(() -> {
            try {
                Log.d(TAG, "📊 [LONG_TERM] Collecting sleep sensors through Kafka...");

                sensorDataIntegrationService.collectLongTermSensors()
                        .thenAccept(readings -> {
                            dataCollectionCount += readings.size();

                            Log.d(TAG, "✅ [LONG_TERM] Collected " + readings.size() + " readings");
                            Log.d(TAG, "✅ " + readings.size() + " long-term sensors → Kafka pipeline");
                        })
                        .exceptionally(throwable -> {
                            Log.e(TAG, "❌ [LONG_TERM] Error in Kafka-only sensor collection", throwable);
                            return null;
                        });

            } catch (Exception e) {
                Log.e(TAG, "❌ [LONG_TERM] Exception in Kafka-only collection", e);
            }
        });
    }

    // Location update prin Kafka-only pipeline
    private void updateLocationKafkaOnly() {
        CompletableFuture.runAsync(() -> {
            try {
                Log.d(TAG, "📍 Updating GPS location through Kafka-only pipeline");

                locationService.updateUserLocation(currentUserId)
                        .thenAccept(locationDTO -> {
                            if (locationDTO != null) {
                                Log.d(TAG, "✅ Location updated: " + locationDTO.getStatus() +
                                        " at " + String.format("%.6f, %.6f",
                                        locationDTO.getLatitude(), locationDTO.getLongitude()));
                                Log.d(TAG, "📤 Location data transmitted through Kafka-only pipeline");
                            }
                        })
                        .exceptionally(throwable -> {
                            Log.e(TAG, "❌ Error updating location through Kafka pipeline", throwable);
                            return null;
                        });

            } catch (Exception e) {
                Log.e(TAG, "❌ Exception in Kafka-only location update", e);
            }
        });
    }

    // Health check pentru Kafka-only pipeline
    private void performKafkaHealthCheck() {
        try {
            // Check if watch is still connected
            boolean watchStillConnected = watchConnectionService.getCurrentStatus().isConnected();

            if (watchStillConnected != isWatchConnected) {
                isWatchConnected = watchStillConnected;

                if (isWatchConnected) {
                    Log.d(TAG, "✅ Samsung Galaxy Watch 7 reconnected");
                    updateServiceNotification("✅ Watch connected - Kafka pipeline active");
                } else {
                    Log.w(TAG, "⚠️ Samsung Galaxy Watch 7 disconnected");
                    updateServiceNotification("⚠️ Watch disconnected - attempting reconnect");

                    // Try to reconnect
                    watchConnectionService.connectWatch();
                }
            }

            // Log periodic status for Kafka-only pipeline
            long uptime = (System.currentTimeMillis() - serviceStartTime) / 60000;
            Log.d(TAG, "💗 Kafka-only pipeline health check - Watch: " + (isWatchConnected ? "✅" : "❌") +
                    ", Uptime: " + uptime + " min, Total readings: " + dataCollectionCount);

        } catch (Exception e) {
            Log.e(TAG, "❌ Error in Kafka-only health check", e);
        }
    }

    // Check watch readiness
    private CompletableFuture<Boolean> checkWatchReadinessAndStart() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                boolean hasMinimalPermissions =
                        ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS)
                                == PackageManager.PERMISSION_GRANTED;

                Log.d(TAG, "📊 Samsung Galaxy Watch 7 readiness check (Kafka-only):");
                Log.d(TAG, "   Essential permissions: " + (hasMinimalPermissions ? "✅" : "❌"));

                return hasMinimalPermissions;
            } catch (Exception e) {
                Log.e(TAG, "❌ Error checking watch readiness", e);
                return false;
            }
        });
    }

    // Start Kafka-only data collection
    private void startKafkaOnlyDataCollection() {
        if (isServiceRunning) {
            Log.d(TAG, "✅ Kafka-only data collection already running");
            return;
        }

        Log.d(TAG, "🚀 Starting Kafka-only data collection from Samsung Galaxy Watch 7");

        // Connect to watch first
        watchConnectionService.connectWatch()
                .thenAccept(status -> {
                    isWatchConnected = status.isConnected();

                    if (isWatchConnected) {
                        Log.d(TAG, "✅ Samsung Galaxy Watch 7 connected, starting Kafka-only pipeline");

                        isServiceRunning = true;
                        dataCollectionCount = 0;

                        // Start all periodic tasks for Kafka-only pipeline
                        handler.post(criticalSensorTask);
                        handler.post(importantSensorTask);
                        handler.post(regularSensorTask);
                        handler.post(locationUpdateTask);
                        handler.post(longTermSensorTask);
                        handler.post(healthCheckTask);

                        updateServiceNotification("✅ Kafka-only pipeline ACTIVE - collecting real data");
                        Log.d(TAG, "🎉 All Kafka-only data collection tasks started successfully");

                    } else {
                        Log.e(TAG, "❌ Failed to connect to Samsung Galaxy Watch 7");
                        updateServiceNotification("❌ Watch connection failed");
                        scheduleWatchConnectionRetry();
                    }
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "❌ Error connecting to Samsung Galaxy Watch 7", throwable);
                    updateServiceNotification("❌ Connection error - will retry");
                    scheduleWatchConnectionRetry();
                    return null;
                });
    }

    // Start Kafka-only data collection with fallback
    private void startKafkaOnlyDataCollectionWithFallback() {
        Log.d(TAG, "🔄 Starting Kafka-only data collection with fallback mechanisms...");

        isServiceRunning = true;

        // Connect to watch with fallback
        watchConnectionService.connectWatchWithFallback()
                .thenAccept(status -> {
                    isWatchConnected = status.isPartiallyConnected() || status.isConnected();

                    if (isWatchConnected) {
                        Log.d(TAG, "✅ Watch connected (partial/full), starting Kafka-only tasks");

                        // Start all periodic tasks with staggered delays for fallback mode
                        handler.post(criticalSensorTask);
                        handler.postDelayed(importantSensorTask, 60000); // Delay 1 minute
                        handler.postDelayed(regularSensorTask, 120000);  // Delay 2 minutes
                        handler.postDelayed(locationUpdateTask, 180000); // Delay 3 minutes
                        handler.postDelayed(longTermSensorTask, 240000); // Delay 4 minutes
                        handler.post(healthCheckTask);

                        updateServiceNotification("✅ Kafka-only pipeline active (fallback mode)");
                    } else {
                        Log.e(TAG, "❌ Failed to connect even in fallback mode");
                        updateServiceNotification("❌ Connection failed - stopping service");
                        stopSelf();
                    }
                });
    }

    private void scheduleWatchConnectionRetry() {
        Log.d(TAG, "⏰ Scheduling watch connection retry in 30 seconds...");
        handler.postDelayed(() -> {
            checkWatchReadinessAndStart()
                    .thenAccept(ready -> {
                        if (ready) {
                            Log.d(TAG, "✅ Watch now ready, starting Kafka-only collection");
                            startKafkaOnlyDataCollection();
                        } else {
                            Log.w(TAG, "⚠️ Watch still not ready, will retry");
                            scheduleWatchConnectionRetry();
                        }
                    });
        }, 30000); // Retry every 30 seconds
    }

    private void stopKafkaOnlyDataCollection() {
        Log.d(TAG, "🛑 Stopping Kafka-only data collection from Samsung Galaxy Watch 7");

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

        Log.d(TAG, "✅ All Kafka-only data collection tasks stopped");
        Log.d(TAG, "📊 Final statistics: " + dataCollectionCount + " total readings collected through Kafka");
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Samsung Galaxy Watch 7 Data Collection (Kafka-Only)",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Collects health data and transmits through Kafka-only pipeline");
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
                .setContentTitle("Samsung Galaxy Watch 7 (Kafka-Only)")
                .setContentText("🔄 Collecting health data through Kafka pipeline...")
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
                    .setContentTitle("Samsung Galaxy Watch 7 (Kafka-Only)")
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
        stopKafkaOnlyDataCollection();

        Log.d(TAG, "🔚 Samsung Galaxy Watch 7 Kafka-Only Data Collection Service destroyed");
        Log.d(TAG, "📊 Service ran for " + ((System.currentTimeMillis() - serviceStartTime) / 60000) + " minutes");
        Log.d(TAG, "📈 Total data points collected through Kafka: " + dataCollectionCount);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not a bound service
    }
}