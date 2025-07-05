package com.feri.watchmyparent.mobile.infrastructure.database.dao;

import androidx.room.*;
import com.feri.watchmyparent.mobile.infrastructure.database.entities.EmergencyContactEntity;
import java.util.List;

@Dao
public interface EmergencyContactDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertEmergencyContact(EmergencyContactEntity contact);

    @Update
    int updateEmergencyContact(EmergencyContactEntity contact);

    @Delete
    int deleteEmergencyContact(EmergencyContactEntity contact);

    @Query("SELECT * FROM emergency_contacts WHERE idContact = :contactId")
    EmergencyContactEntity getEmergencyContactById(String contactId);

    @Query("SELECT * FROM emergency_contacts WHERE userId = :userId ORDER BY firstNameContact, lastNameContact")
    List<EmergencyContactEntity> getEmergencyContactsByUser(String userId);

    @Query("DELETE FROM emergency_contacts WHERE idContact = :contactId")
    int deleteEmergencyContactById(String contactId);

    @Query("DELETE FROM emergency_contacts WHERE userId = :userId")
    int deleteAllEmergencyContactsByUser(String userId);

    @Query("SELECT COUNT(*) FROM emergency_contacts WHERE userId = :userId")
    int getContactCountByUser(String userId);
}
