package com.feri.watchmyparent.mobile.infrastructure.services;

import android.util.Log;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.feri.watchmyparent.mobile.application.dto.SensorDataDTO;
import com.feri.watchmyparent.mobile.infrastructure.database.converters.DateTimeConverter;
import com.feri.watchmyparent.mobile.infrastructure.database.converters.SensorTypeConverter;
import com.feri.watchmyparent.mobile.domain.enums.SensorType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;
import javax.inject.Singleton;

//Gestionează stocarea offline temporară când Kafka nu e disponibil
//Folosește Room database pentru persistența locală
@Singleton
public class OfflineDataManager {

    private static final String TAG = "OfflineDataManager";
    private static final int MAX_OFFLINE_RECORDS = 10000; // Limita pentru evitarea overflow-ului

    private final OfflineDataDatabase database;

    @Inject
    public OfflineDataManager(android.content.Context context) {
        this.database = Room.databaseBuilder(
                context.getApplicationContext(),
                OfflineDataDatabase.class,
                "offline_health_data"
        ).build();

        Log.d(TAG, "✅ OfflineDataManager initialized with Room database");
    }

    // Salvează datele local când Kafka nu e disponibi
    public CompletableFuture<Boolean> storeOfflineData(SensorDataDTO sensorData) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Verifică dacă am prea multe înregistrări offline
                int offlineCount = database.offlineDao().getOfflineCount();
                if (offlineCount >= MAX_OFFLINE_RECORDS) {
                    // Șterge cele mai vechi 1000 de înregistrări
                    database.offlineDao().deleteOldestRecords(1000);
                    Log.w(TAG, "⚠️ Cleaned " + 1000 + " old offline records (limit: " + MAX_OFFLINE_RECORDS + ")");
                }

                // Creează entitatea pentru stocarea offline
                OfflineHealthData offlineData = new OfflineHealthData();
                offlineData.userId = sensorData.getUserId();
                offlineData.sensorType = sensorData.getSensorType();
                offlineData.value = sensorData.getValue();
                offlineData.unit = sensorData.getUnit();
                offlineData.timestamp = sensorData.getTimestamp();
                offlineData.deviceId = sensorData.getDeviceId();
                offlineData.retryCount = sensorData.getRetryCount();
                offlineData.createdAt = LocalDateTime.now();

                long id = database.offlineDao().insertOfflineData(offlineData);

                if (id > 0) {
                    Log.d(TAG, "💾 Stored offline: " + sensorData.getSensorType() +
                            " for user " + sensorData.getUserId());
                    return true;
                } else {
                    Log.e(TAG, "❌ Failed to store offline data");
                    return false;
                }

            } catch (Exception e) {
                Log.e(TAG, "❌ Error storing offline data", e);
                return false;
            }
        });
    }

    // Recuperează toate datele offline pentru transmitere
    public CompletableFuture<List<OfflineHealthData>> getOfflineData() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<OfflineHealthData> offlineData = database.offlineDao().getAllOfflineData();
                Log.d(TAG, "📤 Retrieved " + offlineData.size() + " offline records for transmission");
                return offlineData;

            } catch (Exception e) {
                Log.e(TAG, "❌ Error retrieving offline data", e);
                return new java.util.ArrayList<>();
            }
        });
    }

    // Șterge datele offline după transmiterea cu succes
    public CompletableFuture<Boolean> deleteOfflineData(List<Long> ids) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                int deleted = database.offlineDao().deleteOfflineDataByIds(ids);
                Log.d(TAG, "🗑️ Deleted " + deleted + " offline records after successful transmission");
                return deleted > 0;

            } catch (Exception e) {
                Log.e(TAG, "❌ Error deleting offline data", e);
                return false;
            }
        });
    }

    // Incrementează retry count pentru înregistrările eșuate
    public CompletableFuture<Boolean> incrementRetryCount(List<Long> ids) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                int updated = database.offlineDao().incrementRetryCount(ids);
                Log.d(TAG, "🔄 Updated retry count for " + updated + " records");
                return updated > 0;

            } catch (Exception e) {
                Log.e(TAG, "❌ Error updating retry count", e);
                return false;
            }
        });
    }

    // Curăță înregistrările cu prea multe retry-uri
    public CompletableFuture<Integer> cleanupFailedRecords(int maxRetries) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                int deleted = database.offlineDao().deleteFailedRecords(maxRetries);
                if (deleted > 0) {
                    Log.w(TAG, "🧹 Cleaned up " + deleted + " failed records (max retries: " + maxRetries + ")");
                }
                return deleted;

            } catch (Exception e) {
                Log.e(TAG, "❌ Error cleaning up failed records", e);
                return 0;
            }
        });
    }

    // Obține statistici offline
    public CompletableFuture<OfflineStatistics> getOfflineStatistics() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                OfflineStatistics stats = new OfflineStatistics();
                stats.totalRecords = database.offlineDao().getOfflineCount();
                stats.pendingRecords = database.offlineDao().getPendingCount();
                stats.failedRecords = database.offlineDao().getFailedCount(3);
                stats.oldestRecord = database.offlineDao().getOldestRecordTime();

                return stats;

            } catch (Exception e) {
                Log.e(TAG, "❌ Error getting offline statistics", e);
                return new OfflineStatistics();
            }
        });
    }

    // Entity pentru stocarea offline
    @Entity(tableName = "offline_health_data")
    @TypeConverters({DateTimeConverter.class, SensorTypeConverter.class})
    public static class OfflineHealthData {
        @PrimaryKey(autoGenerate = true)
        public long id;

        public String userId;
        public SensorType sensorType;
        public double value;
        public String unit;
        public LocalDateTime timestamp;
        public String deviceId;
        public int retryCount = 0;
        public LocalDateTime createdAt;
    }

    // DAO pentru operațiuni offline
    @Dao
    public interface OfflineHealthDataDao {
        @Insert
        long insertOfflineData(OfflineHealthData data);

        @Query("SELECT * FROM offline_health_data ORDER BY createdAt ASC")
        List<OfflineHealthData> getAllOfflineData();

        @Query("DELETE FROM offline_health_data WHERE id IN (:ids)")
        int deleteOfflineDataByIds(List<Long> ids);

        @Query("UPDATE offline_health_data SET retryCount = retryCount + 1 WHERE id IN (:ids)")
        int incrementRetryCount(List<Long> ids);

        @Query("DELETE FROM offline_health_data WHERE retryCount >= :maxRetries")
        int deleteFailedRecords(int maxRetries);

        @Query("DELETE FROM offline_health_data WHERE id IN (SELECT id FROM offline_health_data ORDER BY createdAt ASC LIMIT :count)")
        int deleteOldestRecords(int count);

        @Query("SELECT COUNT(*) FROM offline_health_data")
        int getOfflineCount();

        @Query("SELECT COUNT(*) FROM offline_health_data WHERE retryCount = 0")
        int getPendingCount();

        @Query("SELECT COUNT(*) FROM offline_health_data WHERE retryCount >= :maxRetries")
        int getFailedCount(int maxRetries);

        @Query("SELECT MIN(createdAt) FROM offline_health_data")
        LocalDateTime getOldestRecordTime();
    }

    // Database Room
    @Database(entities = {OfflineHealthData.class}, version = 1, exportSchema = false)
    @TypeConverters({DateTimeConverter.class, SensorTypeConverter.class})
    public abstract static class OfflineDataDatabase extends RoomDatabase {
        public abstract OfflineHealthDataDao offlineDao();
    }

    // Statistici offline
    public static class OfflineStatistics {
        public int totalRecords = 0;
        public int pendingRecords = 0;
        public int failedRecords = 0;
        public LocalDateTime oldestRecord = null;

        @Override
        public String toString() {
            return String.format("Offline Stats: Total=%d, Pending=%d, Failed=%d, Oldest=%s",
                    totalRecords, pendingRecords, failedRecords,
                    oldestRecord != null ? oldestRecord.toString() : "N/A");
        }
    }
}