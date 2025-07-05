package com.feri.watchmyparent.mobile.infrastructure.database.dao;

import androidx.room.*;
import com.feri.watchmyparent.mobile.infrastructure.database.entities.LocationDataEntity;

@Dao
public interface LocationDataDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertLocationData(LocationDataEntity locationData);

    @Update
    int updateLocationData(LocationDataEntity locationData);

    @Delete
    int deleteLocationData(LocationDataEntity locationData);

    @Query("SELECT * FROM location_data WHERE userId = :userId ORDER BY updatedAt DESC LIMIT 1")
    LocationDataEntity getLocationDataByUser(String userId);

    @Query("DELETE FROM location_data WHERE idLocationData = :locationDataId")
    int deleteLocationDataById(String locationDataId);
}
