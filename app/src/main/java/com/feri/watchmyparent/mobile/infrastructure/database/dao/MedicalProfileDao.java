package com.feri.watchmyparent.mobile.infrastructure.database.dao;

import androidx.room.*;
import com.feri.watchmyparent.mobile.infrastructure.database.entities.MedicalProfileEntity;

@Dao
public interface MedicalProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertMedicalProfile(MedicalProfileEntity profile);

    @Update
    int updateMedicalProfile(MedicalProfileEntity profile);

    @Delete
    int deleteMedicalProfile(MedicalProfileEntity profile);

    @Query("SELECT * FROM medical_profiles WHERE idMedicalProfile = :profileId")
    MedicalProfileEntity getMedicalProfileById(String profileId);

    @Query("SELECT * FROM medical_profiles WHERE userId = :userId")
    MedicalProfileEntity getMedicalProfileByUser(String userId);

    @Query("DELETE FROM medical_profiles WHERE idMedicalProfile = :profileId")
    int deleteMedicalProfileById(String profileId);

    @Query("DELETE FROM medical_profiles WHERE userId = :userId")
    int deleteMedicalProfileByUser(String userId);

    @Query("SELECT COUNT(*) > 0 FROM medical_profiles WHERE userId = :userId")
    boolean existsByUserId(String userId);
}
