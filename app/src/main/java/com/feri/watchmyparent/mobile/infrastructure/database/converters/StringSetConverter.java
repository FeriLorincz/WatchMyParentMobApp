package com.feri.watchmyparent.mobile.infrastructure.database.converters;

import androidx.room.TypeConverter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

public class StringSetConverter {

    private static final Gson gson = new Gson();

    @TypeConverter
    public static Set<String> fromString(String value) {
        if (value == null) return new HashSet<>();
        Type type = new TypeToken<Set<String>>(){}.getType();
        Set<String> result = gson.fromJson(value, type);
        return result != null ? result : new HashSet<>();
    }

    @TypeConverter
    public static String fromStringSet(Set<String> set) {
        return set == null ? null : gson.toJson(set);
    }
}
