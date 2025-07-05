package com.feri.watchmyparent.mobile.domain.valueobjects;

import com.feri.watchmyparent.mobile.domain.enums.SensorType;

import java.time.LocalDateTime;
import java.util.Objects;

public class SensorReading {

    private SensorType sensorType;
    private double value;
    private LocalDateTime timestamp;
    private String unit;

    public SensorReading() {}

    public SensorReading(SensorType sensorType, double value) {
        this.sensorType = sensorType;
        this.value = value;
        this.timestamp = LocalDateTime.now();
        this.unit = sensorType.getUnit();
    }

    // Getters and Setters
    public SensorType getSensorType() { return sensorType; }
    public void setSensorType(SensorType sensorType) { this.sensorType = sensorType; }

    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SensorReading that = (SensorReading) o;
        return Double.compare(that.value, value) == 0 &&
                sensorType == that.sensorType &&
                Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sensorType, value, timestamp);
    }
}
