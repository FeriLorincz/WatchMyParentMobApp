package com.feri.watchmyparent.mobile.infrastructure.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ForeignKey;
import androidx.room.Index;
import com.feri.watchmyparent.mobile.domain.enums.SensorType;
import com.feri.watchmyparent.mobile.domain.enums.TransmissionStatus;
import java.time.LocalDateTime;

@Entity(
        tableName = "sensor_data",
        foreignKeys = @ForeignKey(
                entity = UserEntity.class,
                parentColumns = "idUser",
                childColumns = "userId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {
                @Index(value = {"userId", "sensorType", "timestamp"}),
                @Index(value = {"transmissionStatus"})
        }
)
public class SensorDataEntity {
    @PrimaryKey
    public String idSensorData;

    public String userId;
    public SensorType sensorType;
    public double value;
    public String unit;
    public LocalDateTime timestamp;
    public TransmissionStatus transmissionStatus;
    public LocalDateTime transmissionTime;
    public String deviceId;
    public String metadata;
}
