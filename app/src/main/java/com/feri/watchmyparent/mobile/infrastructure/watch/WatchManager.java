package com.feri.watchmyparent.mobile.infrastructure.watch;

import com.feri.watchmyparent.mobile.domain.enums.SensorType;
import com.feri.watchmyparent.mobile.domain.valueobjects.SensorReading;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public abstract class WatchManager {

    protected String deviceId;
    protected boolean isConnected;

    public abstract CompletableFuture<Boolean> connect();
    public abstract CompletableFuture<Boolean> disconnect();
    public abstract CompletableFuture<List<SensorReading>> readSensorData(List<SensorType> sensorTypes);
    public abstract CompletableFuture<Boolean> configureSensorFrequency(SensorType sensorType, int frequencySeconds);
    public abstract CompletableFuture<Boolean> isDeviceAvailable();
    public abstract CompletableFuture<List<SensorType>> getSupportedSensors();

    public boolean isConnected() {
        return isConnected;
    }

    public String getDeviceId() {
        return deviceId;
    }
}
