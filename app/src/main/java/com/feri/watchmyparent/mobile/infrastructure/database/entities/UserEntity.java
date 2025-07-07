package com.feri.watchmyparent.mobile.infrastructure.database.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Embedded;
import com.feri.watchmyparent.mobile.domain.enums.UserType;
import com.feri.watchmyparent.mobile.domain.valueobjects.AddressUser;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity(tableName = "users")
public class UserEntity {
    @PrimaryKey
    @NonNull
    public String idUser;

    public String firstNameUser;
    public String lastNameUser;
    public LocalDate dateOfBirthUser;
    public UserType userType;

    @Embedded
    public AddressUser addressUser;

    public String phoneNumberUser;
    public String emailUser;
    public String password;
    public String resetPasswordToken;
    public LocalDateTime resetPasswordTokenExpiry;

    public boolean accountNonExpired = true;
    public boolean accountNonLocked = true;
    public boolean credentialsNonExpired = true;
    public boolean enabled = true;

    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}
