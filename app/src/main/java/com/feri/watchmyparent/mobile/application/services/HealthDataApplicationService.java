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

    public CompletableFuture<List<SensorData>> collectSensorData(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Găsim utilizatorul folosind Optional pentru a evita NoSuchElementException
                Optional<User> userOptional = userRepository.findById(userId).join();

                if (!userOptional.isPresent()) {
                    Log.w(TAG, "User not found: " + userId);
                    return new ArrayList<>(); // Returnăm o listă goală în loc să aruncăm excepție
                }

                User user = userOptional.get();

                // Obținem configurațiile senzorilor
                List<SensorConfiguration> configurations =
                        configurationRepository.findByUserId(userId).join();

                if (configurations.isEmpty()) {
                    Log.w(TAG, "No sensor configurations found for user: " + userId);
                    return new ArrayList<>();
                }

                // Aici ar trebui să colectăm date reale de la senzori
                // Pentru moment, simulăm câteva date
                List<SensorData> collectedData = simulateSensorData(user, configurations);

                // Salvăm datele în repository
                for (SensorData data : collectedData) {
                    sensorDataRepository.save(data).join();

                    // Trimitem date la Kafka
                    kafkaProducer.sendHealthData(data, userId);
                }

                return collectedData;
            } catch (Exception e) {
                Log.e(TAG, "Error collecting sensor data for user " + userId, e);
                return new ArrayList<>(); // Returnăm o listă goală în caz de eroare
            }
        });
    }

    private List<SensorData> simulateSensorData(User user, List<SensorConfiguration> configurations) {
        List<SensorData> data = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (SensorConfiguration config : configurations) {
            SensorData sensorData = new SensorData();
            sensorData.setIdSensorData(java.util.UUID.randomUUID().toString());
            sensorData.setUser(user);
            sensorData.setSensorType(config.getSensorType());
            sensorData.setTimestamp(now);
            sensorData.setValue(Math.random() * 100); // Valoare aleatoare între 0-100
            sensorData.setUnit(config.getUnitOfMeasure());

            data.add(sensorData);
        }

        return data;
    }
}






    /*

    private final WatchManager watchManager;
    private final SensorDataRepository sensorDataRepository;
    private final SensorConfigurationRepository configurationRepository;
    private final UserRepository userRepository;
    private final HealthDataKafkaProducer kafkaProducer;
    private final KafkaMessageFormatter messageFormatter;

    @Inject
    public HealthDataApplicationService(
            WatchManager watchManager,
            SensorDataRepository sensorDataRepository,
            SensorConfigurationRepository configurationRepository,
            UserRepository userRepository,
            HealthDataKafkaProducer kafkaProducer,
            KafkaMessageFormatter messageFormatter) {
        this.watchManager = watchManager;
        this.sensorDataRepository = sensorDataRepository;
        this.configurationRepository = configurationRepository;
        this.userRepository = userRepository;
        this.kafkaProducer = kafkaProducer;
        this.messageFormatter = messageFormatter;
    }

    public CompletableFuture<List<SensorDataDTO>> collectSensorData(String userId, List<SensorType> sensorTypes) {
        return userRepository.findById(userId)
                .thenCompose(userOpt -> {
                    if (userOpt.isPresent()) {
                        throw new RuntimeException("User not found: " + userId);
                    }
                    User user = userOpt.get();

                    return watchManager.readSensorData(sensorTypes)
                            .thenCompose(readings -> processSensorReadings(user, readings));
                })
                .exceptionally(throwable -> {
                    Log.e("HealthDataApplicationService", "Error collecting sensor data for user " + userId + throwable.getMessage());
                    return new ArrayList<>();
                });
    }

    private CompletableFuture<List<SensorDataDTO>> processSensorReadings(User user, List<SensorReading> readings) {
        List<CompletableFuture<SensorDataDTO>> futures = readings.stream()
                .map(reading -> processSingleReading(user, reading))
                .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(ignored -> futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()));
    }

    private CompletableFuture<SensorDataDTO> processSingleReading(User user, SensorReading reading) {
        // Create sensor data entity
        SensorData sensorData = new SensorData(
                user,
                reading.getSensorType(),
                reading.getValue(),
                watchManager.getDeviceId()
        );

        // Save to local database
        return sensorDataRepository.save(sensorData)
                .thenCompose(saved -> {
                    // Transmit to Kafka
                    String formattedMessage = messageFormatter.formatSensorData(saved);
                    return kafkaProducer.sendHealthData(formattedMessage, user.getIdUser())
                            .thenApply(transmitted -> {
                                if (transmitted) {
                                    saved.markAsTransmitted();
                                    sensorDataRepository.save(saved); // Update transmission status
                                }
                                return convertToDTO(saved);
                            });
                });
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
                    if (userOpt.isPresent()) {
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
        String formattedMessage = messageFormatter.formatSensorData(sensorData);
        return kafkaProducer.sendHealthData(formattedMessage, sensorData.getUser().getIdUser())
                .thenCompose(transmitted -> {
                    if (transmitted) {
                        sensorData.markAsTransmitted();
                        return sensorDataRepository.save(sensorData)
                                .thenApply(saved -> true);
                    }
                    return CompletableFuture.completedFuture(false);
                });
    }

    private SensorDataDTO convertToDTO(SensorData sensorData) {
        SensorDataDTO dto = new SensorDataDTO(
                sensorData.getUser().getIdUser(),
                sensorData.getSensorType(),
                sensorData.getValue(),
                sensorData.getDeviceId()
        );
        dto.setTimestamp(sensorData.getTimestamp());
        dto.setTransmitted(sensorData.isTransmitted());
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

     */