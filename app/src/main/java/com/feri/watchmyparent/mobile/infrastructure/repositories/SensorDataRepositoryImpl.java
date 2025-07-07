package com.feri.watchmyparent.mobile.infrastructure.repositories;

import android.util.Log;

import com.feri.watchmyparent.mobile.domain.entities.SensorData;
import com.feri.watchmyparent.mobile.domain.entities.User;
import com.feri.watchmyparent.mobile.domain.repositories.SensorDataRepository;
import com.feri.watchmyparent.mobile.domain.repositories.UserRepository;
import com.feri.watchmyparent.mobile.domain.enums.SensorType;
import com.feri.watchmyparent.mobile.domain.enums.TransmissionStatus;
import com.feri.watchmyparent.mobile.infrastructure.database.dao.SensorDataDao;
import com.feri.watchmyparent.mobile.infrastructure.database.entities.SensorDataEntity;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Singleton
public class SensorDataRepositoryImpl implements SensorDataRepository{

    private final SensorDataDao sensorDataDao;
    private final Executor executor = Executors.newFixedThreadPool(4);

    @Inject
    public SensorDataRepositoryImpl(SensorDataDao sensorDataDao) {
        this.sensorDataDao = sensorDataDao;
    }

    @Override
    public CompletableFuture<SensorData> save(SensorData sensorData) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                SensorDataEntity entity = convertToEntity(sensorData);
                sensorDataDao.insertSensorData(entity);
                Log.d("SensorDataRepositoryImpl", "Sensor data saved: " + sensorData.getSensorType() + " for user " + sensorData.getUser().getIdUser());
                return sensorData;
            } catch (Exception e) {
                Log.e("SensorDataRepositoryImpl", "Error saving sensor data", e);
                throw new RuntimeException("Failed to save sensor data", e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<List<SensorData>> findByUserIdAndSensorType(String userId, SensorType sensorType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<SensorDataEntity> entities = sensorDataDao.getSensorDataByUserAndType(userId, sensorType);
                return entities.stream()
                        .map(this::convertToDomain)
                        .collect(Collectors.toList());
            } catch (Exception e) {
                Log.e("SensorDataRepositoryImpl", "Error finding sensor data by user and type", e);
                throw new RuntimeException("Failed to find sensor data", e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<List<SensorData>> findByTransmissionStatus(TransmissionStatus status) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<SensorDataEntity> entities = sensorDataDao.getSensorDataByTransmissionStatus(status);
                return entities.stream()
                        .map(this::convertToDomain)
                        .collect(Collectors.toList());
            } catch (Exception e) {
                Log.e("SensorDataRepositoryImpl", "Error finding sensor data by transmission status", e);
                throw new RuntimeException("Failed to find sensor data", e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<List<SensorData>> findLatestByUserId(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<SensorDataEntity> entities = sensorDataDao.getLatestSensorDataByUser(userId);
                return entities.stream()
                        .map(this::convertToDomain)
                        .collect(Collectors.toList());
            } catch (Exception e) {
                Log.e("SensorDataRepositoryImpl", "Error finding latest sensor data", e);
                throw new RuntimeException("Failed to find latest sensor data", e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> delete(String id) {
        return CompletableFuture.runAsync(() -> {
            try {
                sensorDataDao.deleteSensorDataById(id);
                Log.d("SensorDataRepositoryImpl", "Sensor data deleted: " + id);
            } catch (Exception e) {
                Log.e("SensorDataRepositoryImpl", "Error deleting sensor data", e);
                throw new RuntimeException("Failed to delete sensor data", e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<List<SensorData>> findPendingTransmissions() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<SensorDataEntity> entities = sensorDataDao.getPendingTransmissions(
                        TransmissionStatus.PENDING, TransmissionStatus.FAILED);
                return entities.stream()
                        .map(this::convertToDomain)
                        .collect(Collectors.toList());
            } catch (Exception e) {
                Log.e("SensorDataRepositoryImpl", "Error finding pending transmissions", e);
                throw new RuntimeException("Failed to find pending transmissions", e);
            }
        }, executor);
    }

    private SensorDataEntity convertToEntity(SensorData sensorData) {
        SensorDataEntity entity = new SensorDataEntity();
        entity.idSensorData = sensorData.getIdSensorData();
        entity.userId = sensorData.getUser().getIdUser();
        entity.sensorType = sensorData.getSensorType();
        entity.value = sensorData.getValue();
        entity.unit = sensorData.getUnit();
        entity.timestamp = sensorData.getTimestamp();
        entity.transmissionStatus = sensorData.getTransmissionStatus();
        entity.transmissionTime = sensorData.getTransmissionTime();
        entity.deviceId = sensorData.getDeviceId();
        entity.metadata = sensorData.getMetadata();
        return entity;
    }

    private SensorData convertToDomain(SensorDataEntity entity) {
        // Create basic user object
        User user = new User();
        user.setIdUser(entity.userId);

        SensorData sensorData = new SensorData();
        sensorData.setIdSensorData(entity.idSensorData);
        sensorData.setUser(user);
        sensorData.setSensorType(entity.sensorType);
        sensorData.setValue(entity.value);
        sensorData.setUnit(entity.unit);
        sensorData.setTimestamp(entity.timestamp);
        sensorData.setTransmissionStatus(entity.transmissionStatus);
        sensorData.setTransmissionTime(entity.transmissionTime);
        sensorData.setDeviceId(entity.deviceId);
        sensorData.setMetadata(entity.metadata);

        return sensorData;
    }
}