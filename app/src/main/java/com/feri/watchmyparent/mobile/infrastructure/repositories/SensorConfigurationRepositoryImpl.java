package com.feri.watchmyparent.mobile.infrastructure.repositories;

import android.util.Log;

import com.feri.watchmyparent.mobile.domain.entities.SensorConfiguration;
import com.feri.watchmyparent.mobile.domain.entities.User;
import com.feri.watchmyparent.mobile.domain.repositories.SensorConfigurationRepository;
import com.feri.watchmyparent.mobile.domain.enums.SensorType;
import com.feri.watchmyparent.mobile.infrastructure.database.dao.SensorConfigurationDao;
import com.feri.watchmyparent.mobile.infrastructure.database.entities.SensorConfigurationEntity;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Singleton
public class SensorConfigurationRepositoryImpl implements SensorConfigurationRepository {

    private final SensorConfigurationDao configurationDao;
    private final Executor executor = Executors.newFixedThreadPool(4);

    @Inject
    public SensorConfigurationRepositoryImpl(SensorConfigurationDao configurationDao) {
        this.configurationDao = configurationDao;
    }

    @Override
    public CompletableFuture<SensorConfiguration> save(SensorConfiguration configuration) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                SensorConfigurationEntity entity = convertToEntity(configuration);
                configurationDao.insertSensorConfiguration(entity);
                Log.d("SensorConfigurationRepository", "Sensor configuration saved: " + configuration.getSensorType() + " for user " + configuration.getUser().getIdUser());
                return configuration;
            } catch (Exception e) {
                Log.e("SensorConfigurationRepository", "Error saving sensor configuration", e);
                throw new RuntimeException("Failed to save sensor configuration", e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Optional<SensorConfiguration>> findByUserIdAndSensorType(String userId, SensorType sensorType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                SensorConfigurationEntity entity = configurationDao.getSensorConfigurationByUserAndType(userId, sensorType);
                return entity != null ? Optional.of(convertToDomain(entity)) : Optional.empty();
            } catch (Exception e) {
                Log.e("SensorConfigurationRepository", "Error finding sensor configuration", e);
                return Optional.empty();
            }
        }, executor);
    }

    @Override
    public CompletableFuture<List<SensorConfiguration>> findByUserId(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<SensorConfigurationEntity> entities = configurationDao.getSensorConfigurationsByUser(userId);
                return entities.stream()
                        .map(this::convertToDomain)
                        .collect(Collectors.toList());
            } catch (Exception e) {
                Log.e("SensorConfigurationRepository", "Error finding sensor configurations", e);
                throw new RuntimeException("Failed to find sensor configurations", e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> delete(String id) {
        return CompletableFuture.runAsync(() -> {
            try {
                configurationDao.deleteSensorConfigurationById(id);
                Log.d("SensorConfigurationRepository", "Sensor configuration deleted: " + id);
            } catch (Exception e) {
                Log.e("SensorConfigurationRepository", "Error deleting sensor configuration", e);
                throw new RuntimeException("Failed to delete sensor configuration", e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<List<SensorConfiguration>> findEnabledByUserId(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<SensorConfigurationEntity> entities = configurationDao.getEnabledSensorConfigurationsByUser(userId);
                return entities.stream()
                        .map(this::convertToDomain)
                        .collect(Collectors.toList());
            } catch (Exception e) {
                Log.e("SensorConfigurationRepository", "Error finding enabled sensor configurations", e);
                throw new RuntimeException("Failed to find enabled sensor configurations", e);
            }
        }, executor);
    }

    private SensorConfigurationEntity convertToEntity(SensorConfiguration config) {
        SensorConfigurationEntity entity = new SensorConfigurationEntity();
        entity.idSensorConfiguration = config.getIdSensorConfiguration();
        entity.userId = config.getUser().getIdUser();
        entity.sensorType = config.getSensorType();
        entity.frequencySeconds = config.getFrequencySeconds();
        entity.isEnabled = config.isEnabled();
        entity.createdAt = config.getCreatedAt();
        entity.updatedAt = config.getUpdatedAt();
        return entity;
    }

    private SensorConfiguration convertToDomain(SensorConfigurationEntity entity) {
        // Create basic user object
        User user = new User();
        user.setIdUser(entity.userId);

        SensorConfiguration config = new SensorConfiguration();
        config.setIdSensorConfiguration(entity.idSensorConfiguration);
        config.setUser(user);
        config.setSensorType(entity.sensorType);
        config.setFrequencySeconds(entity.frequencySeconds);
        config.setEnabled(entity.isEnabled);
        config.setCreatedAt(entity.createdAt);
        config.setUpdatedAt(entity.updatedAt);

        return config;
    }
}
