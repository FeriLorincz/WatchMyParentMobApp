package com.feri.watchmyparent.mobile.infrastructure.database.converters;

import androidx.room.TypeConverter;
import com.feri.watchmyparent.mobile.domain.enums.SensorType;

public class SensorTypeConverter {

    @TypeConverter
    public static SensorType fromString(String value) {
        return value == null ? null : SensorType.fromCode(value);
    }

    @TypeConverter
    public static String fromSensorType(SensorType sensorType) {
        return sensorType == null ? null : sensorType.getCode();
    }
}
