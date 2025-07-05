package com.feri.watchmyparent.mobile.application.services;

import com.feri.watchmyparent.mobile.application.dto.WatchConnectionStatusDTO;
import com.feri.watchmyparent.mobile.application.dto.SensorConfigurationDTO;
import com.feri.watchmyparent.mobile.domain.enums.SensorType;
import com.feri.watchmyparent.mobile.infrastructure.watch.WatchManager;
import com.feri.watchmyparent.mobile.infrastructure.watch.WatchCapabilityRegistry;
import timber.log.Timber;
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
        Timber.d("Attempting to connect to Samsung Watch");

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
                    Timber.e(throwable, "Error connecting to Samsung Watch");
                    currentStatus.setConnected(false);
                    currentStatus.setConnectionError(throwable.getMessage());
                    return currentStatus;
                });
    }

    public CompletableFuture<WatchConnectionStatusDTO> disconnectWatch() {
        Timber.d("Disconnecting from Samsung Watch");

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
                    Timber.d("Loaded %d supported sensors", sensors.size());
                    return currentStatus;
                });
    }

    public CompletableFuture<Boolean> configureSensorFrequency(SensorConfigurationDTO config) {
        if (!currentStatus.isConnected()) {
            Timber.w("Cannot configure sensor: Watch not connected");
            return CompletableFuture.completedFuture(false);
        }

        return watchManager.configureSensorFrequency(config.getSensorType(), config.getFrequencySeconds())
                .thenApply(success -> {
                    if (success) {
                        Timber.d("Successfully configured %s frequency to %d seconds",
                                config.getSensorType(), config.getFrequencySeconds());
                    } else {
                        Timber.w("Failed to configure %s frequency", config.getSensorType());
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
