package com.feri.watchmyparent.mobile;

import android.app.Application;
import android.util.Log;

import com.feri.watchmyparent.mobile.infrastructure.utils.DemoDataInitializer;
import com.feri.watchmyparent.mobile.infrastructure.utils.HealthConnectChecker;

import javax.inject.Inject;

import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class WatchMyParentApplication extends Application {

    private static final String TAG = "WatchMyParentApp";

    @Inject
    DemoDataInitializer demoDataInitializer;

    @Override
    public void onCreate() {
        super.onCreate();

        // Inițializare simplă folosind Log standard Android
        initializeLogging();

        // Check Health Connect availability
        checkHealthConnectStatus();

        // Initialize demo data asynchronously
        initializeDemoDataAsync();

        Log.d(TAG, "✅ WatchMyParentApplication initialized");
    }

    private void initializeLogging() {
        // Verifică dacă aplicația rulează în mod debug
        boolean isDebug = (getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0;

        if (isDebug) {
            Log.d(TAG, "🐛 Running in DEBUG mode");
        } else {
            Log.d(TAG, "🚀 Running in RELEASE mode");
        }
    }

    private void checkHealthConnectStatus() {
        try {
            HealthConnectChecker.HealthConnectStatus status =
                    HealthConnectChecker.checkHealthConnectAvailability(this);

            Log.i(TAG, "=== HEALTH CONNECT STATUS ===");
            Log.i(TAG, status.statusMessage);

            if (!status.isAvailable) {
                Log.w(TAG, "⚠️ Health Connect not available - app will use simulated sensor data");
                Log.w(TAG, "📱 This is normal for MVP testing and development");
            } else {
                Log.i(TAG, "✅ Health Connect is available for real sensor data");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error checking Health Connect status", e);
        }
    }

    private void initializeDemoDataAsync() {
        // Run demo data initialization in background
        new Thread(() -> {
            try {
                Log.d(TAG, "🔄 Initializing demo data...");

                // Wait a bit for Hilt injection to complete
                Thread.sleep(1000);

                if (demoDataInitializer != null) {
                    boolean success = demoDataInitializer.initializeDemoData().join();
                    if (success) {
                        Log.d(TAG, "✅ Demo data initialized successfully");
                    } else {
                        Log.e(TAG, "❌ Failed to initialize demo data");
                    }
                } else {
                    Log.w(TAG, "⚠️ DemoDataInitializer not injected yet, will retry later");
                }

            } catch (Exception e) {
                Log.e(TAG, "❌ Error during demo data initialization", e);
            }
        }).start();
    }

    // Public method to manually trigger demo data initialization
    public void retryDemoDataInitialization() {
        if (demoDataInitializer != null) {
            demoDataInitializer.initializeDemoData()
                    .thenAccept(success -> {
                        if (success) {
                            Log.d(TAG, "✅ Demo data retry successful");
                        } else {
                            Log.e(TAG, "❌ Demo data retry failed");
                        }
                    });
        }
    }

    private void handleKafkaInitializationError(Throwable error) {
        Log.e(TAG, "❌ Failed to initialize Kafka: " + error.getMessage());
        Log.w(TAG, "⚠️ Using mock Kafka implementation instead");
        // You could optionally add code here to switch to the mock implementation
    }
}
