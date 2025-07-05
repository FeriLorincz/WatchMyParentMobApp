package com.feri.watchmyparent.mobile.domain.valueobjects;

import java.time.LocalDateTime;
import java.util.Objects;

public class LocationStatus {

    private String status; // "HOME" or "AWAY"
    private double latitude;
    private double longitude;
    private String address;
    private LocalDateTime timestamp;

    public LocationStatus() {}

    public LocationStatus(String status, double latitude, double longitude, String address) {
        this.status = status;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public boolean isHome() {
        return "HOME".equals(status);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LocationStatus that = (LocationStatus) o;
        return Double.compare(that.latitude, latitude) == 0 &&
                Double.compare(that.longitude, longitude) == 0 &&
                Objects.equals(status, that.status) &&
                Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, latitude, longitude, timestamp);
    }
}
