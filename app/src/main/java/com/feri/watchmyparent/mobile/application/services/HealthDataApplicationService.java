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

    @Inject
    public HealthDataApplicationService(
            UserRepository userRepository,
            SensorDataRepository sensorDataRepository,
            SensorConfigurationRepository configurationRepository,
            HealthDataKafkaProducer kafkaProducer) {
        this.userRepository = userRepository;
        this.sensorDataRepository = sensorDataRepository;
        this.configurationRepository = configurationRepository;
        this.kafkaProducer = kafkaProducer;
    }

    // ✅ Collect sensor data - orchestrates the entire flow
    public CompletableFuture<List<SensorData>> collectSensorData(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Optional<User> userOptional = userRepository.findById(userId).join();
                if (!userOptional.isPresent()) {
                    Log.w(TAG, "User not found: " + userId);
                    return new ArrayList<>();
                }

                User user = userOptional.get();

                // Get sensor configurations for this user
                List<SensorConfiguration> configurations = configurationRepository.findByUserId(userId).join();
                if (configurations.isEmpty()) {
                    Log.w(TAG, "No sensor configurations found for user: " + userId);
                    return new ArrayList<>();
                }

                // Simulate sensor data collection (in real implementation, this would connect to watch)
                List<SensorData> collectedData = simulateSensorDataCollection(user, configurations);

                // Save and transmit each piece of data
                for (SensorData data : collectedData) {
                    sensorDataRepository.save(data).join();
                    kafkaProducer.sendHealthData(data, userId);
                }

                Log.d(TAG, "Successfully collected " + collectedData.size() + " sensor readings for user " + userId);
                return collectedData;

            } catch (Exception e) {
                Log.e(TAG, "Error collecting sensor data for user " + userId, e);
                return new ArrayList<>();
            }
        });
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

    // ✅ Update sensor configuration
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

                                return configurationRepository.save(config)
                                        .thenApply(this::convertToConfigDTO);
                            });
                });
    }

    // ✅ Retry failed transmissions
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

    // ✅ Private helper methods
    private List<SensorData> simulateSensorDataCollection(User user, List<SensorConfiguration> configurations) {
        List<SensorData> data = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (SensorConfiguration config : configurations) {
            if (config.isEnabled()) {
                SensorData sensorData = new SensorData();
                sensorData.setIdSensorData(java.util.UUID.randomUUID().toString());
                sensorData.setUser(user);
                sensorData.setSensorType(config.getSensorType());
                sensorData.setTimestamp(now);
                sensorData.setUnit(config.getSensorType().getUnit());
                sensorData.setDeviceId("samsung_galaxy_watch_7");

                // Generate realistic mock values based on sensor type
                sensorData.setValue(generateMockValue(config.getSensorType()));
                data.add(sensorData);
            }
        }

        return data;
    }

    private double generateMockValue(SensorType sensorType) {
        switch (sensorType) {
            case HEART_RATE:
                return 60 + Math.random() * 40; // 60-100 bpm
            case BLOOD_OXYGEN:
                return 95 + Math.random() * 5; // 95-100%
            case BLOOD_PRESSURE:
                return 120 + Math.random() * 40; // 120-160 mmHg
            case BODY_TEMPERATURE:
                return 36.0 + Math.random() * 2; // 36-38°C
            case STEP_COUNT:
                return Math.random() * 1000; // 0-1000 steps
            case STRESS:
                return Math.random() * 100; // 0-100 stress score
            case SLEEP:
                return 6.0 + Math.random() * 4; // 6-10 hours
            case FALL_DETECTION:
                return Math.random() > 0.98 ? 1.0 : 0.0; // 2% chance of fall
            default:
                return Math.random() * 100;
        }
    }

    private CompletableFuture<Boolean> retryTransmission(SensorData sensorData) {
        return kafkaProducer.sendHealthData(sensorData, sensorData.getUser().getIdUser())
                .thenCompose(transmitted -> {
                    if (transmitted) {
                        sensorData.markAsTransmitted();
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