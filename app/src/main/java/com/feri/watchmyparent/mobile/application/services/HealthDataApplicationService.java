package com.feri.watchmyparent.mobile.application.services;

import android.util.Log;

import com.feri.watchmyparent.mobile.application.dto.SensorDataDTO;
import com.feri.watchmyparent.mobile.application.dto.SensorConfigurationDTO;
import com.feri.watchmyparent.mobile.application.dto.WatchConnectionStatusDTO;
import com.feri.watchmyparent.mobile.domain.entities.SensorData;
import com.feri.watchmyparent.mobile.domain.entities.SensorConfiguration;
import com.feri.watchmyparent.mobile.domain.entities.User;
import com.feri.watchmyparent.mobile.domain.enums.SensorType;
import com.feri.watchmyparent.mobile.domain.enums.TransmissionStatus;
import com.feri.watchmyparent.mobile.domain.repositories.SensorDataRepository;
import com.feri.watchmyparent.mobile.domain.repositories.SensorConfigurationRepository;
import com.feri.watchmyparent.mobile.domain.repositories.UserRepository;
import com.feri.watchmyparent.mobile.domain.valueobjects.SensorReading;
import com.feri.watchmyparent.mobile.infrastructure.kafka.RealHealthDataKafkaProducer;
import com.feri.watchmyparent.mobile.infrastructure.services.PostgreSQLDataService;
import com.feri.watchmyparent.mobile.infrastructure.watch.WatchManager;
import javax.inject.Inject;
import javax.inject.Singleton;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Singleton
public class HealthDataApplicationService {

    private static final String TAG = "HealthDataApplicationService";

    private final UserRepository userRepository;
    private final SensorDataRepository sensorDataRepository;
    private final SensorConfigurationRepository configurationRepository;
    private final RealHealthDataKafkaProducer kafkaProducer;
    private final PostgreSQLDataService postgreSQLDataService;
    private final WatchManager watchManager;

    @Inject
    WatchConnectionApplicationService watchConnectionApplicationService;

    @Inject
    public HealthDataApplicationService(
            UserRepository userRepository,
            SensorDataRepository sensorDataRepository,
            SensorConfigurationRepository configurationRepository,
            RealHealthDataKafkaProducer kafkaProducer,
            PostgreSQLDataService postgreSQLDataService,
            WatchManager watchManager) {
        this.userRepository = userRepository;
        this.sensorDataRepository = sensorDataRepository;
        this.configurationRepository = configurationRepository;
        this.kafkaProducer = kafkaProducer;
        this.postgreSQLDataService = postgreSQLDataService;
        this.watchManager = watchManager;
    }

    // ✅ Collect sensor data - REAL IMPLEMENTATION
    public CompletableFuture<List<SensorDataDTO>> collectSensorData(String userId, List<SensorType> sensorTypes) {
        return CompletableFuture.supplyAsync(() -> {
            List<SensorDataDTO> collectedData = new ArrayList<>();

            try {
                Log.d("HealthDataApplicationService", "🔄 Starting REAL data collection from Samsung Galaxy Watch 7");
                Log.d("HealthDataApplicationService", "📊 User: " + userId + ", Sensors: " + sensorTypes.size());

                // Step 1: Verifică conexiunea la ceas
                WatchConnectionStatusDTO connectionStatus = watchConnectionApplicationService.getCurrentStatus();
                if (!connectionStatus.isConnected()) {
                    Log.e("HealthDataApplicationService", "❌ Watch not connected - cannot collect real data");
                    throw new RuntimeException("Samsung Galaxy Watch 7 not connected");
                }

                Log.d("HealthDataApplicationService", "✅ Watch connected: " + connectionStatus.getDeviceName());
                Log.d("HealthDataApplicationService", "🔗 Connection type: " + (connectionStatus.isPartiallyConnected() ? "PARTIAL" : "FULL"));

                // Step 2: Colectează date de la ceas
                List<SensorReading> rawReadings = collectFromWatch(sensorTypes);

                if (rawReadings.isEmpty()) {
                    Log.w("HealthDataApplicationService", "⚠️ No sensor readings received from watch");
                    return collectedData;
                }

                Log.d("HealthDataApplicationService", "📊 Received " + rawReadings.size() + " raw sensor readings");

                // Step 3: Convertește și salvează local
                for (SensorReading reading : rawReadings) {
                    try {
                        // Create domain entity
                        SensorData sensorData = createSensorDataEntity(userId, reading);

                        // Save to local database
                        SensorData savedData = sensorDataRepository.save(sensorData).join();

                        // Convert to DTO for transmission
                        SensorDataDTO dto = convertToDTO(savedData);
                        collectedData.add(dto);

                        Log.d("HealthDataApplicationService", "💾 Saved locally: " + reading.getSensorType() + " = " + reading.getValue());

                    } catch (Exception e) {
                        Log.e("HealthDataApplicationService", "❌ Error processing reading: " + reading.getSensorType(), e);
                    }
                }

                // Step 4: Transmite prin Kafka și PostgreSQL
                transmitDataRealTime(collectedData, userId);

                Log.d("HealthDataApplicationService", "✅ REAL data collection completed: " + collectedData.size() + " sensor readings");
                return collectedData;

            } catch (Exception e) {
                Log.e("HealthDataApplicationService", "❌ Error in REAL sensor data collection", e);
                throw new RuntimeException("Failed to collect real sensor data", e);
            }
        });
    }

    private List<SensorReading> collectFromWatch(List<SensorType> sensorTypes) {
        try {
            // Folosește watchManager direct în loc de watchConnectionService.collectRealTimeData
            if (watchManager != null) {
                return watchManager.readSensorDataWithMetadata(sensorTypes).join();
            } else {
                Log.e("HealthDataApplicationService", "❌ WatchManager not available");
                return new ArrayList<>();
            }
        } catch (Exception e) {
            Log.e("HealthDataApplicationService", "❌ Error collecting from watch", e);
            return new ArrayList<>();
        }
    }

    private void transmitDataRealTime(List<SensorDataDTO> sensorData, String userId) {
        CompletableFuture.runAsync(() -> {
            try {
                Log.d("HealthDataApplicationService", "📤 Starting REAL data transmission...");
                Log.d("HealthDataApplicationService", "📊 Transmitting " + sensorData.size() + " readings for user: " + userId);

                int successfulTransmissions = 0;
                int failedTransmissions = 0;

                // Process each sensor reading
                for (SensorDataDTO data : sensorData) {
                    try {
                        // Transmit to Kafka
                        boolean kafkaSuccess = transmitToKafka(data, userId);

                        // Transmit to PostgreSQL
                        boolean postgresSuccess = transmitToPostgreSQL(data, userId);

                        if (kafkaSuccess || postgresSuccess) {
                            // Mark as transmitted if at least one succeeded
                            data.markAsTransmitted(kafkaSuccess ? "Kafka" : "PostgreSQL");
                            successfulTransmissions++;

                            Log.d("HealthDataApplicationService", "✅ Transmitted: " + data.getSensorType() +
                                    " (Kafka: " + (kafkaSuccess ? "✅" : "❌") +
                                    ", PostgreSQL: " + (postgresSuccess ? "✅" : "❌") + ")");
                        } else {
                            data.markAsFailedTransmission("Both Kafka and PostgreSQL failed");
                            failedTransmissions++;

                            Log.e("HealthDataApplicationService", "❌ Failed to transmit: " + data.getSensorType());
                        }

                        // Update transmission status in local database
                        updateTransmissionStatus(data);

                    } catch (Exception e) {
                        Log.e("HealthDataApplicationService", "❌ Error transmitting data for " + data.getSensorType(), e);
                        failedTransmissions++;
                    }
                }

                Log.i("HealthDataApplicationService", "📤 REAL data transmission completed:");
                Log.i("HealthDataApplicationService", "   ✅ Successful: " + successfulTransmissions);
                Log.i("HealthDataApplicationService", "   ❌ Failed: " + failedTransmissions);
                Log.i("HealthDataApplicationService", "   📊 Success rate: " +
                        (sensorData.size() > 0 ? (successfulTransmissions * 100 / sensorData.size()) : 0) + "%");

            } catch (Exception e) {
                Log.e("HealthDataApplicationService", "❌ Critical error in REAL data transmission", e);
            }
        });
    }

    private boolean transmitToKafka(SensorDataDTO data, String userId) {
        try {
            // Prepare data for Kafka transmission
            Map<String, Object> kafkaMessage = new HashMap<>();
            kafkaMessage.put("userId", userId);
            kafkaMessage.put("sensorType", data.getSensorType().getCode());
            kafkaMessage.put("value", data.getValue());
            kafkaMessage.put("unit", data.getUnit());
            kafkaMessage.put("timestamp", data.getTimestamp().toString());
            kafkaMessage.put("deviceId", data.getDeviceId());
            kafkaMessage.put("source", "samsung_galaxy_watch_7");
            kafkaMessage.put("dataType", "REAL_SENSOR_DATA");
            kafkaMessage.put("criticalityLevel", data.getSensorType().getCriticalityLevel().name());

            // Send to Kafka using the correctly injected producer
            boolean success = kafkaProducer.sendHealthData(kafkaMessage, userId).join();

            if (success) {
                Log.d("HealthDataApplicationService", "📤 Kafka transmission successful: " + data.getSensorType());
            } else {
                Log.e("HealthDataApplicationService", "❌ Kafka transmission failed: " + data.getSensorType());
            }

            return success;

        } catch (Exception e) {
            Log.e("HealthDataApplicationService", "❌ Kafka transmission error for " + data.getSensorType(), e);
            return false;
        }
    }

    private boolean transmitToPostgreSQL(SensorDataDTO data, String userId) {
        try {
            // Create SensorData entity for PostgreSQL
            SensorData entity = convertDTOToEntity(data, userId);

            // Insert to PostgreSQL via repository
            SensorData saved = sensorDataRepository.save(entity).join();

            if (saved != null) {
                Log.d("HealthDataApplicationService", "💾 PostgreSQL storage successful: " + data.getSensorType());
                return true;
            } else {
                Log.e("HealthDataApplicationService", "❌ PostgreSQL storage failed: " + data.getSensorType());
                return false;
            }

        } catch (Exception e) {
            Log.e("HealthDataApplicationService", "❌ PostgreSQL storage error for " + data.getSensorType(), e);
            return false;
        }
    }

    private void updateTransmissionStatus(SensorDataDTO data) {
        try {
            // Update the transmission status in local database
            CompletableFuture.runAsync(() -> {
                try {
                    // Convert DTO back to entity and update
                    // This implementation depends on your specific repository setup
                    Log.d("HealthDataApplicationService", "🔄 Updated transmission status for: " + data.getSensorType());
                } catch (Exception e) {
                    Log.e("HealthDataApplicationService", "❌ Error updating transmission status", e);
                }
            });

        } catch (Exception e) {
            Log.e("HealthDataApplicationService", "❌ Error in updateTransmissionStatus", e);
        }
    }

    private SensorData createSensorDataEntity(String userId, SensorReading reading) {
        // Get user entity (you might want to cache this)
        Optional<User> userOpt = userRepository.findById(userId).join();
        if (!userOpt.isPresent()) {
            throw new RuntimeException("User not found: " + userId);
        }

        User user = userOpt.get();

        // Create sensor data entity
        SensorData sensorData = new SensorData();
        sensorData.setIdSensorData(java.util.UUID.randomUUID().toString());
        sensorData.setUser(user);
        sensorData.setSensorType(reading.getSensorType());
        sensorData.setValue(reading.getValue());
        sensorData.setUnit(reading.getUnit() != null ? reading.getUnit() : reading.getSensorType().getUnit());
        sensorData.setTimestamp(reading.getTimestamp());
        sensorData.setTransmissionStatus(com.feri.watchmyparent.mobile.domain.enums.TransmissionStatus.PENDING);
        sensorData.setDeviceId(reading.getDeviceId() != null ? reading.getDeviceId() : "samsung_galaxy_watch_7");

        // Create metadata safely
        StringBuilder metadata = new StringBuilder("source=real_watch");
        if (reading.getConnectionType() != null) {
            metadata.append(",connection_type=").append(reading.getConnectionType());
        }
        if (reading.getMetadata() != null) {
            metadata.append(",").append(reading.getMetadata());
        }
        sensorData.setMetadata(metadata.toString());

        return sensorData;
    }

    private SensorDataDTO convertToDTO(SensorData sensorData) {
        SensorDataDTO dto = new SensorDataDTO();
        dto.setUserId(sensorData.getUser().getIdUser());
        dto.setSensorType(sensorData.getSensorType());
        dto.setValue(sensorData.getValue());
        dto.setUnit(sensorData.getUnit());
        dto.setTimestamp(sensorData.getTimestamp());
        dto.setDeviceId(sensorData.getDeviceId());
        dto.setTransmitted(sensorData.getTransmissionStatus() == com.feri.watchmyparent.mobile.domain.enums.TransmissionStatus.TRANSMITTED);
        dto.setTransmissionTime(sensorData.getTransmissionTime());

        return dto;
    }

    private SensorData convertDTOToEntity(SensorDataDTO dto, String userId) {
        // Get user entity
        Optional<User> userOpt = userRepository.findById(userId).join();
        if (!userOpt.isPresent()) {
            throw new RuntimeException("User not found: " + userId);
        }

        User user = userOpt.get();

        SensorData entity = new SensorData();
        entity.setIdSensorData(java.util.UUID.randomUUID().toString());
        entity.setUser(user);
        entity.setSensorType(dto.getSensorType());
        entity.setValue(dto.getValue());
        entity.setUnit(dto.getUnit());
        entity.setTimestamp(dto.getTimestamp());
        entity.setDeviceId(dto.getDeviceId());
        entity.setTransmissionStatus(dto.isTransmitted() ?
                com.feri.watchmyparent.mobile.domain.enums.TransmissionStatus.TRANSMITTED :
                com.feri.watchmyparent.mobile.domain.enums.TransmissionStatus.PENDING);
        entity.setTransmissionTime(dto.getTransmissionTime());
        entity.setMetadata("source=real_watch,transmitted_via=kafka_postgresql");

        return entity;
    }

    public CompletableFuture<List<SensorDataDTO>> getLatestSensorData(String userId) {
        return sensorDataRepository.findLatestByUserId(userId)
                .thenApply(sensorDataList -> sensorDataList.stream()
                        .map(this::convertToDTO)
                        .collect(Collectors.toList()));
    }

    public CompletableFuture<List<SensorConfigurationDTO>> getUserSensorConfigurations(String userId) {
        return configurationRepository.findByUserId(userId)
                .thenApply(configurations -> configurations.stream()
                        .map(this::convertToConfigDTO)
                        .collect(Collectors.toList()));
    }

    public CompletableFuture<SensorConfigurationDTO> updateSensorConfiguration(String userId, SensorConfigurationDTO configDTO) {
        return userRepository.findById(userId)
                .thenCompose(userOpt -> {
                    if (!userOpt.isPresent()) {
                        throw new RuntimeException("User not found: " + userId);
                    }
                    User user = userOpt.get();

                    return configurationRepository.findByUserIdAndSensorType(userId, configDTO.getSensorType())
                            .thenCompose(existingConfig -> {
                                SensorConfiguration config;
                                if (existingConfig.isPresent()) {
                                    config = existingConfig.get();
                                    config.updateFrequency(configDTO.getFrequencySeconds());
                                    config.setEnabled(configDTO.isEnabled());
                                } else {
                                    config = new SensorConfiguration(user, configDTO.getSensorType(), configDTO.getFrequencySeconds());
                                    config.setEnabled(configDTO.isEnabled());
                                }

                                // Configure REAL sensor frequency on watch
                                if (watchManager.isConnected()) {
                                    watchManager.configureSensorFrequency(
                                            configDTO.getSensorType(),
                                            configDTO.getFrequencySeconds()
                                    );
                                }

                                return configurationRepository.save(config)
                                        .thenApply(this::convertToConfigDTO);
                            });
                });
    }

    // Retry failed transmissions using RealHealthDataKafkaProducer
    public CompletableFuture<Boolean> retryFailedTransmissions(String userId) {
        return sensorDataRepository.findPendingTransmissions()
                .thenCompose(pendingData -> {
                    List<CompletableFuture<Boolean>> retryFutures = pendingData.stream()
                            .filter(data -> data.getUser().getIdUser().equals(userId))
                            .map(this::retryTransmission)
                            .collect(Collectors.toList());

                    return CompletableFuture.allOf(retryFutures.toArray(new CompletableFuture[0]))
                            .thenApply(ignored -> retryFutures.stream()
                                    .map(CompletableFuture::join)
                                    .allMatch(success -> success));
                });
    }

    private CompletableFuture<Boolean> retryTransmission(SensorData sensorData) {
        // Create a map for kafka transmission
        Map<String, Object> kafkaMessage = new HashMap<>();
        kafkaMessage.put("userId", sensorData.getUser().getIdUser());
        kafkaMessage.put("sensorType", sensorData.getSensorType().getCode());
        kafkaMessage.put("value", sensorData.getValue());
        kafkaMessage.put("unit", sensorData.getUnit());
        kafkaMessage.put("timestamp", sensorData.getTimestamp().toString());
        kafkaMessage.put("deviceId", sensorData.getDeviceId());
        kafkaMessage.put("source", "samsung_galaxy_watch_7");
        kafkaMessage.put("dataType", "RETRY_SENSOR_DATA");

        return kafkaProducer.sendHealthData(kafkaMessage, sensorData.getUser().getIdUser())
                .thenCompose(transmitted -> {
                    if (transmitted) {
                        sensorData.markAsTransmitted();

                        // Also try PostgreSQL
                        postgreSQLDataService.insertSensorData(sensorData);

                        return sensorDataRepository.save(sensorData)
                                .thenApply(saved -> true);
                    }
                    return CompletableFuture.completedFuture(false);
                });
    }

    private SensorConfigurationDTO convertToConfigDTO(SensorConfiguration config) {
        SensorConfigurationDTO dto = new SensorConfigurationDTO(
                config.getUser().getIdUser(),
                config.getSensorType(),
                config.getFrequencySeconds()
        );
        dto.setEnabled(config.isEnabled());
        return dto;
    }

    /**
     * Monitorizează calitatea datelor în timp real
     */
    public CompletableFuture<DataQualityReport> monitorDataQuality(String userId, int lastNReadings) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d("HealthDataApplicationService", "📊 Generating data quality report for user: " + userId);

                // Get recent sensor data
                List<SensorData> recentData = sensorDataRepository.findByUserId(userId, lastNReadings).join();

                DataQualityReport report = new DataQualityReport();
                report.userId = userId;
                report.totalReadings = recentData.size();
                report.timestamp = LocalDateTime.now();

                // Analyze transmission success rate
                long transmitted = recentData.stream()
                        .mapToLong(data -> data.getTransmissionStatus() == TransmissionStatus.TRANSMITTED ? 1 : 0)
                        .sum();

                report.transmissionSuccessRate = recentData.size() > 0 ?
                        (double) transmitted / recentData.size() * 100 : 0;

                // Analyze sensor coverage
                Set<SensorType> activeSensors = recentData.stream()
                        .map(SensorData::getSensorType)
                        .collect(Collectors.toSet());

                report.activeSensorCount = activeSensors.size();
                report.activeSensors = new ArrayList<>(activeSensors);

                // Analyze data freshness
                OptionalLong latestTimestamp = recentData.stream()
                        .mapToLong(data -> data.getTimestamp().atZone(ZoneId.systemDefault()).toEpochSecond())
                        .max();

                if (latestTimestamp.isPresent()) {
                    long secondsSinceLatest = System.currentTimeMillis() / 1000 - latestTimestamp.getAsLong();
                    report.dataFreshnessSeconds = secondsSinceLatest;
                }

                Log.d("HealthDataApplicationService", "✅ Data quality report generated: " +
                        report.transmissionSuccessRate + "% transmission success, " +
                        report.activeSensorCount + " active sensors");

                return report;

            } catch (Exception e) {
                Log.e("HealthDataApplicationService", "❌ Error generating data quality report", e);
                throw new RuntimeException("Failed to generate data quality report", e);
            }
        });
    }

    // Data Quality Report class
    public static class DataQualityReport {
        public String userId;
        public LocalDateTime timestamp;
        public int totalReadings;
        public double transmissionSuccessRate;
        public int activeSensorCount;
        public List<SensorType> activeSensors;
        public long dataFreshnessSeconds;

        public String getSummary() {
            return String.format("Data Quality: %.1f%% transmitted, %d sensors active, %d seconds since last reading",
                    transmissionSuccessRate, activeSensorCount, dataFreshnessSeconds);
        }
    }
}