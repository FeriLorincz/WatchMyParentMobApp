package com.feri.watchmyparent.mobile.infrastructure.database.converters;

import androidx.room.TypeConverter;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DateTimeConverter {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    @TypeConverter
    public static LocalDateTime fromTimestamp(String value) {
        return value == null ? null : LocalDateTime.parse(value, DATE_TIME_FORMATTER);
    }

    @TypeConverter
    public static String dateTimeToTimestamp(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.format(DATE_TIME_FORMATTER);
    }

    @TypeConverter
    public static LocalDate fromDateString(String value) {
        return value == null ? null : LocalDate.parse(value, DATE_FORMATTER);
    }

    @TypeConverter
    public static String dateToString(LocalDate date) {
        return date == null ? null : date.format(DATE_FORMATTER);
    }
}
