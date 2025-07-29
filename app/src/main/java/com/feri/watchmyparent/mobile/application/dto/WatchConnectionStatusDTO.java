package com.feri.watchmyparent.mobile.application.dto;

import com.feri.watchmyparent.mobile.domain.enums.SensorType;

import java.time.LocalDateTime;
import java.util.List;

public class WatchConnectionStatusDTO {
    private boolean isConnected;
    private String deviceId;
    private String deviceName;
    private LocalDateTime lastConnectionTime;
    private List<SensorType> supportedSensors;
    private String connectionError;
    private boolean partiallyConnected; // Adăugat nou

    public WatchConnectionStatusDTO() {}

    public WatchConnectionStatusDTO(boolean isConnected, String deviceId, String deviceName) {
        this.isConnected = isConnected;
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.lastConnectionTime = isConnected ? LocalDateTime.now() : null;
        this.partiallyConnected = false; // Default: nu este parțial conectat
    }

    // Constructor nou cu partiallyConnected
    public WatchConnectionStatusDTO(boolean isConnected, String deviceId, String deviceName, boolean partiallyConnected) {
        this.isConnected = isConnected;
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.lastConnectionTime = isConnected ? LocalDateTime.now() : null;
        this.partiallyConnected = partiallyConnected;
    }

    // Getters and Setters
    public boolean isConnected() { return isConnected; }
    public void setConnected(boolean connected) {
        isConnected = connected;
        if (connected) {
            lastConnectionTime = LocalDateTime.now();
            connectionError = null;
        }
    }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

    public LocalDateTime getLastConnectionTime() { return lastConnectionTime; }
    public void setLastConnectionTime(LocalDateTime lastConnectionTime) { this.lastConnectionTime = lastConnectionTime; }

    public List<SensorType> getSupportedSensors() { return supportedSensors; }
    public void setSupportedSensors(List<SensorType> supportedSensors) { this.supportedSensors = supportedSensors; }

    public String getConnectionError() { return connectionError; }
    public void setConnectionError(String connectionError) { this.connectionError = connectionError; }

    // Metodă nouă pentru partiallyConnected
    public boolean isPartiallyConnected() {
        return partiallyConnected;
    }

    // Setter nou pentru partiallyConnected
    public void setPartiallyConnected(boolean partiallyConnected) {
        this.partiallyConnected = partiallyConnected;
    }

    public String getStatusText() {
        if (isConnected) {
            return partiallyConnected ? "Partially Connected" : "Connected";
        } else {
            return "Disconnected";
        }
    }
}