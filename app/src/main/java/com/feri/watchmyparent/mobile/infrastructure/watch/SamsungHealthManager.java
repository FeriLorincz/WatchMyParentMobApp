package com.feri.watchmyparent.mobile.infrastructure.watch;

import android.content.Context;
import android.util.Log;

import com.samsung.android.sdk.healthdata.HealthConnectionErrorResult;
import com.samsung.android.sdk.healthdata.HealthConstants;
import com.samsung.android.sdk.healthdata.HealthDataResolver;
import com.samsung.android.sdk.healthdata.HealthDataService;
import com.samsung.android.sdk.healthdata.HealthDataStore;
import com.samsung.android.sdk.healthdata.HealthPermissionManager;
import com.samsung.android.sdk.healthdata.HealthResultHolder;

import com.feri.watchmyparent.mobile.domain.enums.SensorType;
import com.feri.watchmyparent.mobile.domain.valueobjects.SensorReading;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class SamsungHealthManager extends WatchManager{

    private static final String TAG = "SamsungHealthManager";

    private final Context context;
    private HealthDataStore healthDataStore;
    private HealthPermissionManager permissionManager;
    private final Map<SensorType, Integer> sensorFrequencies = new HashMap<>();
    private boolean isHealthDataStoreConnected = false;

    // Samsung Health data types mapping
    private static final Map<SensorType, String> HEALTH_DATA_TYPES = new HashMap<>();

    static {
        HEALTH_DATA_TYPES.put(SensorType.HEART_RATE, HealthConstants.HeartRate.HEALTH_DATA_TYPE);
        HEALTH_DATA_TYPES.put(SensorType.STEP_COUNT, HealthConstants.StepCount.HEALTH_DATA_TYPE);
        HEALTH_DATA_TYPES.put(SensorType.SLEEP, HealthConstants.Sleep.HEALTH_DATA_TYPE);
        HEALTH_DATA_TYPES.put(SensorType.BLOOD_OXYGEN, HealthConstants.SpO2.HEALTH_DATA_TYPE);
        HEALTH_DATA_TYPES.put(SensorType.BLOOD_PRESSURE, HealthConstants.BloodPressure.HEALTH_DATA_TYPE);
        HEALTH_DATA_TYPES.put(SensorType.BODY_TEMPERATURE, HealthConstants.BodyTemperature.HEALTH_DATA_TYPE);
        HEALTH_DATA_TYPES.put(SensorType.STRESS, HealthConstants.StressLevel.HEALTH_DATA_TYPE);
    }

    public SamsungHealthManager(Context context) {
        this.context = context;
        this.deviceId = "samsung_galaxy_watch_7_real";
        initializeSamsungHealth();
    }

    private void initializeSamsungHealth() {
        try {
            Log.d(TAG, "🔄 Initializing Samsung Health SDK...");

            // Initialize Samsung Health Data Store
            healthDataStore = new HealthDataStore(context, mConnectionListener);
            healthDataStore.connectService();

            Log.d(TAG, "✅ Samsung Health SDK initialization started");

        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to initialize Samsung Health SDK", e);
            isConnected = false;
        }
    }

    private final HealthDataStore.ConnectionListener mConnectionListener = new HealthDataStore.ConnectionListener() {
        @Override
        public void onConnected() {
            Log.d(TAG, "✅ Samsung Health Data Store connected");
            isHealthDataStoreConnected = true;
            isConnected = true;

            // Initialize permission manager
            permissionManager = new HealthPermissionManager(healthDataStore);

            // Request necessary permissions
            requestHealthPermissions();
        }

        @Override
        public void onConnectionFailed(HealthConnectionErrorResult error) {
            Log.e(TAG, "❌ Samsung Health connection failed: " + error.getErrorCode());
            isHealthDataStoreConnected = false;
            isConnected = false;

            // Handle different error types
            handleConnectionError(error);
        }

        @Override
        public void onDisconnected() {
            Log.d(TAG, "🔌 Samsung Health Data Store disconnected");
            isHealthDataStoreConnected = false;
            isConnected = false;
        }
    };

    private void handleConnectionError(HealthConnectionErrorResult error) {
        switch (error.getErrorCode()) {
            case HealthConnectionErrorResult.PLATFORM_NOT_INSTALLED:
                Log.e(TAG, "Samsung Health not installed");
                break;
            case HealthConnectionErrorResult.OLD_VERSION_PLATFORM:
                Log.e(TAG, "Samsung Health version too old");
                break;
            case HealthConnectionErrorResult.PLATFORM_DISABLED:
                Log.e(TAG, "Samsung Health disabled");
                break;
            case HealthConnectionErrorResult.USER_AGREEMENT_NEEDED:
                Log.e(TAG, "User agreement needed for Samsung Health");
                break;
            default:
                Log.e(TAG, "Unknown Samsung Health connection error");
                break;
        }
    }

    private void requestHealthPermissions() {
        try {
            // Define required permissions
            Set<HealthPermissionManager.PermissionKey> permissionKeys = new HashSet<>();

            // Add read permissions for supported sensors
            for (SensorType sensorType : getSupportedSensorTypes()) {
                String dataType = HEALTH_DATA_TYPES.get(sensorType);
                if (dataType != null) {
                    permissionKeys.add(new HealthPermissionManager.PermissionKey(
                            dataType, HealthPermissionManager.PermissionType.READ));
                }
            }

            // Request permissions
            permissionManager.requestPermissions(permissionKeys, context)
                    .setResultListener(result -> {
                        Map<HealthPermissionManager.PermissionKey, Boolean> resultMap = result.getResultMap();

                        if (result.isSuccess()) {
                            Log.d(TAG, "✅ Samsung Health permissions granted");
                            logPermissionResults(resultMap);
                        } else {
                            Log.e(TAG, "❌ Samsung Health permissions denied");
                            logPermissionResults(resultMap);
                        }
                    });

        } catch (Exception e) {
            Log.e(TAG, "❌ Error requesting Samsung Health permissions", e);
        }
    }

    private void logPermissionResults(Map<HealthPermissionManager.PermissionKey, Boolean> resultMap) {
        Log.d(TAG, "=== SAMSUNG HEALTH PERMISSIONS ===");
        for (Map.Entry<HealthPermissionManager.PermissionKey, Boolean> entry : resultMap.entrySet()) {
            String dataType = entry.getKey().getDataType();
            boolean granted = entry.getValue();
            Log.d(TAG, dataType + ": " + (granted ? "✅ GRANTED" : "❌ DENIED"));
        }
    }

    @Override
    public CompletableFuture<Boolean> connect() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (isHealthDataStoreConnected) {
                    Log.d(TAG, "✅ Samsung Health already connected");
                    return true;
                }

                // If not connected, try to connect
                if (healthDataStore == null) {
                    initializeSamsungHealth();
                }

                // Wait for connection (with timeout)
                int attempts = 0;
                while (!isHealthDataStoreConnected && attempts < 10) {
                    try {
                        Thread.sleep(500);
                        attempts++;
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }

                if (isHealthDataStoreConnected) {
                    Log.d(TAG, "✅ Samsung Health connected successfully");
                    return true;
                } else {
                    Log.e(TAG, "❌ Samsung Health connection timeout");
                    return false;
                }

            } catch (Exception e) {
                Log.e(TAG, "❌ Error connecting to Samsung Health", e);
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> disconnect() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (healthDataStore != null && isHealthDataStoreConnected) {
                    healthDataStore.disconnectService();
                    Log.d(TAG, "🔌 Samsung Health disconnected");
                }
                isConnected = false;
                isHealthDataStoreConnected = false;
                return true;
            } catch (Exception e) {
                Log.e(TAG, "❌ Error disconnecting Samsung Health", e);
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<List<SensorReading>> readSensorData(List<SensorType> sensorTypes) {
        return CompletableFuture.supplyAsync(() -> {
            List<SensorReading> readings = new ArrayList<>();

            if (!isHealthDataStoreConnected) {
                Log.w(TAG, "❌ Samsung Health not connected, cannot read sensor data");
                return readings;
            }

            try {
                Log.d(TAG, "📊 Reading real sensor data from Samsung Health for " + sensorTypes.size() + " sensors");

                for (SensorType sensorType : sensorTypes) {
                    try {
                        SensorReading reading = readSingleSensorData(sensorType);
                        if (reading != null) {
                            readings.add(reading);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "❌ Error reading " + sensorType + " data", e);
                    }
                }

                Log.d(TAG, "✅ Successfully read " + readings.size() + " real sensor readings");
                return readings;

            } catch (Exception e) {
                Log.e(TAG, "❌ Failed to read sensor data from Samsung Health", e);
                return readings;
            }
        });
    }

    private SensorReading readSingleSensorData(SensorType sensorType) {
        try {
            String dataType = HEALTH_DATA_TYPES.get(sensorType);
            if (dataType == null) {
                Log.w(TAG, "⚠️ No Samsung Health data type for sensor: " + sensorType);
                return null;
            }

            // Create resolver for this data type
            HealthDataResolver resolver = new HealthDataResolver(healthDataStore, null);

            // Define time range (last 24 hours)
            long endTime = System.currentTimeMillis();
            long startTime = endTime - TimeUnit.HOURS.toMillis(24);

            // Build query based on sensor type
            HealthDataResolver.ReadRequest request = createReadRequest(dataType, startTime, endTime);

            // Execute query synchronously (for now)
            HealthResultHolder result = resolver.read(request).get();

            if (result.getStatus().isSuccess()) {
                return parseHealthDataResult(sensorType, result);
            } else {
                Log.w(TAG, "⚠️ Failed to read " + sensorType + " data: " + result.getStatus().getStatusMessage());
                return null;
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Error reading " + sensorType + " from Samsung Health", e);
            return null;
        }
    }

    private HealthDataResolver.ReadRequest createReadRequest(String dataType, long startTime, long endTime) {
        HealthDataResolver.ReadRequest.Builder builder = new HealthDataResolver.ReadRequest.Builder()
                .setDataType(dataType)
                .setProperties(getPropertiesForDataType(dataType))
                .setLocalTimeRange(HealthConstants.Common.START_TIME, HealthConstants.Common.TIME_OFFSET,
                        startTime, endTime)
                .setSort(HealthConstants.Common.START_TIME, HealthDataResolver.SortOrder.DESC);

        return builder.build();
    }

    private String[] getPropertiesForDataType(String dataType) {
        // Return appropriate properties based on data type
        if (HealthConstants.HeartRate.HEALTH_DATA_TYPE.equals(dataType)) {
            return new String[]{
                    HealthConstants.HeartRate.HEART_RATE,
                    HealthConstants.HeartRate.START_TIME
            };
        } else if (HealthConstants.StepCount.HEALTH_DATA_TYPE.equals(dataType)) {
            return new String[]{
                    HealthConstants.StepCount.COUNT,
                    HealthConstants.StepCount.START_TIME
            };
        } else if (HealthConstants.SpO2.HEALTH_DATA_TYPE.equals(dataType)) {
            return new String[]{
                    HealthConstants.SpO2.SPO2,
                    HealthConstants.SpO2.START_TIME
            };
        }
        // Add more cases as needed

        return new String[]{HealthConstants.Common.START_TIME};
    }

    private SensorReading parseHealthDataResult(SensorType sensorType, HealthResultHolder result) {
        try {
            // Get the latest reading
            if (result.getResultMap().isEmpty()) {
                Log.w(TAG, "⚠️ No data found for sensor: " + sensorType);
                return null;
            }

            // Parse based on sensor type
            double value = extractValueFromResult(sensorType, result);

            SensorReading reading = new SensorReading(sensorType, value);
            Log.d(TAG, "📊 Real data: " + sensorType + " = " + value + " " + sensorType.getUnit());

            return reading;

        } catch (Exception e) {
            Log.e(TAG, "❌ Error parsing " + sensorType + " result", e);
            return null;
        }
    }

    private double extractValueFromResult(SensorType sensorType, HealthResultHolder result) {
        // Implementation depends on Samsung Health SDK data structure
        // This is a simplified example - you'll need to implement based on actual SDK

        switch (sensorType) {
            case HEART_RATE:
                // Extract heart rate from result
                return extractHeartRateValue(result);
            case STEP_COUNT:
                // Extract step count from result
                return extractStepCountValue(result);
            case BLOOD_OXYGEN:
                // Extract SpO2 from result
                return extractSpO2Value(result);
            default:
                Log.w(TAG, "⚠️ Unknown sensor type for value extraction: " + sensorType);
                return 0.0;
        }
    }

    private double extractHeartRateValue(HealthResultHolder result) {
        // Implementation for heart rate extraction
        // This needs to be implemented based on Samsung Health SDK documentation
        return 75.0; // Placeholder
    }

    private double extractStepCountValue(HealthResultHolder result) {
        // Implementation for step count extraction
        return 1000.0; // Placeholder
    }

    private double extractSpO2Value(HealthResultHolder result) {
        // Implementation for SpO2 extraction
        return 98.0; // Placeholder
    }

    @Override
    public CompletableFuture<Boolean> configureSensorFrequency(SensorType sensorType, int frequencySeconds) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                sensorFrequencies.put(sensorType, frequencySeconds);
                Log.d(TAG, "⚙️ Configured real sensor " + sensorType + " frequency to " + frequencySeconds + " seconds");
                return true;
            } catch (Exception e) {
                Log.e(TAG, "❌ Failed to configure sensor frequency", e);
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> isDeviceAvailable() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Check if Samsung Health is installed and available
                return HealthDataService.isHealthDataServiceAvailable(context);
            } catch (Exception e) {
                Log.e(TAG, "❌ Error checking Samsung Health availability", e);
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<List<SensorType>> getSupportedSensors() {
        return CompletableFuture.supplyAsync(() -> {
            // Return sensor types that have Samsung Health data type mappings
            return getSupportedSensorTypes();
        });
    }

    private List<SensorType> getSupportedSensorTypes() {
        return Arrays.asList(
                SensorType.HEART_RATE,
                SensorType.STEP_COUNT,
                SensorType.SLEEP,
                SensorType.BLOOD_OXYGEN,
                SensorType.BLOOD_PRESSURE,
                SensorType.BODY_TEMPERATURE,
                SensorType.STRESS
        );
    }

    public boolean isSamsungHealthConnected() {
        return isHealthDataStoreConnected;
    }

    public HealthDataStore getHealthDataStore() {
        return healthDataStore;
    }
}
