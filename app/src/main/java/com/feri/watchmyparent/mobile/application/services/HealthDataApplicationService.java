package com.feri.watchmyparent.mobile.application.services;

import android.util.Log;

import com.feri.watchmyparent.mobile.application.dto.SensorDataDTO;
import com.feri.watchmyparent.mobile.application.dto.SensorConfigurationDTO;
import com.feri.watchmyparent.mobile.domain.entities.SensorData;
import com.feri.watchmyparent.mobile.domain.entities.SensorConfiguration;
import com.feri.watchmyparent.mobile.domain.entities.User;
import com.feri.watchmyparent.mobile.domain.enums.SensorType;
import com.feri.watchmyparent.mobile.domain.repositories.SensorDataRepository;
import com.feri.watchmyparent.mobile.domain.repositories.SensorConfigurationRepository;
import com.feri.watchmyparent.mobile.domain.repositories.UserRepository;
import com.feri.watchmyparent.mobile.domain.valueobjects.SensorReading;
import com.feri.watchmyparent.mobile.infrastructure.services.PostgreSQLDataService;
import com.feri.watchmyparent.mobile.infrastructure.watch.WatchManager;
import com.feri.watchmyparent.mobile.infrastructure.kafka.HealthDataKafkaProducer;
import com.feri.watchmyparent.mobile.infrastructure.kafka.KafkaMessageFormatter;
import javax.inject.Inject;
import javax.inject.Singleton;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Singleton
public class HealthDataApplicationService {

    private static final String TAG = "HealthDataApplicationService";

    private final UserRepository userRepository;
    private final SensorDataRepository sensorDataRepository;
    private final SensorConfigurationRepository configurationRepository;
    private final HealthDataKafkaProducer kafkaProducer;
    private final PostgreSQLDataService postgreSQLDataService;
    private final WatchManager watchManager;

    @Inject
    public HealthDataApplicationService(
            UserRepository userRepository,
            SensorDataRepository sensorDataRepository,
            SensorConfigurationRepository configurationRepository,
            HealthDataKafkaProducer kafkaProducer,
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
    public CompletableFuture<List<SensorData>> collectSensorData(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "🔄 Starting REAL sensor data collection for user: " + userId);

                Optional<User> userOptional = userRepository.findById(userId).join();
                if (!userOptional.isPresent()) {
                    Log.w(TAG, "❌ User not found: " + userId);
                    return new ArrayList<>();
                }

                User user = userOptional.get();

                // Get sensor configurations for this user
                List<SensorConfiguration> configurations = configurationRepository.findByUserId(userId).join();
                if (configurations.isEmpty()) {
                    Log.w(TAG, "⚠️ No sensor configurations found for user: " + userId);
                    return new ArrayList<>();
                }

                // Get enabled sensor types
                List<SensorType> enabledSensorTypes = configurations.stream()
                        .filter(SensorConfiguration::isEnabled)
                        .map(SensorConfiguration::getSensorType)
                        .collect(Collectors.toList());

                if (enabledSensorTypes.isEmpty()) {
                    Log.w(TAG, "⚠️ No enabled sensors for user: " + userId);
                    return new ArrayList<>();
                }

                // ✅ REAL DATA COLLECTION from Samsung Health SDK
                List<SensorReading> realReadings = collectRealSensorData(enabledSensorTypes);

                // Convert to SensorData entities
                List<SensorData> collectedData = convertReadingsToSensorData(user, realReadings);

                // Save locally, send via Kafka, and store in PostgreSQL
                for (SensorData data : collectedData) {
                    // Save to local Room database
                    sensorDataRepository.save(data).join();

                    // Send via Kafka (real)
                    kafkaProducer.sendHealthData(data, userId);

                    // Save to PostgreSQL (real)
                    postgreSQLDataService.insertSensorData(data);
                }

                Log.d(TAG, "✅ Successfully collected " + collectedData.size() + " REAL sensor readings for user " + userId);
                return collectedData;

            } catch (Exception e) {
                Log.e(TAG, "❌ Error collecting REAL sensor data for user " + userId, e);
                return new ArrayList<>();
            }
        });
    }

    private List<SensorReading> collectRealSensorData(List<SensorType> sensorTypes) {
        try {
            if (!watchManager.isConnected()) {
                Log.w(TAG, "⚠️ Watch not connected, attempting to connect...");
                boolean connected = watchManager.connect().join();
                if (!connected) {
                    Log.e(TAG, "❌ Failed to connect to watch for real data collection");
                    return new ArrayList<>();
                }
            }

            // ✅ READ REAL DATA from Samsung Health SDK
            List<SensorReading> readings = watchManager.readSensorData(sensorTypes).join();

            Log.d(TAG, "📊 Collected " + readings.size() + " REAL sensor readings");
            for (SensorReading reading : readings) {
                Log.d(TAG, "📊 REAL: " + reading.getSensorType() + " = " + reading.getValue() + " " + reading.getUnit());
            }

            return readings;

        } catch (Exception e) {
            Log.e(TAG, "❌ Error collecting real sensor data from watch", e);
            return new ArrayList<>();
        }
    }

    private List<SensorData> convertReadingsToSensorData(User user, List<SensorReading> readings) {
        List<SensorData> sensorDataList = new ArrayList<>();

        for (SensorReading reading : readings) {
            SensorData sensorData = new SensorData();
            sensorData.setIdSensorData(java.util.UUID.randomUUID().toString());
            sensorData.setUser(user);
            sensorData.setSensorType(reading.getSensorType());
            sensorData.setValue(reading.getValue());
            sensorData.setTimestamp(reading.getTimestamp());
            sensorData.setUnit(reading.getUnit());
            sensorData.setDeviceId(watchManager.getDeviceId());

            sensorDataList.add(sensorData);
        }

        return sensorDataList;
    }

    // ✅ Overloaded method for specific sensor types
    public CompletableFuture<List<SensorDataDTO>> collectSensorData(String userId, List<SensorType> sensorTypes) {
        return collectSensorData(userId)
                .thenApply(sensorDataList -> sensorDataList.stream()
                        .filter(data -> sensorTypes.contains(data.getSensorType()))
                        .map(this::convertToDTO)
                        .collect(Collectors.toList()));
    }

    // ✅ Get latest sensor data as DTOs
    public CompletableFuture<List<SensorDataDTO>> getLatestSensorData(String userId) {
        return sensorDataRepository.findLatestByUserId(userId)
                .thenApply(sensorDataList -> sensorDataList.stream()
                        .map(this::convertToDTO)
                        .collect(Collectors.toList()));
    }

    // ✅ Get user sensor configurations as DTOs
    public CompletableFuture<List<SensorConfigurationDTO>> getUserSensorConfigurations(String userId) {
        return configurationRepository.findByUserId(userId)
                .thenApply(configurations -> configurations.stream()
                        .map(this::convertToConfigDTO)
                        .collect(Collectors.toList()));
    }

    // ✅ Update sensor configuration - with REAL watch configuration
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

                                // ✅ Configure REAL sensor frequency on watch
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

    // ✅ Retry failed transmissions - with real Kafka and PostgreSQL
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
        return kafkaProducer.sendHealthData(sensorData, sensorData.getUser().getIdUser())
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

    // ✅ DTO conversion methods
    private SensorDataDTO convertToDTO(SensorData sensorData) {
        SensorDataDTO dto = new SensorDataDTO(
                sensorData.getUser().getIdUser(),
                sensorData.getSensorType(),
                sensorData.getValue(),
                sensorData.getDeviceId()
        );
        dto.setTimestamp(sensorData.getTimestamp());
        dto.setTransmitted(sensorData.isTransmitted());
        dto.setUnit(sensorData.getUnit());
        return dto;
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
}