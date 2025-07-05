package com.feri.watchmyparent.mobile.infrastructure.database.converters;

import androidx.room.TypeConverter;
import com.feri.watchmyparent.mobile.domain.enums.TransmissionStatus;

public class TransmissionStatusConverter {

    @TypeConverter
    public static TransmissionStatus fromString(String value) {
        return value == null ? null : TransmissionStatus.valueOf(value);
    }

    @TypeConverter
    public static String fromTransmissionStatus(TransmissionStatus status) {
        return status == null ? null : status.name();
    }
}
