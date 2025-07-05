package com.feri.watchmyparent.mobile.infrastructure.database.converters;

import androidx.room.TypeConverter;
import com.feri.watchmyparent.mobile.domain.enums.UserType;

public class UserTypeConverter {

    @TypeConverter
    public static UserType fromString(String value) {
        return value == null ? null : UserType.valueOf(value);
    }

    @TypeConverter
    public static String fromUserType(UserType userType) {
        return userType == null ? null : userType.name();
    }
}
