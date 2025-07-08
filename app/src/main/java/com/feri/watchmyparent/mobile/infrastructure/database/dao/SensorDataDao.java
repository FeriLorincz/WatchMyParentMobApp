package com.feri.watchmyparent.mobile.infrastructure.database.dao;

import androidx.room.*;
import com.feri.watchmyparent.mobile.infrastructure.database.entities.SensorDataEntity;
import com.feri.watchmyparent.mobile.domain.enums.SensorType;
import com.feri.watchmyparent.mobile.domain.enums.TransmissionStatus;
import java.util.List;

@Dao
public interface SensorDataDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertSensorData(SensorDataEntity sensorData);

    @Update
    int updateSensorData(SensorDataEntity sensorData);

    @Delete
    int deleteSensorData(SensorDataEntity sensorData);

    @Query("SELECT * FROM sensor_data WHERE userId = :userId AND sensorType = :sensorType ORDER BY timestamp DESC")
    List<SensorDataEntity> getSensorDataByUserAndType(String userId, SensorType sensorType);

    @Query("SELECT * FROM sensor_data WHERE transmissionStatus = :status ORDER BY timestamp ASC")
    List<SensorDataEntity> getSensorDataByTransmissionStatus(TransmissionStatus status);

    @Query("SELECT * FROM sensor_data WHERE userId = :userId ORDER BY timestamp DESC")
    List<SensorDataEntity> getAllSensorDataByUser(String userId);

    @Query("SELECT * FROM sensor_data WHERE userId = :userId GROUP BY sensorType ORDER BY timestamp DESC")
    List<SensorDataEntity> getLatestSensorDataByUser(String userId);

    @Query("SELECT * FROM sensor_data WHERE transmissionStatus IN (:statuses) ORDER BY timestamp ASC")
    List<SensorDataEntity> getPendingTransmissions(TransmissionStatus... statuses);

    @Query("DELETE FROM sensor_data WHERE idSensorData = :sensorDataId")
    int deleteSensorDataById(String sensorDataId);

    @Query("SELECT * FROM sensor_data WHERE userId = :userId ORDER BY timestamp DESC LIMIT :limit")
    List<SensorDataEntity> getSensorDataByUserWithLimit(String userId, int limit);
}
