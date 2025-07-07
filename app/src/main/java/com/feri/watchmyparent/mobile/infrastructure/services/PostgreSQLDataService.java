package com.feri.watchmyparent.mobile.infrastructure.services;

import android.util.Log;

import com.feri.watchmyparent.mobile.infrastructure.database.PostgreSQLConfig;
import com.feri.watchmyparent.mobile.domain.entities.SensorData;
import com.feri.watchmyparent.mobile.domain.entities.LocationData;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.CompletableFuture;

@Singleton
public class PostgreSQLDataService {

    @Inject
    public PostgreSQLDataService() {
    }

    public CompletableFuture<Boolean> insertSensorData(SensorData sensorData) {
        return PostgreSQLConfig.getConnection()
                .thenApply(connection -> {
                    try {
                        // FIXARE: Înlocuit text block cu string concatenare pentru Java 8
                        String sql = "INSERT INTO sensor_data (" +
                                "id_sensor_data, user_id, sensor_type, value, unit, " +
                                "timestamp, transmission_status, device_id, metadata" +
                                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb)";

                        PreparedStatement stmt = connection.prepareStatement(sql);
                        stmt.setString(1, sensorData.getIdSensorData());
                        stmt.setString(2, sensorData.getUser().getIdUser());
                        stmt.setString(3, sensorData.getSensorType().getCode());
                        stmt.setDouble(4, sensorData.getValue());
                        stmt.setString(5, sensorData.getUnit());

                        // FIXARE: Conversie corectă LocalDateTime -> Timestamp pentru Java 8
                        Timestamp timestamp = convertLocalDateTimeToTimestamp(sensorData.getTimestamp());
                        stmt.setTimestamp(6, timestamp);

                        stmt.setString(7, sensorData.getTransmissionStatus().name());
                        stmt.setString(8, sensorData.getDeviceId());
                        stmt.setString(9, sensorData.getMetadata() != null ? sensorData.getMetadata() : "{}");

                        int rowsAffected = stmt.executeUpdate();

                        stmt.close();
                        PostgreSQLConfig.closeConnection(connection);

                        Log.d("PostgreSQLDataService", "Sensor data inserted successfully: " + rowsAffected + " rows affected");
                        return rowsAffected > 0;

                    } catch (SQLException e) {
                        Log.e("PostgreSQLDataService", "Error inserting sensor data: " + e.getMessage());

                        PostgreSQLConfig.closeConnection(connection);
                        return false;
                    }
                })
                .exceptionally(throwable -> {
                    Log.e("PostgreSQLDataService", "Failed to insert sensor data: " + throwable.getMessage());
                    return false;
                });
    }

    public CompletableFuture<Boolean> insertLocationData(LocationData locationData) {
        return PostgreSQLConfig.getConnection()
                .thenApply(connection -> {
                    try {
                        // FIXARE: Înlocuit text block cu string concatenare pentru Java 8
                        String sql = "INSERT INTO location_data (" +
                                "id_location_data, user_id, location_status, " +
                                "home_latitude, home_longitude, radius_meters, device_id" +
                                ") VALUES (?, ?, ?::jsonb, ?, ?, ?, ?) " +
                                "ON CONFLICT (user_id) " +
                                "DO UPDATE SET " +
                                "location_status = EXCLUDED.location_status, " +
                                "home_latitude = EXCLUDED.home_latitude, " +
                                "home_longitude = EXCLUDED.home_longitude, " +
                                "updated_at = CURRENT_TIMESTAMP";

                        PreparedStatement stmt = connection.prepareStatement(sql);
                        stmt.setString(1, locationData.getIdLocationData());
                        stmt.setString(2, locationData.getUser().getIdUser());

                        // Convert LocationStatus to JSON string - VERSIUNE JAVA 8
                        String locationStatusJson = buildLocationStatusJson(locationData);
                        stmt.setString(3, locationStatusJson);

                        stmt.setDouble(4, locationData.getHomeLatitude());
                        stmt.setDouble(5, locationData.getHomeLongitude());
                        stmt.setDouble(6, locationData.getRadiusMeters());
                        stmt.setString(7, locationData.getDeviceId());

                        int rowsAffected = stmt.executeUpdate();

                        stmt.close();
                        PostgreSQLConfig.closeConnection(connection);

                        Log.d("PostgreSQLDataService", "Location data inserted successfully: " + rowsAffected + " rows affected");
                        return rowsAffected > 0;

                    } catch (SQLException e) {
                        Log.e("PostgreSQLDataService", "Error inserting location data: " + e.getMessage());
                        PostgreSQLConfig.closeConnection(connection);
                        return false;
                    }
                })
                .exceptionally(throwable -> {
                    Log.e("PostgreSQLDataService", "Failed to insert location data: " + throwable.getMessage());
                    return false;
                });
    }

    // HELPER METHODS pentru Java 8

    /**
     * Convertește LocalDateTime la Timestamp pentru Java 8
     */
    private Timestamp convertLocalDateTimeToTimestamp(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return new Timestamp(System.currentTimeMillis());
        }
        // Folosește epochSecond pentru compatibilitate Java 8
        long epochSecond = localDateTime.toEpochSecond(ZoneOffset.UTC);
        int nano = localDateTime.getNano();
        Timestamp timestamp = new Timestamp(epochSecond * 1000);
        timestamp.setNanos(nano);
        return timestamp;
    }

    /**
     * Construiește JSON pentru LocationStatus compatibil cu Java 8
     */
    private String buildLocationStatusJson(LocationData locationData) {
        if (locationData.getLocationStatus() == null) {
            return "{}";
        }

        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"status\":\"").append(escapeJson(locationData.getLocationStatus().getStatus())).append("\",");
        json.append("\"latitude\":").append(locationData.getLocationStatus().getLatitude()).append(",");
        json.append("\"longitude\":").append(locationData.getLocationStatus().getLongitude()).append(",");
        json.append("\"address\":\"").append(escapeJson(locationData.getLocationStatus().getAddress())).append("\",");
        json.append("\"timestamp\":\"").append(locationData.getLocationStatus().getTimestamp().toString()).append("\"");
        json.append("}");

        return json.toString();
    }

    /**
     * Escape JSON strings pentru a evita probleme cu quotes
     */
    private String escapeJson(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    /**
     * Inserează date de test pentru verificare
     */
    public CompletableFuture<Boolean> insertTestData() {
        return PostgreSQLConfig.getConnection()
                .thenApply(connection -> {
                    try {
                        String sql = "INSERT INTO sensor_data (" +
                                "id_sensor_data, user_id, sensor_type, value, unit, device_id" +
                                ") VALUES (?, ?, ?, ?, ?, ?) " +
                                "ON CONFLICT (id_sensor_data) DO NOTHING";

                        PreparedStatement stmt = connection.prepareStatement(sql);
                        stmt.setString(1, "test-" + System.currentTimeMillis());
                        stmt.setString(2, "demo-user-id");
                        stmt.setString(3, "heart_rate");
                        stmt.setDouble(4, 75.0);
                        stmt.setString(5, "bpm");
                        stmt.setString(6, "samsung_galaxy_watch_7");

                        int rowsAffected = stmt.executeUpdate();

                        stmt.close();
                        PostgreSQLConfig.closeConnection(connection);

                        Log.d("PostgreSQLDataService", "Test data inserted successfully: " + rowsAffected + " rows affected");
                        return rowsAffected > 0;

                    } catch (SQLException e) {
                        Log.e("PostgreSQLDataService", "Error inserting test data: " + e.getMessage());
                        PostgreSQLConfig.closeConnection(connection);
                        return false;
                    }
                });
    }
}
