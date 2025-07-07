package com.feri.watchmyparent.mobile.application.services;

import android.util.Log;

import com.feri.watchmyparent.mobile.application.dto.WatchConnectionStatusDTO;
import com.feri.watchmyparent.mobile.application.dto.SensorConfigurationDTO;
import com.feri.watchmyparent.mobile.domain.enums.SensorType;
import com.feri.watchmyparent.mobile.infrastructure.watch.WatchManager;
import com.feri.watchmyparent.mobile.infrastructure.watch.WatchCapabilityRegistry;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Singleton
public class WatchConnectionApplicationService {

    private final WatchManager watchManager;
    private WatchConnectionStatusDTO currentStatus;

    @Inject
    public WatchConnectionApplicationService(WatchManager watchManager) {
        this.watchManager = watchManager;
        this.currentStatus = new WatchConnectionStatusDTO(false, null, "Samsung Galaxy Watch 7");
    }

    public CompletableFuture<WatchConnectionStatusDTO> connectWatch() {
        Log.d("WatchConnectionApplicationService", "Attempting to connect to Samsung Watch");

        return watchManager.connect()
                .thenCompose(connected -> {
                    if (connected) {
                        currentStatus.setConnected(true);
                        currentStatus.setDeviceId(watchManager.getDeviceId());
                        return loadSupportedSensors();
                    } else {
                        currentStatus.setConnected(false);
                        currentStatus.setConnectionError("Failed to connect to Samsung Watch");
                        return CompletableFuture.completedFuture(currentStatus);
                    }
                })
                .exceptionally(throwable -> {
                    Log.e("WatchConnectionApplicationService", "Error connecting to Samsung Watch", throwable);
                    currentStatus.setConnected(false);
                    currentStatus.setConnectionError(throwable.getMessage());
                    return currentStatus;
                });
    }

    public CompletableFuture<WatchConnectionStatusDTO> disconnectWatch() {
        Log.d("WatchConnectionApplicationService", "Disconnecting from Samsung Watch");

        return watchManager.disconnect()
                .thenApply(success -> {
                    currentStatus.setConnected(false);
                    if (!success) {
                        currentStatus.setConnectionError("Error during disconnection");
                    }
                    return currentStatus;
                });
    }

    private CompletableFuture<WatchConnectionStatusDTO> loadSupportedSensors() {
        return watchManager.getSupportedSensors()
                .thenApply(sensors -> {
                    currentStatus.setSupportedSensors(sensors);
                    Log.d("WatchConnectionApplicationService", "Loaded " + sensors.size() + " supported sensors");
                    return currentStatus;
                });
    }

    public CompletableFuture<Boolean> configureSensorFrequency(SensorConfigurationDTO config) {
        if (!currentStatus.isConnected()) {
            Log.w("WatchConnectionApplicationService", "Cannot configure sensor: Watch not connected");
            return CompletableFuture.completedFuture(false);
        }

        return watchManager.configureSensorFrequency(config.getSensorType(), config.getFrequencySeconds())
                .thenApply(success -> {
                    if (success) {
                        Log.d("WatchConnectionApplicationService", "Successfully configured " + config.getSensorType() + " frequency to " + config.getFrequencySeconds() + " seconds");
                    } else {
                        Log.w("WatchConnectionApplicationService", "Failed to configure " + config.getSensorType() + " frequency");
                    }
                    return success;
                });
    }

    public CompletableFuture<Boolean> isWatchAvailable() {
        return watchManager.isDeviceAvailable();
    }

    public WatchConnectionStatusDTO getCurrentStatus() {
        return currentStatus;
    }

    public boolean isConnected() {
        return currentStatus.isConnected();
    }

    public List<SensorType> getSupportedSensors() {
        return currentStatus.getSupportedSensors();
    }

    public boolean isSensorSupported(SensorType sensorType) {
        if (currentStatus.getSupportedSensors() == null) {
            return WatchCapabilityRegistry.isSensorSupported(watchManager.getDeviceId(), sensorType);
        }
        return currentStatus.getSupportedSensors().contains(sensorType);
    }
}
