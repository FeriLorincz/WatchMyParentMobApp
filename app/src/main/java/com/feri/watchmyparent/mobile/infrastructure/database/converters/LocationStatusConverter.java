package com.feri.watchmyparent.mobile.infrastructure.database.converters;

import androidx.room.TypeConverter;
import com.feri.watchmyparent.mobile.domain.valueobjects.LocationStatus;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

public class LocationStatusConverter {

    private static final Gson gson = new Gson();

    @TypeConverter
    public static LocationStatus fromString(String value) {
        if (value == null) return null;
        Type type = new TypeToken<LocationStatus>(){}.getType();
        return gson.fromJson(value, type);
    }

    @TypeConverter
    public static String fromLocationStatus(LocationStatus locationStatus) {
        return locationStatus == null ? null : gson.toJson(locationStatus);
    }
}
