package com.feri.watchmyparent.mobile.infrastructure.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ForeignKey;
import androidx.room.Embedded;
import androidx.room.Index;
import com.feri.watchmyparent.mobile.domain.valueobjects.AddressContact;
import java.time.LocalDateTime;

@Entity(
        tableName = "emergency_contacts",
        foreignKeys = @ForeignKey(
                entity = UserEntity.class,
                parentColumns = "idUser",
                childColumns = "userId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index(value = "userId")}
)
public class EmergencyContactEntity {
    @PrimaryKey
    public String idContact;

    public String userId;

    @Embedded
    public AddressContact addressContact;

    public String firstNameContact;
    public String lastNameContact;
    public String relationship;
    public String phoneNumberContact;
    public String emailContact;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;

    // Default constructor required by Room
    public EmergencyContactEntity() {
    }
}
