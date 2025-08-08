package com.feri.watchmyparent.mobile.application.dto;

import java.time.LocalDateTime;

public class LocationDataDTO {

    private String userId;
    private String status; // "HOME", "AWAY", "ACTIVE", "UNKNOWN"
    private double latitude;
    private double longitude;
    private String address;
    private LocalDateTime timestamp;
    private boolean isAtHome;

    // ✅ ADĂUGAT: Câmpurile lipsă pentru compatibilitate completă
    private double homeLatitude;
    private double homeLongitude;
    private double radiusMeters = 50.0; // Default 50 meters

    // Constructors
    public LocationDataDTO() {
        this.timestamp = LocalDateTime.now();
    }

    public LocationDataDTO(String userId, String status, double latitude, double longitude, String address) {
        this.userId = userId;
        this.status = status;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.timestamp = LocalDateTime.now();
        this.isAtHome = "HOME".equals(status);
    }

    // ✅ Constructor complet cu toate câmpurile
    public LocationDataDTO(String userId, String status, double latitude, double longitude,
                           String address, double homeLatitude, double homeLongitude, double radiusMeters) {
        this(userId, status, latitude, longitude, address);
        this.homeLatitude = homeLatitude;
        this.homeLongitude = homeLongitude;
        this.radiusMeters = radiusMeters;

        // Calculează dacă e acasă pe baza distanței
        this.isAtHome = calculateIsAtHome();
    }

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getStatus() { return status; }
    public void setStatus(String status) {
        this.status = status;
        // Recalculează isAtHome când se schimbă statusul
        if ("HOME".equals(status)) {
            this.isAtHome = true;
        } else if ("AWAY".equals(status)) {
            this.isAtHome = false;
        } else {
            // Pentru ACTIVE sau UNKNOWN, calculează pe baza distanței
            this.isAtHome = calculateIsAtHome();
        }
    }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) {
        this.latitude = latitude;
        // Recalculează isAtHome când se schimbă coordonatele
        this.isAtHome = calculateIsAtHome();
    }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) {
        this.longitude = longitude;
        // Recalculează isAtHome când se schimbă coordonatele
        this.isAtHome = calculateIsAtHome();
    }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public boolean isAtHome() { return isAtHome; }
    public void setAtHome(boolean atHome) { this.isAtHome = atHome; }

    public double getHomeLatitude() { return homeLatitude; }
    public void setHomeLatitude(double homeLatitude) {
        this.homeLatitude = homeLatitude;
        // Recalculează isAtHome când se schimbă home coordinates
        this.isAtHome = calculateIsAtHome();
    }

    public double getHomeLongitude() { return homeLongitude; }
    public void setHomeLongitude(double homeLongitude) {
        this.homeLongitude = homeLongitude;
        // Recalculează isAtHome când se schimbă home coordinates
        this.isAtHome = calculateIsAtHome();
    }

    public double getRadiusMeters() { return radiusMeters; }
    public void setRadiusMeters(double radiusMeters) {
        this.radiusMeters = radiusMeters;
        // Recalculează isAtHome când se schimbă radius-ul
        this.isAtHome = calculateIsAtHome();
    }

    // Utility Methods
    public String getFormattedCoordinates() {
        return String.format("%.6f, %.6f", latitude, longitude);
    }

    public String getFormattedHomeCoordinates() {
        return String.format("%.6f, %.6f", homeLatitude, homeLongitude);
    }

    //Calculează distanța până la casă în metri
    public double getDistanceFromHome() {
        if (homeLatitude == 0.0 && homeLongitude == 0.0) {
            return Double.MAX_VALUE; // Home not set
        }
        return calculateDistance(latitude, longitude, homeLatitude, homeLongitude);
    }

    // Calculează dacă utilizatorul este acasă pe baza coordonatelor și radius-ului
    private boolean calculateIsAtHome() {
        if (homeLatitude == 0.0 && homeLongitude == 0.0) {
            // Home coordinates not set, fallback to status
            return "HOME".equals(status);
        }

        double distanceFromHome = getDistanceFromHome();
        return distanceFromHome <= radiusMeters;
    }

    // Calculează distanța între două puncte GPS (formula Haversine)
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // Earth radius in meters
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c; // Distance in meters
    }

    // Update home location și recalculează status
    public void updateHomeLocation(double homeLatitude, double homeLongitude) {
        this.homeLatitude = homeLatitude;
        this.homeLongitude = homeLongitude;
        this.isAtHome = calculateIsAtHome();

        // Actualizează și status-ul dacă e necesar
        if (this.isAtHome && !"HOME".equals(this.status)) {
            this.status = "HOME";
        } else if (!this.isAtHome && "HOME".equals(this.status)) {
            this.status = "AWAY";
        }
    }

    // Update current location și recalculează status
    public void updateCurrentLocation(double latitude, double longitude, String address) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.timestamp = LocalDateTime.now();
        this.isAtHome = calculateIsAtHome();

        // Actualizează status-ul pe baza noii locații
        if (this.isAtHome) {
            this.status = "HOME";
        } else {
            this.status = "AWAY";
        }
    }

    // Returnează un summary al locației pentru debugging
    public String getLocationSummary() {
        return String.format("Location: %s at %s (Distance from home: %.1fm, At home: %s)",
                status, getFormattedCoordinates(), getDistanceFromHome(), isAtHome ? "Yes" : "No");
    }

    @Override
    public String toString() {
        return String.format("LocationDataDTO{userId='%s', status='%s', coordinates=%s, isAtHome=%s, address='%s'}",
                userId, status, getFormattedCoordinates(), isAtHome, address);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        LocationDataDTO that = (LocationDataDTO) obj;
        return Double.compare(that.latitude, latitude) == 0 &&
                Double.compare(that.longitude, longitude) == 0 &&
                userId != null ? userId.equals(that.userId) : that.userId == null;
    }

    @Override
    public int hashCode() {
        int result = userId != null ? userId.hashCode() : 0;
        long temp = Double.doubleToLongBits(latitude);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(longitude);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}