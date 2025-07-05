package com.feri.watchmyparent.mobile.infrastructure.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ForeignKey;
import androidx.room.Embedded;
import com.feri.watchmyparent.mobile.domain.valueobjects.LocationStatus;
import java.time.LocalDateTime;

@Entity(
        tableName = "location_data",
        foreignKeys = @ForeignKey(
                entity = UserEntity.class,
                parentColumns = "idUser",
                childColumns = "userId",
                onDelete = ForeignKey.CASCADE
        )
)
public class LocationDataEntity {

    @PrimaryKey
    public String idLocationData;

    public String userId;

    @Embedded
    public LocationStatus locationStatus;

    public double homeLatitude;
    public double homeLongitude;
    public double radiusMeters;
    public String deviceId;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}
