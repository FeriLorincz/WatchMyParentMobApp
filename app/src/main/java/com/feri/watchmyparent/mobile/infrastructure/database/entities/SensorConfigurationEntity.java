package com.feri.watchmyparent.mobile.infrastructure.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ForeignKey;
import androidx.room.Index;
import com.feri.watchmyparent.mobile.domain.enums.SensorType;
import java.time.LocalDateTime;

@Entity(
        tableName = "sensor_configurations",
        foreignKeys = @ForeignKey(
                entity = UserEntity.class,
                parentColumns = "idUser",
                childColumns = "userId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = @Index(value = {"userId", "sensorType"}, unique = true)
)
public class SensorConfigurationEntity {

    @PrimaryKey
    public String idSensorConfiguration;

    public String userId;
    public SensorType sensorType;
    public int frequencySeconds;
    public boolean isEnabled;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}
