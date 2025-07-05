package com.feri.watchmyparent.mobile.domain.entities;

import com.feri.watchmyparent.mobile.domain.valueobjects.LocationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public class LocationData {

    private String idLocationData;
    private User user;
    private LocationStatus locationStatus;
    private double homeLatitude;
    private double homeLongitude;
    private double radiusMeters;
    private String deviceId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public LocationData() {
        this.idLocationData = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.radiusMeters = 50.0; // Default 50m radius
    }

    public LocationData(User user, double homeLatitude, double homeLongitude) {
        this();
        this.user = user;
        this.homeLatitude = homeLatitude;
        this.homeLongitude = homeLongitude;
    }

    // Business Logic Methods
    public void updateLocation(double currentLatitude, double currentLongitude, String address) {
        double distance = calculateDistance(homeLatitude, homeLongitude, currentLatitude, currentLongitude);

        if (distance <= radiusMeters) {
            this.locationStatus = new LocationStatus("HOME", homeLatitude, homeLongitude,
                    user.getAddressUser() != null ? getFormattedHomeAddress() : address);
        } else {
            this.locationStatus = new LocationStatus("AWAY", currentLatitude, currentLongitude, address);
        }

        this.updatedAt = LocalDateTime.now();
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the earth in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c * 1000; // convert to meters
    }

    private String getFormattedHomeAddress() {
        if (user.getAddressUser() == null) return "";

        StringBuilder address = new StringBuilder();
        if (user.getAddressUser().getStreetUser() != null) {
            address.append(user.getAddressUser().getStreetUser()).append(" ");
        }
        if (user.getAddressUser().getNumberUser() != null) {
            address.append(user.getAddressUser().getNumberUser()).append(", ");
        }
        if (user.getAddressUser().getCityUser() != null) {
            address.append(user.getAddressUser().getCityUser());
        }
        return address.toString();
    }

    public void updateHomeLocation(double latitude, double longitude) {
        this.homeLatitude = latitude;
        this.homeLongitude = longitude;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateRadius(double radiusMeters) {
        this.radiusMeters = radiusMeters;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isAtHome() {
        return locationStatus != null && locationStatus.isHome();
    }

    // Getters and Setters

    public String getIdLocationData() {
        return idLocationData;
    }

    public void setIdLocationData(String idLocationData) {
        this.idLocationData = idLocationData;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocationStatus getLocationStatus() {
        return locationStatus;
    }

    public void setLocationStatus(LocationStatus locationStatus) {
        this.locationStatus = locationStatus;
    }

    public double getHomeLatitude() {
        return homeLatitude;
    }

    public void setHomeLatitude(double homeLatitude) {
        this.homeLatitude = homeLatitude;
    }

    public double getHomeLongitude() {
        return homeLongitude;
    }

    public void setHomeLongitude(double homeLongitude) {
        this.homeLongitude = homeLongitude;
    }

    public double getRadiusMeters() {
        return radiusMeters;
    }

    public void setRadiusMeters(double radiusMeters) {
        this.radiusMeters = radiusMeters;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
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
}
