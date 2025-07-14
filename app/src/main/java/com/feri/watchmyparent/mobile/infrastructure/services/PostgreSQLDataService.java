package com.feri.watchmyparent.mobile.infrastructure.services;

import android.util.Log;
import com.feri.watchmyparent.mobile.domain.entities.LocationData;
import com.feri.watchmyparent.mobile.domain.entities.SensorData;
import com.feri.watchmyparent.mobile.infrastructure.database.PostgreSQLConfig;

import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PostgreSQLDataService {

    private static final String TAG = "PostgreSQLDataService";
    private final PostgreSQLConfig postgreSQLConfig;

    @Inject
    public PostgreSQLDataService(PostgreSQLConfig postgreSQLConfig) {
        this.postgreSQLConfig = postgreSQLConfig;
    }

    public CompletableFuture<Boolean> insertSensorData(SensorData sensorData) {
        if (postgreSQLConfig.isOfflineMode()) {
            Log.w(TAG, "⚠️ Cannot insert sensor data - PostgreSQL in offline mode");
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                // For the MVP, we'll just log success and return true
                Log.d(TAG, "✅ Sensor data insertion simulated: " +
                        sensorData.getSensorType() + " = " + sensorData.getValue() + " " +
                        sensorData.getUnit());
                return true;
            } catch (Exception e) {
                Log.e(TAG, "❌ Failed to insert sensor data", e);
                return false;
            }
        });
    }

    public CompletableFuture<Boolean> insertLocationData(LocationData locationData) {
        if (postgreSQLConfig.isOfflineMode()) {
            Log.w(TAG, "⚠️ Cannot insert location data - PostgreSQL in offline mode");
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                // For the MVP, we'll just log success and return true
                Log.d(TAG, "✅ Location data insertion simulated: " +
                        locationData.getLocationStatus().getStatus() + " at " +
                        locationData.getLocationStatus().getLatitude() + "," +
                        locationData.getLocationStatus().getLongitude());
                return true;
            } catch (Exception e) {
                Log.e(TAG, "❌ Failed to insert location data", e);
                return false;
            }
        });
    }

    public CompletableFuture<Boolean> insertTestData() {
        return postgreSQLConfig.insertTestData();
    }
}