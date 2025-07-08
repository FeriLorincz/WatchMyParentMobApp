package com.feri.watchmyparent.mobile.domain.repositories;

import com.feri.watchmyparent.mobile.domain.entities.SensorData;
import com.feri.watchmyparent.mobile.domain.enums.SensorType;
import com.feri.watchmyparent.mobile.domain.enums.TransmissionStatus;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface SensorDataRepository {
    CompletableFuture<SensorData> save(SensorData sensorData);
    CompletableFuture<List<SensorData>> findByUserIdAndSensorType(String userId, SensorType sensorType);
    CompletableFuture<List<SensorData>> findByTransmissionStatus(TransmissionStatus status);
    CompletableFuture<List<SensorData>> findLatestByUserId(String userId);
    CompletableFuture<Void> delete(String id);
    CompletableFuture<List<SensorData>> findPendingTransmissions();
    CompletableFuture<List<SensorData>> findByUserId(String userId, int limit);
}
