package com.feri.watchmyparent.mobile.application.services;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import com.feri.watchmyparent.mobile.application.dto.WatchConnectionStatusDTO;
import com.feri.watchmyparent.mobile.application.dto.SensorConfigurationDTO;
import com.feri.watchmyparent.mobile.domain.enums.SensorType;
import com.feri.watchmyparent.mobile.domain.valueobjects.SensorReading;
import com.feri.watchmyparent.mobile.infrastructure.watch.RealSamsungHealthManager;
import com.feri.watchmyparent.mobile.infrastructure.watch.WatchManager;
import com.feri.watchmyparent.mobile.infrastructure.watch.WatchCapabilityRegistry;
import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Singleton
public class WatchConnectionApplicationService {

    private final WatchManager watchManager;
    private WatchConnectionStatusDTO currentStatus;

    @Inject
    public WatchConnectionApplicationService(WatchManager watchManager) {
        this.watchManager = watchManager;
        this.currentStatus = new WatchConnectionStatusDTO(false, null, "Samsung Galaxy Watch 7");
    }

    public CompletableFuture<WatchConnectionStatusDTO> connectWatch() {
        Log.d("WatchConnectionApplicationService", "Attempting to connect to Samsung Watch");

        return watchManager.connect()
                .thenCompose(connected -> {
                    if (connected) {
                        currentStatus.setConnected(true);
                        currentStatus.setDeviceId(watchManager.getDeviceId());
                        return loadSupportedSensors();
                    } else {
                        currentStatus.setConnected(false);
                        currentStatus.setConnectionError("Failed to connect to Samsung Watch");
                        return CompletableFuture.completedFuture(currentStatus);
                    }
                })
                .exceptionally(throwable -> {
                    Log.e("WatchConnectionApplicationService", "Error connecting to Samsung Watch", throwable);
                    currentStatus.setConnected(false);
                    currentStatus.setConnectionError(throwable.getMessage());
                    return currentStatus;
                });
    }

    //  Conectează la ceas cu opțiune de fallback pentru situații când
    //  conectarea completă nu este posibilă
    public CompletableFuture<WatchConnectionStatusDTO> connectWatchWithFallback() {
        Log.d("WatchConnectionApplicationService", "🔄 Starting REAL connection to Samsung Galaxy Watch 7...");

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Step 1: Check prerequisites
                if (!checkPrerequisites()) {
                    Log.e("WatchConnectionApplicationService", "❌ Prerequisites not met for connection");
                    return createErrorStatus("Prerequisites not met - install Galaxy Wearable and pair watch");
                }

                // Step 2: Try Health Connect connection (preferred method)
                WatchConnectionStatusDTO healthConnectResult = tryHealthConnectConnection();
                if (healthConnectResult.isConnected()) {
                    Log.d("WatchConnectionApplicationService", "✅ Connected via Health Connect");
                    return healthConnectResult;
                }

                // Step 3: Try direct Samsung Health SDK connection
                WatchConnectionStatusDTO samsungHealthResult = trySamsungHealthConnection();
                if (samsungHealthResult.isConnected()) {
                    Log.d("WatchConnectionApplicationService", "✅ Connected via Samsung Health SDK");
                    return samsungHealthResult;
                }

                // Step 4: Try hardware sensor fallback (partial connection)
                WatchConnectionStatusDTO hardwareResult = tryHardwareSensorConnection();
                if (hardwareResult.isConnected()) {
                    Log.w("WatchConnectionApplicationService", "⚠️ Partial connection via hardware sensors");
                    return hardwareResult;
                }

                // Step 5: Final fallback - realistic simulation
                Log.w("WatchConnectionApplicationService", "⚠️ All direct methods failed, using enhanced simulation");
                return createSimulationStatus();

            } catch (Exception e) {
                Log.e("WatchConnectionApplicationService", "❌ Error during connection attempt", e);
                return createErrorStatus("Connection error: " + e.getMessage());
            }
        });
    }

    private boolean checkPrerequisites() {
        try {
            // Check if Galaxy Wearable is installed
            Context context = watchManager.getContext(); // Assume we have context access
            String[] wearablePackages = {
                    "com.samsung.android.app.watchmanager",
                    "com.samsung.android.geargplugin"
            };

            boolean wearableInstalled = false;
            for (String pkg : wearablePackages) {
                try {
                    context.getPackageManager().getPackageInfo(pkg, 0);
                    wearableInstalled = true;
                    Log.d("WatchConnectionApplicationService", "✅ Found Galaxy Wearable: " + pkg);
                    break;
                } catch (PackageManager.NameNotFoundException e) {
                    // Continue checking
                }
            }

            if (!wearableInstalled) {
                Log.e("WatchConnectionApplicationService", "❌ Galaxy Wearable not installed");
                return false;
            }

            // Check if Bluetooth is enabled
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
                Log.e("WatchConnectionApplicationService", "❌ Bluetooth not available or disabled");
                return false;
            }

            return true;

        } catch (Exception e) {
            Log.e("WatchConnectionApplicationService", "❌ Error checking prerequisites", e);
            return false;
        }
    }

    private WatchConnectionStatusDTO tryHealthConnectConnection() {
        try {
            Log.d("WatchConnectionApplicationService", "🔄 Attempting Health Connect connection...");

            // Try to connect via Health Connect
            if (watchManager instanceof RealSamsungHealthManager) {
                RealSamsungHealthManager realManager = (RealSamsungHealthManager) watchManager;

                boolean connected = realManager.connect().join();

                if (connected && realManager.isHealthConnectReady()) {
                    Log.d("WatchConnectionApplicationService", "✅ Health Connect connection successful");

                    WatchConnectionStatusDTO status = new WatchConnectionStatusDTO(
                            true,
                            "samsung_galaxy_watch_7_hc",
                            "Samsung Galaxy Watch 7 (Health Connect)",
                            false // Full connection
                    );

                    // Load supported sensors
                    status.setSupportedSensors(realManager.getSupportedSensors().join());
                    currentStatus = status;
                    return status;
                }
            }

            Log.w("WatchConnectionApplicationService", "⚠️ Health Connect connection failed");
            return createErrorStatus("Health Connect not available");

        } catch (Exception e) {
            Log.e("WatchConnectionApplicationService", "❌ Health Connect connection error", e);
            return createErrorStatus("Health Connect error: " + e.getMessage());
        }
    }

    private WatchConnectionStatusDTO trySamsungHealthConnection() {
        try {
            Log.d("WatchConnectionApplicationService", "🔄 Attempting Samsung Health SDK connection...");

            // Try Samsung Health SDK connection
            boolean connected = watchManager.connect().join();

            if (connected) {
                Log.d("WatchConnectionApplicationService", "✅ Samsung Health SDK connection successful");

                WatchConnectionStatusDTO status = new WatchConnectionStatusDTO(
                        true,
                        watchManager.getDeviceId(),
                        "Samsung Galaxy Watch 7 (Samsung Health)",
                        false // Full connection
                );

                // Load supported sensors
                status.setSupportedSensors(watchManager.getSupportedSensors().join());
                currentStatus = status;
                return status;
            }

            Log.w("WatchConnectionApplicationService", "⚠️ Samsung Health SDK connection failed");
            return createErrorStatus("Samsung Health SDK not available");

        } catch (Exception e) {
            Log.e("WatchConnectionApplicationService", "❌ Samsung Health SDK connection error", e);
            return createErrorStatus("Samsung Health SDK error: " + e.getMessage());
        }
    }

    private WatchConnectionStatusDTO tryHardwareSensorConnection() {
        try {
            Log.d("WatchConnectionApplicationService", "🔄 Attempting hardware sensor connection...");

            if (watchManager instanceof RealSamsungHealthManager) {
                RealSamsungHealthManager realManager = (RealSamsungHealthManager) watchManager;

                if (realManager.areHardwareSensorsReady()) {
                    Log.d("WatchConnectionApplicationService", "✅ Hardware sensors available");

                    WatchConnectionStatusDTO status = new WatchConnectionStatusDTO(
                            true,
                            "samsung_galaxy_watch_7_hw",
                            "Samsung Galaxy Watch 7 (Hardware Sensors)",
                            true // Partial connection
                    );

                    // Limited sensor set for hardware
                    List<SensorType> hwSensors = Arrays.asList(
                            SensorType.HEART_RATE,
                            SensorType.STEP_COUNT,
                            SensorType.ACCELEROMETER,
                            SensorType.GYROSCOPE
                    );
                    status.setSupportedSensors(hwSensors);
                    currentStatus = status;
                    return status;
                }
            }

            Log.w("WatchConnectionApplicationService", "⚠️ Hardware sensors not available");
            return createErrorStatus("Hardware sensors not available");

        } catch (Exception e) {
            Log.e("WatchConnectionApplicationService", "❌ Hardware sensor connection error", e);
            return createErrorStatus("Hardware sensor error: " + e.getMessage());
        }
    }

    private WatchConnectionStatusDTO createSimulationStatus() {
        Log.w("WatchConnectionApplicationService", "🎭 Creating enhanced simulation status");

        WatchConnectionStatusDTO status = new WatchConnectionStatusDTO(
                true,
                "samsung_galaxy_watch_7_sim",
                "Samsung Galaxy Watch 7 (Enhanced Simulation)",
                true // Partial connection (simulation)
        );

        // Full sensor set for simulation
        List<SensorType> simSensors = Arrays.asList(
                SensorType.HEART_RATE,
                SensorType.BLOOD_OXYGEN,
                SensorType.STEP_COUNT,
                SensorType.SLEEP,
                SensorType.BODY_TEMPERATURE,
                SensorType.STRESS,
                SensorType.ACCELEROMETER,
                SensorType.GYROSCOPE
        );
        status.setSupportedSensors(simSensors);
        currentStatus = status;
        return status;
    }

    private WatchConnectionStatusDTO createErrorStatus(String error) {
        WatchConnectionStatusDTO status = new WatchConnectionStatusDTO(
                false,
                null,
                "Samsung Galaxy Watch 7 (Disconnected)",
                false
        );
        status.setConnectionError(error);
        currentStatus = status;
        return status;
    }

    /**
     * Start continuous monitoring mode
     */
    public CompletableFuture<Boolean> startContinuousMonitoring(List<SensorType> criticalSensors) {
        if (!currentStatus.isConnected()) {
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d("WatchConnectionApplicationService", "🔄 Starting continuous monitoring mode");

                // Configure sensor frequencies based on criticality
                for (SensorType sensorType : criticalSensors) {
                    int frequency = getSensorFrequency(sensorType);
                    watchManager.configureSensorFrequency(sensorType, frequency);
                }

                Log.d("WatchConnectionApplicationService", "✅ Continuous monitoring started for " + criticalSensors.size() + " sensors");
                return true;

            } catch (Exception e) {
                Log.e("WatchConnectionApplicationService", "❌ Error starting continuous monitoring", e);
                return false;
            }
        });
    }

    private int getSensorFrequency(SensorType sensorType) {
        // Define optimal frequencies for different sensor types
        switch (sensorType) {
            case HEART_RATE:
            case BLOOD_OXYGEN:
            case FALL_DETECTION:
                return 30; // Critical sensors every 30 seconds

            case STEP_COUNT:
            case ACCELEROMETER:
            case GYROSCOPE:
                return 60; // Movement sensors every minute

            case BODY_TEMPERATURE:
            case STRESS:
                return 300; // Slower changing sensors every 5 minutes

            case SLEEP:
                return 900; // Long-term sensors every 15 minutes

            default:
                return 120; // Default 2 minutes
        }
    }

    public CompletableFuture<WatchConnectionStatusDTO> disconnectWatch() {
        Log.d("WatchConnectionApplicationService", "Disconnecting from Samsung Watch");

        return watchManager.disconnect()
                .thenApply(success -> {
                    currentStatus.setConnected(false);
                    if (!success) {
                        currentStatus.setConnectionError("Error during disconnection");
                    }
                    return currentStatus;
                });
    }

    private CompletableFuture<WatchConnectionStatusDTO> loadSupportedSensors() {
        return watchManager.getSupportedSensors()
                .thenApply(sensors -> {
                    currentStatus.setSupportedSensors(sensors);
                    Log.d("WatchConnectionApplicationService", "Loaded " + sensors.size() + " supported sensors");
                    return currentStatus;
                });
    }

    public CompletableFuture<Boolean> configureSensorFrequency(SensorConfigurationDTO config) {
        if (!currentStatus.isConnected()) {
            Log.w("WatchConnectionApplicationService", "Cannot configure sensor: Watch not connected");
            return CompletableFuture.completedFuture(false);
        }

        return watchManager.configureSensorFrequency(config.getSensorType(), config.getFrequencySeconds())
                .thenApply(success -> {
                    if (success) {
                        Log.d("WatchConnectionApplicationService", "Successfully configured " + config.getSensorType() + " frequency to " + config.getFrequencySeconds() + " seconds");
                    } else {
                        Log.w("WatchConnectionApplicationService", "Failed to configure " + config.getSensorType() + " frequency");
                    }
                    return success;
                });
    }

    public CompletableFuture<Boolean> isWatchAvailable() {
        return watchManager.isDeviceAvailable();
    }

    public WatchConnectionStatusDTO getCurrentStatus() {
        return currentStatus;
    }

    public boolean isConnected() {
        return currentStatus.isConnected();
    }

    public List<SensorType> getSupportedSensors() {
        return currentStatus.getSupportedSensors();
    }

    public boolean isSensorSupported(SensorType sensorType) {
        if (currentStatus.getSupportedSensors() == null) {
            return WatchCapabilityRegistry.isSensorSupported(watchManager.getDeviceId(), sensorType);
        }
        return currentStatus.getSupportedSensors().contains(sensorType);
    }
}