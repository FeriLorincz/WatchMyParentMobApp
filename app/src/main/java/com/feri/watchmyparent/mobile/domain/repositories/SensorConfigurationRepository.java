package com.feri.watchmyparent.mobile.domain.repositories;

import com.feri.watchmyparent.mobile.domain.entities.SensorConfiguration;
import com.feri.watchmyparent.mobile.domain.enums.SensorType;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface SensorConfigurationRepository {
    CompletableFuture<SensorConfiguration> save(SensorConfiguration configuration);
    CompletableFuture<Optional<SensorConfiguration>> findByUserIdAndSensorType(String userId, SensorType sensorType);
    CompletableFuture<List<SensorConfiguration>> findByUserId(String userId);
    CompletableFuture<Void> delete(String id);
    CompletableFuture<List<SensorConfiguration>> findEnabledByUserId(String userId);
}
