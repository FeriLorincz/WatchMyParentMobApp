package com.feri.watchmyparent.mobile.domain.entities;

import com.feri.watchmyparent.mobile.domain.enums.SensorType;
import com.feri.watchmyparent.mobile.domain.enums.TransmissionStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public class SensorData {

    private String idSensorData;
    private User user;
    private SensorType sensorType;
    private double value;
    private String unit;
    private LocalDateTime timestamp;
    private TransmissionStatus transmissionStatus;
    private LocalDateTime transmissionTime;
    private String deviceId;
    private String metadata;

    public SensorData() {
        this.idSensorData = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
        this.transmissionStatus = TransmissionStatus.PENDING;
    }

    public SensorData(User user, SensorType sensorType, double value, String deviceId) {
        this();
        this.user = user;
        this.sensorType = sensorType;
        this.value = value;
        this.unit = sensorType.getUnit();
        this.deviceId = deviceId;
    }

    // Business Logic Methods
    public void markAsTransmitted() {
        this.transmissionStatus = TransmissionStatus.TRANSMITTED;
        this.transmissionTime = LocalDateTime.now();
    }

    public void markAsFailed() {
        this.transmissionStatus = TransmissionStatus.FAILED;
    }

    public void markAsQueued() {
        this.transmissionStatus = TransmissionStatus.QUEUED;
    }

    public boolean isTransmitted() {
        return transmissionStatus == TransmissionStatus.TRANSMITTED;
    }

    public boolean requiresTransmission() {
        return transmissionStatus == TransmissionStatus.PENDING ||
                transmissionStatus == TransmissionStatus.FAILED;
    }

    // Getters and Setters
    public String getIdSensorData() { return idSensorData; }
    public void setIdSensorData(String idSensorData) { this.idSensorData = idSensorData; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public SensorType getSensorType() { return sensorType; }
    public void setSensorType(SensorType sensorType) { this.sensorType = sensorType; }

    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public TransmissionStatus getTransmissionStatus() { return transmissionStatus; }
    public void setTransmissionStatus(TransmissionStatus transmissionStatus) { this.transmissionStatus = transmissionStatus; }

    public LocalDateTime getTransmissionTime() { return transmissionTime; }
    public void setTransmissionTime(LocalDateTime transmissionTime) { this.transmissionTime = transmissionTime; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
}
