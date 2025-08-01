package com.feri.watchmyparent.mobile.infrastructure.services;

import android.util.Log;

import com.feri.watchmyparent.mobile.domain.enums.SensorType;
import com.feri.watchmyparent.mobile.domain.valueobjects.SensorReading;
import com.feri.watchmyparent.mobile.infrastructure.watch.RealSamsungHealthManager;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;
import javax.inject.Singleton;

// Orchestrates data collection from Samsung Health SDK, Health Connect, and Hardware sensors
 //Based on my Samsung developer permissions and CriticalityLevel frequencies

@Singleton
public class SensorDataIntegrationService {

    private static final String TAG = "SensorDataIntegration";

    private final RealSamsungHealthManager watchManager;
    private final SamsungHealthDataService samsungHealthDataService;
    private final PostgreSQLDataService postgreSQLDataService;

    // Samsung Health permitted sensors (from your developer agreement)
    private final Set<SensorType> SAMSUNG_HEALTH_PERMITTED = new HashSet<>(Arrays.asList(
            SensorType.HEART_RATE,        // com.samsung.health.heart_rate
            SensorType.BLOOD_OXYGEN,      // com.samsung.health.blood_oxygen
            SensorType.BLOOD_PRESSURE,    // com.samsung.health.blood_pressure
            SensorType.BODY_TEMPERATURE,  // com.samsung.health.skin_temperature
            SensorType.SLEEP              // com.samsung.health.sleep
    ));

    // Health Connect / Hardware sensors (remaining sensors)
    private final Set<SensorType> HEALTH_CONNECT_SENSORS = new HashSet<>(Arrays.asList(
            SensorType.STEP_COUNT,
            SensorType.ACCELEROMETER,
            SensorType.GYROSCOPE,
            SensorType.STRESS,
            SensorType.FALL_DETECTION,
            SensorType.BIA,
            SensorType.GRAVITY,
            SensorType.LINEAR_ACCELERATION,
            SensorType.ROTATION,
            SensorType.ORIENTATION,
            SensorType.MAGNETIC_FIELD,
            SensorType.HUMIDITY,
            SensorType.LIGHT,
            SensorType.PROXIMITY,
            SensorType.LOCATION
    ));

    @Inject
    public SensorDataIntegrationService(
            RealSamsungHealthManager watchManager,
            SamsungHealthDataService samsungHealthDataService,
            PostgreSQLDataService postgreSQLDataService) {
        this.watchManager = watchManager;
        this.samsungHealthDataService = samsungHealthDataService;
        this.postgreSQLDataService = postgreSQLDataService;

        Log.d(TAG, "🔄 Initializing Sensor Data Integration Service");
        Log.d(TAG, "📊 Samsung Health permitted sensors: " + SAMSUNG_HEALTH_PERMITTED.size());
        Log.d(TAG, "📊 Health Connect/Hardware sensors: " + HEALTH_CONNECT_SENSORS.size());
    }

    //Collect sensor data based on criticality levels and data sources
    public CompletableFuture<List<SensorReading>> collectSensorDataByCriticality(
            com.feri.watchmyparent.mobile.domain.enums.CriticalityLevel criticalityLevel) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                List<SensorType> sensorsToRead = getSensorsByCriticality(criticalityLevel);

                Log.d(TAG, "📊 Collecting " + criticalityLevel.name() + " sensors: " + sensorsToRead.size());

                List<SensorReading> allReadings = new ArrayList<>();

                // ✅ PHASE 1: Collect from Samsung Health SDK (permitted sensors)
                List<SensorType> samsungHealthSensors = sensorsToRead.stream()
                        .filter(SAMSUNG_HEALTH_PERMITTED::contains)
                        .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

                if (!samsungHealthSensors.isEmpty()) {
                    List<SensorReading> samsungReadings = collectFromSamsungHealth(samsungHealthSensors);
                    allReadings.addAll(samsungReadings);
                    Log.d(TAG, "✅ Samsung Health SDK: " + samsungReadings.size() + " readings");
                }

                // ✅ PHASE 2: Collect from Health Connect/Hardware (remaining sensors)
                List<SensorType> healthConnectSensors = sensorsToRead.stream()
                        .filter(HEALTH_CONNECT_SENSORS::contains)
                        .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

                if (!healthConnectSensors.isEmpty()) {
                    List<SensorReading> healthConnectReadings = collectFromHealthConnect(healthConnectSensors);
                    allReadings.addAll(healthConnectReadings);
                    Log.d(TAG, "✅ Health Connect/Hardware: " + healthConnectReadings.size() + " readings");
                }

                // ✅ PHASE 3: Store in PostgreSQL
                if (!allReadings.isEmpty()) {
                    storeReadingsInPostgreSQL(allReadings, criticalityLevel);
                }

                Log.d(TAG, "✅ Total collected for " + criticalityLevel.name() + ": " + allReadings.size() + " readings");
                return allReadings;

            } catch (Exception e) {
                Log.e(TAG, "❌ Error collecting sensor data for " + criticalityLevel.name(), e);
                return new ArrayList<>();
            }
        });
    }

    // Get sensors by criticality level
    private List<SensorType> getSensorsByCriticality(com.feri.watchmyparent.mobile.domain.enums.CriticalityLevel criticalityLevel) {
        List<SensorType> sensors = new ArrayList<>();

        for (SensorType sensorType : SensorType.values()) {
            if (sensorType.getCriticalityLevel() == criticalityLevel) {
                sensors.add(sensorType);
            }
        }

        return sensors;
    }

    //Collect from Samsung Health SDK (your permitted sensors)
    private List<SensorReading> collectFromSamsungHealth(List<SensorType> sensors) {
        List<SensorReading> readings = new ArrayList<>();

        try {
            Log.d(TAG, "📱 Collecting from Samsung Health SDK: " + sensors.size() + " sensors");

            for (SensorType sensorType : sensors) {
                if (samsungHealthDataService.isConnected() &&
                        samsungHealthDataService.isSensorPermitted(sensorType)) {

                    SensorReading reading = samsungHealthDataService.readSensorData(sensorType).join();
                    if (reading != null) {
                        reading.setDeviceId("samsung_galaxy_watch_7");
                        reading.setConnectionType("SAMSUNG_HEALTH_SDK");
                        reading.setMetadata("source=samsung_health_sdk,permitted=true");
                        readings.add(reading);

                        Log.d(TAG, "📊 Samsung Health: " + sensorType + " = " +
                                String.format("%.2f", reading.getValue()) + " " + sensorType.getUnit());
                    }
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Error collecting from Samsung Health SDK", e);
        }

        return readings;
    }

    // Collect from Health Connect and Hardware sensors
    private List<SensorReading> collectFromHealthConnect(List<SensorType> sensors) {
        List<SensorReading> readings = new ArrayList<>();

        try {
            Log.d(TAG, "🔗 Collecting from Health Connect/Hardware: " + sensors.size() + " sensors");

            // Use the watch manager to read these sensors
            List<SensorReading> watchReadings = watchManager.readSensorData(sensors).join();

            for (SensorReading reading : watchReadings) {
                if (reading != null) {
                    reading.setDeviceId("samsung_galaxy_watch_7");
                    if (reading.getConnectionType() == null) {
                        reading.setConnectionType("HEALTH_CONNECT_HARDWARE");
                    }
                    reading.setMetadata("source=health_connect_hardware,permitted=false");
                    readings.add(reading);

                    Log.d(TAG, "📊 Health Connect/HW: " + reading.getSensorType() + " = " +
                            String.format("%.2f", reading.getValue()) + " " + reading.getSensorType().getUnit());
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Error collecting from Health Connect/Hardware", e);
        }

        return readings;
    }

    //Store readings in PostgreSQL with metadata
    private void storeReadingsInPostgreSQL(List<SensorReading> readings,
                                           com.feri.watchmyparent.mobile.domain.enums.CriticalityLevel criticalityLevel) {
        try {
            Log.d(TAG, "💾 Storing " + readings.size() + " readings in PostgreSQL...");

            for (SensorReading reading : readings) {
                // Convert SensorReading to SensorData entity
                com.feri.watchmyparent.mobile.domain.entities.SensorData sensorData =
                        convertToSensorDataEntity(reading, "demo-user-id", criticalityLevel);

                // Store in PostgreSQL
                postgreSQLDataService.insertSensorData(sensorData)
                        .thenAccept(success -> {
                            if (success) {
                                Log.d(TAG, "✅ Stored: " + reading.getSensorType() + " in PostgreSQL");
                            } else {
                                Log.w(TAG, "⚠️ Failed to store: " + reading.getSensorType());
                            }
                        });
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Error storing readings in PostgreSQL", e);
        }
    }

    // Convert SensorReading to SensorData entity
    private com.feri.watchmyparent.mobile.domain.entities.SensorData convertToSensorDataEntity(
            SensorReading reading, String userId,
            com.feri.watchmyparent.mobile.domain.enums.CriticalityLevel criticalityLevel) {

        // Create a minimal user entity (in real implementation, fetch from repository)
        com.feri.watchmyparent.mobile.domain.entities.User user =
                new com.feri.watchmyparent.mobile.domain.entities.User();
        user.setIdUser(userId);

        com.feri.watchmyparent.mobile.domain.entities.SensorData sensorData =
                new com.feri.watchmyparent.mobile.domain.entities.SensorData();

        sensorData.setIdSensorData(java.util.UUID.randomUUID().toString());
        sensorData.setUser(user);
        sensorData.setSensorType(reading.getSensorType());
        sensorData.setValue(reading.getValue());
        sensorData.setUnit(reading.getUnit() != null ? reading.getUnit() : reading.getSensorType().getUnit());
        sensorData.setTimestamp(reading.getTimestamp() != null ? reading.getTimestamp() : LocalDateTime.now());
        sensorData.setDeviceId(reading.getDeviceId());
        sensorData.setTransmissionStatus(com.feri.watchmyparent.mobile.domain.enums.TransmissionStatus.PENDING);

        // Build metadata
        StringBuilder metadata = new StringBuilder();
        metadata.append("criticality=").append(criticalityLevel.name());
        metadata.append(",frequency=").append(criticalityLevel.getDefaultFrequencySeconds()).append("s");

        if (reading.getConnectionType() != null) {
            metadata.append(",connection=").append(reading.getConnectionType());
        }
        if (reading.getMetadata() != null) {
            metadata.append(",").append(reading.getMetadata());
        }

        sensorData.setMetadata(metadata.toString());

        return sensorData;
    }

    // Collect all CRITICAL sensors (30 second frequency)
    public CompletableFuture<List<SensorReading>> collectCriticalSensors() {
        return collectSensorDataByCriticality(com.feri.watchmyparent.mobile.domain.enums.CriticalityLevel.CRITICAL);
    }

    // Collect all IMPORTANT sensors (2 minute frequency)
    public CompletableFuture<List<SensorReading>> collectImportantSensors() {
        return collectSensorDataByCriticality(com.feri.watchmyparent.mobile.domain.enums.CriticalityLevel.IMPORTANT);
    }

    //Collect all REGULAR sensors (5 minute frequency)
    public CompletableFuture<List<SensorReading>> collectRegularSensors() {
        return collectSensorDataByCriticality(com.feri.watchmyparent.mobile.domain.enums.CriticalityLevel.REGULAR);
    }

    // Collect all LONG_TERM sensors (15 minute frequency)
    public CompletableFuture<List<SensorReading>> collectLongTermSensors() {
        return collectSensorDataByCriticality(com.feri.watchmyparent.mobile.domain.enums.CriticalityLevel.LONG_TERM);
    }

    // Get data source for a specific sensor
    public String getDataSourceForSensor(SensorType sensorType) {
        if (SAMSUNG_HEALTH_PERMITTED.contains(sensorType)) {
            return "Samsung Health Data SDK (Permitted)";
        } else if (HEALTH_CONNECT_SENSORS.contains(sensorType)) {
            return "Health Connect / Hardware Sensors";
        } else {
            return "Unknown";
        }
    }

    // Get service status
    public String getServiceStatus() {
        StringBuilder status = new StringBuilder();
        status.append("Sensor Data Integration Service:\n");
        status.append("- Samsung Health SDK: ").append(samsungHealthDataService.isConnected() ? "✅" : "❌").append("\n");
        status.append("- Watch Manager: ").append(watchManager.isConnected() ? "✅" : "❌").append("\n");
        status.append("- PostgreSQL: ").append(postgreSQLDataService.getConnectionStatus()).append("\n");
        status.append("- Permitted sensors: ").append(SAMSUNG_HEALTH_PERMITTED.size()).append("\n");
        status.append("- Other sensors: ").append(HEALTH_CONNECT_SENSORS.size());
        return status.toString();
    }
}
