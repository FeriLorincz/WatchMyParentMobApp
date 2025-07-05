package com.feri.watchmyparent.mobile.infrastructure.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ForeignKey;
import androidx.room.TypeConverters;
import androidx.room.Index;
import com.feri.watchmyparent.mobile.infrastructure.database.converters.StringSetConverter;
import com.feri.watchmyparent.mobile.infrastructure.database.converters.MedicationSetConverter;
import com.feri.watchmyparent.mobile.domain.valueobjects.Medication;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.HashSet;

@Entity(
        tableName = "medical_profiles",
        foreignKeys = @ForeignKey(
                entity = UserEntity.class,
                parentColumns = "idUser",
                childColumns = "userId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index(value = "userId", unique = true)}
)
@TypeConverters({StringSetConverter.class, MedicationSetConverter.class})
public class MedicalProfileEntity {

    @PrimaryKey
    public String idMedicalProfile;

    public String userId;

    public Set<String> currentDiseases = new HashSet<>();
    public Set<String> sensorModifyingConditions = new HashSet<>();
    public boolean hasAthleticHistory;
    public String athleticHistoryDetails;
    public Set<Medication> medications = new HashSet<>();
    public boolean gdprConsent;
    public boolean disclaimerAccepted;
    public boolean emergencyEntryPermission;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;

    public MedicalProfileEntity() {
        // Default constructor required by Room
    }
}
