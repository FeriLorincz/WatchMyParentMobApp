package com.feri.watchmyparent.mobile.domain.entities;

import com.feri.watchmyparent.mobile.domain.enums.SensorType;

import java.time.LocalDateTime;
import java.util.UUID;

public class SensorConfiguration {

    private String idSensorConfiguration;
    private User user;
    private SensorType sensorType;
    private int frequencySeconds;
    private boolean isEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public SensorConfiguration() {
        this.idSensorConfiguration = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.isEnabled = true;
    }

    public SensorConfiguration(User user, SensorType sensorType, int frequencySeconds) {
        this();
        this.user = user;
        this.sensorType = sensorType;
        this.frequencySeconds = frequencySeconds;
    }

    // Business Logic Methods
    public void updateFrequency(int newFrequencySeconds) {
        if (newFrequencySeconds < 30 || newFrequencySeconds > 1800) {
            throw new IllegalArgumentException("Frequency must be between 30 seconds and 30 minutes");
        }
        this.frequencySeconds = newFrequencySeconds;
        this.updatedAt = LocalDateTime.now();
    }

    public void toggleEnabled() {
        this.isEnabled = !this.isEnabled;
        this.updatedAt = LocalDateTime.now();
    }

    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getIdSensorConfiguration() {
        return idSensorConfiguration;
    }

    public void setIdSensorConfiguration(String idSensorConfiguration) {
        this.idSensorConfiguration = idSensorConfiguration;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public SensorType getSensorType() {
        return sensorType;
    }

    public void setSensorType(SensorType sensorType) {
        this.sensorType = sensorType;
    }

    public int getFrequencySeconds() {
        return frequencySeconds;
    }

    public void setFrequencySeconds(int frequencySeconds) {
        this.frequencySeconds = frequencySeconds;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }


    // Get the unit of measure for this sensor configuration
    // @return the unit of measure as String
    public String getUnitOfMeasure() {
        return this.sensorType != null ? this.sensorType.getUnit() : "";
    }
}
