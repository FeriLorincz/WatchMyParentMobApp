package com.feri.watchmyparent.mobile.infrastructure.database.dao;

import androidx.room.*;
import com.feri.watchmyparent.mobile.infrastructure.database.entities.SensorConfigurationEntity;
import com.feri.watchmyparent.mobile.domain.enums.SensorType;
import java.util.List;

@Dao
public interface SensorConfigurationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertSensorConfiguration(SensorConfigurationEntity config);

    @Update
    int updateSensorConfiguration(SensorConfigurationEntity config);

    @Delete
    int deleteSensorConfiguration(SensorConfigurationEntity config);

    @Query("SELECT * FROM sensor_configurations WHERE userId = :userId AND sensorType = :sensorType")
    SensorConfigurationEntity getSensorConfigurationByUserAndType(String userId, SensorType sensorType);

    @Query("SELECT * FROM sensor_configurations WHERE userId = :userId ORDER BY sensorType")
    List<SensorConfigurationEntity> getSensorConfigurationsByUser(String userId);

    @Query("SELECT * FROM sensor_configurations WHERE userId = :userId AND isEnabled = 1 ORDER BY sensorType")
    List<SensorConfigurationEntity> getEnabledSensorConfigurationsByUser(String userId);

    @Query("DELETE FROM sensor_configurations WHERE idSensorConfiguration = :configId")
    int deleteSensorConfigurationById(String configId);
}
