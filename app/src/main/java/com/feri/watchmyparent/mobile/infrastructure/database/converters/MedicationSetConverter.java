package com.feri.watchmyparent.mobile.infrastructure.database.converters;

import androidx.room.TypeConverter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.feri.watchmyparent.mobile.domain.valueobjects.Medication;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

public class MedicationSetConverter {

    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, new LocalDateTypeAdapter())
            .create();

    @TypeConverter
    public static Set<Medication> fromString(String value) {
        if (value == null) return new HashSet<>();
        Type type = new TypeToken<Set<Medication>>(){}.getType();
        Set<Medication> result = gson.fromJson(value, type);
        return result != null ? result : new HashSet<>();
    }

    @TypeConverter
    public static String fromMedicationSet(Set<Medication> medications) {
        return medications == null ? null : gson.toJson(medications);
    }

    // Helper class for LocalDate serialization
    private static class LocalDateTypeAdapter implements com.google.gson.JsonSerializer<LocalDate>, com.google.gson.JsonDeserializer<LocalDate> {
        @Override
        public com.google.gson.JsonElement serialize(LocalDate src, Type typeOfSrc, com.google.gson.JsonSerializationContext context) {
            return new com.google.gson.JsonPrimitive(src.toString());
        }

        @Override
        public LocalDate deserialize(com.google.gson.JsonElement json, Type typeOfT, com.google.gson.JsonDeserializationContext context) {
            return LocalDate.parse(json.getAsString());
        }
    }
}
