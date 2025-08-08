package com.feri.watchmyparent.mobile.infrastructure.services;

import android.util.Log;

import com.feri.watchmyparent.mobile.application.interfaces.DataTransmissionService;
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
//Eliminat PostgreSQL direct - totul merge prin DataTransmissionService
//MODIFICAT: Orchestrează colectarea și transmiterea DOAR prin Kafka
@Singleton
public class SensorDataIntegrationService {

    private static final String TAG = "SensorDataIntegration";

    private final RealSamsungHealthManager watchManager;
    private final SamsungHealthDataService samsungHealthDataService;
    private final DataTransmissionService dataTransmissionService;

    // Samsung Health permitted sensors (from your developer agreement)
    private final Set<SensorType> SAMSUNG_HEALTH_PERMITTED = new HashSet<>(Arrays.asList(
            SensorType.HEART_RATE,        // com.samsung.health.heart_rate
            SensorType.BLOOD_OXYGEN,      // com.samsung.health.oxygen_saturation
            SensorType.BLOOD_PRESSURE,    // com.samsung.health.blood_pressure
            SensorType.BODY_TEMPERATURE,  // com.samsung.health.body_temperature
            SensorType.SLEEP,             // com.samsung.health.sleep
            SensorType.STEP_COUNT         // com.samsung.health.step_count (din Exercise)
    ));

    // Android Sensor API sensors / Hardware sensors (remaining sensors)
    private final Set<SensorType> ANDROID_SENSOR_API = new HashSet<>(Arrays.asList(
            SensorType.ACCELEROMETER,
            SensorType.GYROSCOPE,
            SensorType.GRAVITY,
            SensorType.LINEAR_ACCELERATION,
            SensorType.ROTATION,
            SensorType.ORIENTATION,
            SensorType.MAGNETIC_FIELD,
            SensorType.LIGHT,
            SensorType.PROXIMITY,
            SensorType.LOCATION,
            SensorType.FALL_DETECTION,
            SensorType.STRESS
    ));

       @Inject
       public SensorDataIntegrationService(
               RealSamsungHealthManager watchManager,
               SamsungHealthDataService samsungHealthDataService,
               DataTransmissionService dataTransmissionService) { // ✅ ÎNLOCUIT PostgreSQLDataService

           this.watchManager = watchManager;
           this.samsungHealthDataService = samsungHealthDataService;
           this.dataTransmissionService = dataTransmissionService;

           Log.d(TAG, "✅ SensorDataIntegrationService initialized with Kafka-only pipeline");
           Log.d(TAG, "📊 Samsung Health permitted sensors: " + SAMSUNG_HEALTH_PERMITTED.size());
           Log.d(TAG, "📊 Android Sensor API sensors: " + ANDROID_SENSOR_API.size());
       }

    //Collect sensor data based on criticality levels and data sources
       //Trimite valorile sensorilor DOAR prin Kafka
    public CompletableFuture<List<SensorReading>> collectSensorDataByCriticality(
            com.feri.watchmyparent.mobile.domain.enums.CriticalityLevel criticalityLevel) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                List<SensorType> sensorsToRead = getSensorsByCriticality(criticalityLevel);
                Log.d(TAG, "📊 Collecting " + criticalityLevel.name() + " sensors: " + sensorsToRead.size());

                List<SensorReading> allReadings = new ArrayList<>();

                // ✅ PHASE 1: Samsung Health SDK (6 permitted sensors)
                List<SensorType> samsungHealthSensors = sensorsToRead.stream()
                        .filter(SAMSUNG_HEALTH_PERMITTED::contains)
                        .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

                if (!samsungHealthSensors.isEmpty()) {
                    List<SensorReading> samsungReadings = collectFromSamsungHealth(samsungHealthSensors);
                    allReadings.addAll(samsungReadings);
                    Log.d(TAG, "✅ Samsung Health SDK: " + samsungReadings.size() + " readings");
                }

                // ✅ PHASE 2: Android Sensor API (12 sensors)
                List<SensorType> androidSensors = sensorsToRead.stream()
                        .filter(ANDROID_SENSOR_API::contains)
                        .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

                if (!androidSensors.isEmpty()) {
                    List<SensorReading> androidReadings = collectFromAndroidSensors(androidSensors);
                    allReadings.addAll(androidReadings);
                    Log.d(TAG, "✅ Android Sensor API: " + androidReadings.size() + " readings");
                }

                // ✅ PHASE 3: Transmite prin Kafka DOAR (eliminat PostgreSQL)
                if (!allReadings.isEmpty()) {
                    transmitThroughKafkaOnly(allReadings, criticalityLevel);
                }

                Log.d(TAG, "✅ Total collected for " + criticalityLevel.name() + ": " + allReadings.size() + " readings");
                return allReadings;

            } catch (Exception e) {
                Log.e(TAG, "❌ Error collecting sensor data for " + criticalityLevel.name(), e);
                return new ArrayList<>();
            }
        });
    }

       // MODIFICAT: Transmite DOAR prin Kafka (eliminat PostgreSQL)
       private void transmitThroughKafkaOnly(List<SensorReading> readings,
                                             com.feri.watchmyparent.mobile.domain.enums.CriticalityLevel criticalityLevel) {
           try {
               Log.d(TAG, "📤 Transmitting " + readings.size() + " readings through Kafka-only pipeline...");

               for (SensorReading reading : readings) {
                   // Convertește în SensorDataDTO
                   com.feri.watchmyparent.mobile.application.dto.SensorDataDTO sensorDataDTO =
                           convertToSensorDataDTO(reading, "demo-user-id", criticalityLevel);

                   // ✅ Transmite DOAR prin Kafka via DataTransmissionService
                   dataTransmissionService.transmitData(sensorDataDTO, "demo-user-id")
                           .thenAccept(success -> {
                               if (success) {
                                   Log.d(TAG, "✅ Kafka transmission successful: " + reading.getSensorType());
                               } else {
                                   Log.w(TAG, "⚠️ Kafka transmission failed (will retry): " + reading.getSensorType());
                               }
                           })
                           .exceptionally(throwable -> {
                               Log.e(TAG, "❌ Kafka transmission error: " + reading.getSensorType(), throwable);
                               return null;
                           });
               }

               Log.d(TAG, "✅ All readings submitted to Kafka-only pipeline");

           } catch (Exception e) {
               Log.e(TAG, "❌ Error in Kafka-only transmission", e);
           }
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

       private List<SensorReading> collectFromAndroidSensors(List<SensorType> sensors) {
           List<SensorReading> readings = new ArrayList<>();
           try {
               Log.d(TAG, "🤖 Collecting from Android Sensor API: " + sensors.size() + " sensors");

               List<SensorReading> androidReadings = watchManager.readSensorData(sensors).join();
               for (SensorReading reading : androidReadings) {
                   if (reading != null) {
                       reading.setDeviceId("samsung_galaxy_watch_7");
                       if (reading.getConnectionType() == null) {
                           reading.setConnectionType("ANDROID_SENSOR_API");
                       }
                       reading.setMetadata("source=android_sensor_api,permitted=false");
                       readings.add(reading);

                       Log.d(TAG, "📊 Android Sensor: " + reading.getSensorType() + " = " +
                               String.format("%.2f", reading.getValue()) + " " + reading.getSensorType().getUnit());
                   }
               }
           } catch (Exception e) {
               Log.e(TAG, "❌ Error collecting from Android Sensor API", e);
           }
           return readings;
       }

       private com.feri.watchmyparent.mobile.application.dto.SensorDataDTO convertToSensorDataDTO(
               SensorReading reading, String userId,
               com.feri.watchmyparent.mobile.domain.enums.CriticalityLevel criticalityLevel) {

           com.feri.watchmyparent.mobile.application.dto.SensorDataDTO dto =
                   new com.feri.watchmyparent.mobile.application.dto.SensorDataDTO();

           dto.setUserId(userId);
           dto.setSensorType(reading.getSensorType());
           dto.setValue(reading.getValue());
           dto.setUnit(reading.getUnit() != null ? reading.getUnit() : reading.getSensorType().getUnit());
           dto.setTimestamp(reading.getTimestamp() != null ? reading.getTimestamp() : LocalDateTime.now());
           dto.setDeviceId(reading.getDeviceId());
           dto.setTransmitted(false);

           return dto;
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
        } else if (ANDROID_SENSOR_API.contains(sensorType)) {
            return "Android Sensor API";
        } else {
            return "Unknown";
        }
    }

    // Get service status
    public String getServiceStatus() {
        StringBuilder status = new StringBuilder();
        status.append("Sensor Data Integration Service (Kafka-Only):\n");
        status.append("- Samsung Health SDK: ").append(samsungHealthDataService.isConnected() ? "✅" : "❌").append("\n");
        status.append("- Watch Manager: ").append(watchManager.isConnected() ? "✅" : "❌").append("\n");
        status.append("- Data Transmission: ✅ Kafka-Only Pipeline\n");
        status.append("- Permitted sensors: ").append(SAMSUNG_HEALTH_PERMITTED.size()).append("\n");
        status.append("- Android sensors: ").append(ANDROID_SENSOR_API.size());
        return status.toString();
    }
}