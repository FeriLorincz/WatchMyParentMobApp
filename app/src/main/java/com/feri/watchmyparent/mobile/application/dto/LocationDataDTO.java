package com.feri.watchmyparent.mobile.application.dto;

import java.time.LocalDateTime;

public class LocationDataDTO {

    private String userId;
    private String status; // "HOME" or "AWAY"
    private double latitude;
    private double longitude;
    private String address;
    private LocalDateTime timestamp;
    private boolean isAtHome;

    public LocationDataDTO() {}

    public LocationDataDTO(String userId, String status, double latitude, double longitude, String address) {
        this.userId = userId;
        this.status = status;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.timestamp = LocalDateTime.now();
        this.isAtHome = "HOME".equals(status);
    }

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getStatus() { return status; }
    public void setStatus(String status) {
        this.status = status;
        this.isAtHome = "HOME".equals(status);
    }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public boolean isAtHome() { return isAtHome; }
    public void setAtHome(boolean atHome) { isAtHome = atHome; }

    public String getFormattedCoordinates() {
        return String.format("%.6f, %.6f", latitude, longitude);
    }
}
