package com.feri.watchmyparent.mobile.infrastructure.watch;

import android.content.Context;

import com.feri.watchmyparent.mobile.domain.enums.SensorType;
import com.feri.watchmyparent.mobile.domain.valueobjects.SensorReading;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public abstract class WatchManager {

    protected String deviceId;
    protected boolean isConnected;
    protected Context context;

    // ✅ NEW: Constructor that accepts Context
    public WatchManager(Context context) {
        this.context = context;
        this.isConnected = false;
    }

    // ✅ DEFAULT: No-argument constructor for backward compatibility
    public WatchManager() {
        this.isConnected = false;
    }

    // EXISTING abstract methods
    public abstract CompletableFuture<Boolean> connect();
    public abstract CompletableFuture<Boolean> disconnect();
    public abstract CompletableFuture<List<SensorReading>> readSensorData(List<SensorType> sensorTypes);
    public abstract CompletableFuture<Boolean> configureSensorFrequency(SensorType sensorType, int frequencySeconds);
    public abstract CompletableFuture<Boolean> isDeviceAvailable();
    public abstract CompletableFuture<List<SensorType>> getSupportedSensors();

    // EXISTING getters
    public boolean isConnected() {
        return isConnected;
    }

    public String getDeviceId() {
        return deviceId;
    }


    // NEW: Context getter
    public Context getContext() {
        return context;
    }

    // NEW: Context setter for dependency injection
    public void setContext(Context context) {
        this.context = context;
    }

    // ✅ NEW: Enhanced device operations
    public String getConnectionStatus() {
        if (isConnected) {
            return "Connected to " + (deviceId != null ? deviceId : "unknown device");
        } else {
            return "Disconnected";
        }
    }

    public CompletableFuture<Boolean> ping() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Basic connectivity check
                return isDeviceAvailable().join();
            } catch (Exception e) {
                return false;
            }
        });
    }

    // ✅ NEW: Enhanced sensor reading with device metadata
    public CompletableFuture<List<SensorReading>> readSensorDataWithMetadata(List<SensorType> sensorTypes) {
        return readSensorData(sensorTypes)
                .thenApply(readings -> {
                    // Add device metadata to readings
                    for (SensorReading reading : readings) {
                        if (reading.getDeviceId() == null || reading.getDeviceId().isEmpty()) {
                            reading.setDeviceId(this.deviceId);
                        }
                        reading.setConnectionType(isConnected ? "CONNECTED" : "DISCONNECTED");
                    }
                    return readings;
                });
    }

    // ✅ NEW: Batch sensor configuration
    public CompletableFuture<Boolean> configureMultipleSensors(List<SensorType> sensorTypes, int frequencySeconds) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                boolean allConfigured = true;
                for (SensorType sensorType : sensorTypes) {
                    boolean configured = configureSensorFrequency(sensorType, frequencySeconds).join();
                    if (!configured) {
                        allConfigured = false;
                    }
                }
                return allConfigured;
            } catch (Exception e) {
                return false;
            }
        });
    }

    // ✅ NEW: Health check method
    public CompletableFuture<HealthStatus> performHealthCheck() {
        return CompletableFuture.supplyAsync(() -> {
            HealthStatus status = new HealthStatus();

            try {
                status.deviceAvailable = isDeviceAvailable().join();
                status.connected = isConnected();
                status.deviceId = getDeviceId();
                status.supportedSensorsCount = getSupportedSensors().join().size();
                status.timestamp = java.time.LocalDateTime.now();
                status.healthy = status.deviceAvailable && status.connected;

            } catch (Exception e) {
                status.healthy = false;
                status.errorMessage = e.getMessage();
            }

            return status;
        });
    }

    // ✅ NEW: Health status data class
    public static class HealthStatus {
        public boolean healthy = false;
        public boolean deviceAvailable = false;
        public boolean connected = false;
        public String deviceId;
        public int supportedSensorsCount = 0;
        public java.time.LocalDateTime timestamp;
        public String errorMessage;

        public String getSummary() {
            if (healthy) {
                return String.format("✅ Healthy - Device: %s, Sensors: %d", deviceId, supportedSensorsCount);
            } else {
                return "❌ Unhealthy" + (errorMessage != null ? " - " + errorMessage : "");
            }
        }
    }

    @Override
    public String toString() {
        return String.format("WatchManager{deviceId='%s', connected=%s, class=%s}",
                deviceId, isConnected, this.getClass().getSimpleName());
    }
}
