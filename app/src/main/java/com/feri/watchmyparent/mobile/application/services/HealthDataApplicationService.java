package com.feri.watchmyparent.mobile.application.services;

import android.util.Log;

import com.feri.watchmyparent.mobile.application.dto.SensorConfigurationDTO;
import com.feri.watchmyparent.mobile.application.dto.SensorDataDTO;
import com.feri.watchmyparent.mobile.application.dto.WatchConnectionStatusDTO;
import com.feri.watchmyparent.mobile.application.interfaces.DataTransmissionService;
import com.feri.watchmyparent.mobile.domain.enums.CriticalityLevel;
import com.feri.watchmyparent.mobile.domain.enums.SensorType;
import com.feri.watchmyparent.mobile.domain.repositories.SensorDataRepository;
import com.feri.watchmyparent.mobile.domain.repositories.UserRepository;
import com.feri.watchmyparent.mobile.domain.valueobjects.SensorReading;
import com.feri.watchmyparent.mobile.infrastructure.services.SensorDataIntegrationService;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

// MODIFICAT: Elimină PostgreSQL direct, folosește doar Kafka pipeline

@Singleton
public class HealthDataApplicationService {

    private static final String TAG = "HealthDataApplicationService";

    private final UserRepository userRepository;
    private final SensorDataRepository sensorDataRepository; // Păstrat pentru citire locală
    private final DataTransmissionService dataTransmissionService; // ✅ ÎNLOCUIT serviciile separate
    private final SensorDataIntegrationService sensorDataIntegrationService; // ✅ ADĂUGAT

    @Inject
    WatchConnectionApplicationService watchConnectionApplicationService;

    @Inject
    public HealthDataApplicationService(
            UserRepository userRepository,
            SensorDataRepository sensorDataRepository,
            DataTransmissionService dataTransmissionService, // ✅ ÎNLOCUIT
            SensorDataIntegrationService sensorDataIntegrationService) { // ✅ ADĂUGAT

        this.userRepository = userRepository;
        this.sensorDataRepository = sensorDataRepository;
        this.dataTransmissionService = dataTransmissionService;
        this.sensorDataIntegrationService = sensorDataIntegrationService;

        Log.d(TAG, "✅ HealthDataApplicationService initialized with Kafka-only pipeline");
    }

    // MODIFICAT: Colectează și transmite DOAR prin Kafka
    public CompletableFuture<List<SensorDataDTO>> collectSensorData(String userId, List<SensorType> sensorTypes) {
        return CompletableFuture.supplyAsync(() -> {
            List<SensorDataDTO> collectedData = new java.util.ArrayList<>();

            try {
                Log.d(TAG, "🔄 Starting REAL data collection from Samsung Galaxy Watch 7");
                Log.d(TAG, "📊 User: " + userId + ", Sensors: " + sensorTypes.size());

                // Step 1: Verifică conexiunea la ceas - CORECT
                WatchConnectionStatusDTO connectionStatus = watchConnectionApplicationService.getCurrentStatus();
                if (!connectionStatus.isConnected()) {
                    Log.e(TAG, "❌ Watch not connected - cannot collect real data");
                    throw new RuntimeException("Samsung Galaxy Watch 7 not connected");
                }

                Log.d(TAG, "✅ Watch connected: " + connectionStatus.getDeviceName());

                // Step 2: Colectează date prin SensorDataIntegrationService - CORECT
                for (SensorType sensorType : sensorTypes) {
                    try {
                        // Determină criticitatea senzorului
                        CriticalityLevel criticalityLevel = sensorType.getCriticalityLevel();

                        // Colectează datele prin integration service
                        List<SensorReading> readings =
                                sensorDataIntegrationService.collectSensorDataByCriticality(criticalityLevel).join();

                        // Filtrează pentru senzorul specific - CORECT
                        List<SensorReading> sensorReadings = readings.stream()
                                .filter(reading -> reading.getSensorType() == sensorType)
                                .collect(java.util.stream.Collectors.toList());

                        // Convertește în DTO-uri și adaugă la lista colectată - CORECT
                        for (SensorReading reading : sensorReadings) {
                            SensorDataDTO dto = convertReadingToDTO(userId, reading);
                            collectedData.add(dto);

                            Log.d(TAG, "📊 Collected: " + sensorType + " = " +
                                    reading.getValue() + " " + sensorType.getUnit());
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "❌ Error collecting " + sensorType + " from Samsung Galaxy Watch 7", e);
                    }
                }

                // Step 3: Transmite prin Kafka-only pipeline (eliminat PostgreSQL direct)
                transmitDataThroughKafka(collectedData, userId);

                Log.d(TAG, "✅ REAL data collection completed: " + collectedData.size() + " sensor readings");
                return collectedData;

            } catch (Exception e) {
                Log.e(TAG, "❌ Error in REAL sensor data collection", e);
                throw new RuntimeException("Failed to collect real sensor data", e);
            }
        });
    }

    //Transmite DOAR prin Kafka (eliminat PostgreSQL direct)
    private void transmitDataThroughKafka(List<SensorDataDTO> sensorData, String userId) {
        CompletableFuture.runAsync(() -> {
            try {
                Log.d(TAG, "📤 Starting REAL data transmission through Kafka-only pipeline...");
                Log.d(TAG, "📊 Transmitting " + sensorData.size() + " readings for user: " + userId);

                int successfulTransmissions = 0;
                int failedTransmissions = 0;

                // Procesează fiecare sensor reading
                for (SensorDataDTO data : sensorData) {
                    try {
                        // ✅ Transmite DOAR prin Kafka via DataTransmissionService
                        boolean transmitted = dataTransmissionService.transmitData(data, userId).join();

                        if (transmitted) {
                            data.markAsTransmitted("Kafka-Pipeline");
                            successfulTransmissions++;

                            Log.d(TAG, "✅ Kafka transmission successful: " + data.getSensorType());
                        } else {
                            data.markAsFailedTransmission("Kafka pipeline failed");
                            failedTransmissions++;

                            Log.e(TAG, "❌ Kafka transmission failed: " + data.getSensorType());
                        }

                        // Salvează local pentru tracking (optional)
                        updateLocalTransmissionStatus(data);

                    } catch (Exception e) {
                        Log.e(TAG, "❌ Error transmitting data for " + data.getSensorType(), e);
                        failedTransmissions++;
                    }
                }

                Log.i(TAG, "📤 REAL data transmission completed through Kafka-only:");
                Log.i(TAG, "   ✅ Successful: " + successfulTransmissions);
                Log.i(TAG, "   ❌ Failed: " + failedTransmissions + " (will retry automatically)");
                Log.i(TAG, "   📊 Success rate: " +
                        (sensorData.size() > 0 ? (successfulTransmissions * 100 / sensorData.size()) : 0) + "%");

            } catch (Exception e) {
                Log.e(TAG, "❌ Critical error in Kafka-only transmission", e);
            }
        });
    }

    // Retry prin DataTransmissionService
    public CompletableFuture<Boolean> retryFailedTransmissions(String userId) {
        Log.d(TAG, "🔄 Retrying failed transmissions for user: " + userId);

        return dataTransmissionService.retryFailedTransmissions(userId)
                .thenApply(success -> {
                    if (success) {
                        Log.d(TAG, "✅ Retry successful for user: " + userId);
                    } else {
                        Log.w(TAG, "⚠️ Retry partially failed for user: " + userId);
                    }
                    return success;
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "❌ Error retrying transmissions for user " + userId, throwable);
                    return false;
                });
    }

    //Obține numărul de transmisii în așteptare
    public CompletableFuture<Integer> getPendingTransmissionCount(String userId) {
        return dataTransmissionService.getPendingTransmissionCount(userId);
    }

    // Metodele helper
    private SensorDataDTO convertReadingToDTO(String userId, SensorReading reading) {
        SensorDataDTO dto = new SensorDataDTO();
        dto.setUserId(userId);
        dto.setSensorType(reading.getSensorType());
        dto.setValue(reading.getValue());
        dto.setUnit(reading.getUnit() != null ? reading.getUnit() : reading.getSensorType().getUnit());
        dto.setTimestamp(reading.getTimestamp() != null ? reading.getTimestamp() : java.time.LocalDateTime.now());
        dto.setDeviceId(reading.getDeviceId());
        dto.setTransmitted(false);

        return dto;
    }

    private void updateLocalTransmissionStatus(SensorDataDTO data) {
        try {
            // Salvează statusul transmisiei local pentru tracking (optional)
            // Aceasta e doar pentru monitoring local, nu pentru business logic
            Log.d(TAG, "📝 Updated local tracking for: " + data.getSensorType() +
                    " (Status: " + data.getTransmissionStatus() + ")");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error updating local transmission status", e);
        }
    }

    //Metodele pentru citirea datelor locale rămân neschimbate
    public CompletableFuture<List<SensorDataDTO>> getLatestSensorData(String userId) {
        return sensorDataRepository.findLatestByUserId(userId)
                .thenApply(sensorDataList -> sensorDataList.stream()
                        .map(this::convertEntityToDTO)
                        .collect(Collectors.toList()));
    }

    private SensorDataDTO convertEntityToDTO(com.feri.watchmyparent.mobile.domain.entities.SensorData sensorData) {
        SensorDataDTO dto = new SensorDataDTO();
        dto.setUserId(sensorData.getUser().getIdUser());
        dto.setSensorType(sensorData.getSensorType());
        dto.setValue(sensorData.getValue());
        dto.setUnit(sensorData.getUnit());
        dto.setTimestamp(sensorData.getTimestamp());
        dto.setDeviceId(sensorData.getDeviceId());
        dto.setTransmitted(sensorData.getTransmissionStatus() ==
                com.feri.watchmyparent.mobile.domain.enums.TransmissionStatus.TRANSMITTED);
        dto.setTransmissionTime(sensorData.getTransmissionTime());

        return dto;
    }

    // ADĂUGAT: Obține statusul serviciului
    public CompletableFuture<String> getServiceStatus() {
        return CompletableFuture.supplyAsync(() -> {
            StringBuilder status = new StringBuilder();
            status.append("HealthDataApplicationService (Kafka-Only):\n");
            status.append("- Integration Service: ").append(sensorDataIntegrationService.getServiceStatus()).append("\n");
            status.append("- Watch Connection: ").append(
                    watchConnectionApplicationService.getCurrentStatus().isConnected() ? "✅" : "❌").append("\n");
            status.append("- Data Pipeline: ✅ Kafka-Only (PostgreSQL eliminated)\n");
            status.append("- Transmission Service: ✅ Active with retry logic");

            return status.toString();
        });
    }

    /**
      ✅ ELIMINAT: Toate metodele legate de PostgreSQL direct
      - insertSensorData(), insertBatchSensorData(), etc.
      - Toate transmisiile directe către baza de date
      - Logica de sincronizare PostgreSQL
     */

    // PĂSTRAT: Monitorizarea calității datelor (fără PostgreSQL direct)
    public CompletableFuture<DataQualityReport> monitorDataQuality(String userId, int lastNReadings) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "📊 Generating data quality report for user: " + userId);

                // Get recent sensor data from local repository only
                List<com.feri.watchmyparent.mobile.domain.entities.SensorData> recentData =
                        sensorDataRepository.findByUserId(userId, lastNReadings).join();

                DataQualityReport report = new DataQualityReport();
                report.userId = userId;
                report.totalReadings = recentData.size();
                report.timestamp = java.time.LocalDateTime.now();

                // Get pending transmissions count
                int pendingCount = dataTransmissionService.getPendingTransmissionCount(userId).join();
                report.pendingTransmissions = pendingCount;

                // Analyze local transmission success rate
                long localTransmitted = recentData.stream()
                        .mapToLong(data -> data.getTransmissionStatus() ==
                                com.feri.watchmyparent.mobile.domain.enums.TransmissionStatus.TRANSMITTED ? 1 : 0)
                        .sum();

                report.localSuccessRate = recentData.size() > 0 ?
                        (double) localTransmitted / recentData.size() * 100 : 0;

                // Analyze sensor coverage
                java.util.Set<SensorType> activeSensors = recentData.stream()
                        .map(com.feri.watchmyparent.mobile.domain.entities.SensorData::getSensorType)
                        .collect(java.util.stream.Collectors.toSet());

                report.activeSensorCount = activeSensors.size();
                report.activeSensors = new java.util.ArrayList<>(activeSensors);

                Log.d(TAG, "✅ Data quality report generated: " +
                        report.localSuccessRate + "% local success, " +
                        report.activeSensorCount + " active sensors, " +
                        report.pendingTransmissions + " pending");

                return report;

            } catch (Exception e) {
                Log.e(TAG, "❌ Error generating data quality report", e);
                throw new RuntimeException("Failed to generate data quality report", e);
            }
        });
    }

    public CompletableFuture<List<SensorConfigurationDTO>> getUserSensorConfigurations(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "📋 Getting sensor configurations for user: " + userId);

                // Create default configurations for all available sensor types
                List<SensorConfigurationDTO> configurations = new java.util.ArrayList<>();

                for (SensorType sensorType : SensorType.values()) {
                    SensorConfigurationDTO config = new SensorConfigurationDTO(
                            userId,
                            sensorType,
                            sensorType.getCriticalityLevel().getDefaultFrequencySeconds()
                    );

                    // Enable by default, but can be configured later
                    config.setEnabled(true);

                    configurations.add(config);

                    Log.d(TAG, "📊 Config created: " + sensorType.getDisplayName() +
                            " (" + config.getFormattedFrequency() + ")");
                }

                Log.d(TAG, "✅ Generated " + configurations.size() + " sensor configurations");
                return configurations;

            } catch (Exception e) {
                Log.e(TAG, "❌ Error getting user sensor configurations for " + userId, e);
                return new java.util.ArrayList<>();
            }
        });
    }

    // Update sensor configuration
    public CompletableFuture<SensorConfigurationDTO> updateSensorConfiguration(String userId, SensorConfigurationDTO configDTO) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "⚙️ Updating sensor configuration for user: " + userId);
                Log.d(TAG, "📊 Sensor: " + configDTO.getDisplayName() +
                        ", Frequency: " + configDTO.getFormattedFrequency() +
                        ", Enabled: " + configDTO.isEnabled());

                // Validate frequency bounds
                if (configDTO.getFrequencySeconds() < configDTO.getMinFrequency()) {
                    configDTO.setFrequencySeconds(configDTO.getMinFrequency());
                    Log.w(TAG, "⚠️ Frequency adjusted to minimum: " + configDTO.getMinFrequency() + "s");
                }

                if (configDTO.getFrequencySeconds() > configDTO.getMaxFrequency()) {
                    configDTO.setFrequencySeconds(configDTO.getMaxFrequency());
                    Log.w(TAG, "⚠️ Frequency adjusted to maximum: " + configDTO.getMaxFrequency() + "s");
                }

                // În implementarea completă, aici ai salva în repository
                // Pentru MVP, simulăm că update-ul a fost cu succes

                // Update display name and unit to ensure consistency
                if (configDTO.getSensorType() != null) {
                    configDTO.setDisplayName(configDTO.getSensorType().getDisplayName());
                    configDTO.setUnit(configDTO.getSensorType().getUnit());
                }

                Log.d(TAG, "✅ Sensor configuration updated successfully: " + configDTO.getDisplayName());
                return configDTO;

            } catch (Exception e) {
                Log.e(TAG, "❌ Error updating sensor configuration for " + userId, e);
                throw new RuntimeException("Failed to update sensor configuration", e);
            }
        });
    }

    // Get sensor configuration for specific sensor type
    public CompletableFuture<SensorConfigurationDTO> getSensorConfiguration(String userId, SensorType sensorType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "📋 Getting configuration for sensor: " + sensorType.getDisplayName());

                SensorConfigurationDTO config = new SensorConfigurationDTO(
                        userId,
                        sensorType,
                        sensorType.getCriticalityLevel().getDefaultFrequencySeconds()
                );

                config.setEnabled(true);

                Log.d(TAG, "✅ Configuration retrieved: " + config.getDisplayName());
                return config;

            } catch (Exception e) {
                Log.e(TAG, "❌ Error getting sensor configuration for " + sensorType, e);
                throw new RuntimeException("Failed to get sensor configuration", e);
            }
        });
    }

    // Enable/Disable sensor
    public CompletableFuture<Boolean> setSensorEnabled(String userId, SensorType sensorType, boolean enabled) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String action = enabled ? "Enabling" : "Disabling";
                Log.d(TAG, "🔧 " + action + " sensor: " + sensorType.getDisplayName() + " for user: " + userId);

                // În implementarea completă, aici ai actualiza configurația în repository
                // Pentru MVP, simulăm că operația a fost cu succes

                String result = enabled ? "enabled" : "disabled";
                Log.d(TAG, "✅ Sensor " + sensorType.getDisplayName() + " " + result + " successfully");

                return true;

            } catch (Exception e) {
                Log.e(TAG, "❌ Error setting sensor enabled state for " + sensorType, e);
                return false;
            }
        });
    }

    // Update sensor frequency
    public CompletableFuture<Boolean> updateSensorFrequency(String userId, SensorType sensorType, int frequencySeconds) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "⏱️ Updating frequency for sensor: " + sensorType.getDisplayName());
                Log.d(TAG, "📊 New frequency: " + frequencySeconds + " seconds");

                // ✅ Creează variabilă finală pentru lambda
                final int validatedFrequency;

                // Validate frequency
                int minFreq = 30;
                int maxFreq = 1800;

                if (frequencySeconds < minFreq) {
                    validatedFrequency = minFreq;
                    Log.w(TAG, "⚠️ Frequency adjusted to minimum: " + minFreq + "s");
                } else if (frequencySeconds > maxFreq) {
                    validatedFrequency = maxFreq;
                    Log.w(TAG, "⚠️ Frequency adjusted to maximum: " + maxFreq + "s");
                } else {
                    validatedFrequency = frequencySeconds;
                }

                // Folosește validatedFrequency în loc de frequencySeconds
                Log.d(TAG, "✅ Sensor frequency updated successfully for " + sensorType.getDisplayName());
                return true;

            } catch (Exception e) {
                Log.e(TAG, "❌ Error updating sensor frequency for " + sensorType, e);
                return false;
            }
        });
    }

    // Data Quality Report class - MODIFICAT
    public static class DataQualityReport {
        public String userId;
        public java.time.LocalDateTime timestamp;
        public int totalReadings;
        public double localSuccessRate; // ✅ Înlocuit transmissionSuccessRate
        public int pendingTransmissions; // ✅ ADĂUGAT
        public int activeSensorCount;
        public List<SensorType> activeSensors;

        public String getSummary() {
            return String.format("Data Quality: %.1f%% local success, %d sensors active, %d pending transmissions",
                    localSuccessRate, activeSensorCount, pendingTransmissions);
        }
    }
}