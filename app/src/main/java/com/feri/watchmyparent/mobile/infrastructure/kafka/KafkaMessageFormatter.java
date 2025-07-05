package com.feri.watchmyparent.mobile.infrastructure.kafka;

import com.feri.watchmyparent.mobile.domain.entities.SensorData;
import com.feri.watchmyparent.mobile.domain.entities.LocationData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class KafkaMessageFormatter {

    private final Gson gson;

    public KafkaMessageFormatter() {
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, (com.google.gson.JsonSerializer<LocalDateTime>)
                        (src, typeOfSrc, context) -> context.serialize(src.toString()))
                .create();
    }

    public String formatSensorData(SensorData sensorData) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "SENSOR_DATA");
        message.put("userId", sensorData.getUser().getIdUser());
        message.put("sensorType", sensorData.getSensorType().getCode());
        message.put("value", sensorData.getValue());
        message.put("unit", sensorData.getUnit());
        message.put("timestamp", sensorData.getTimestamp().toString());
        message.put("deviceId", sensorData.getDeviceId());
        message.put("criticality", sensorData.getSensorType().getCriticalityLevel().name());

        return gson.toJson(message);
    }

    public String formatLocationData(LocationData locationData) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "LOCATION_DATA");
        message.put("userId", locationData.getUser().getIdUser());
        message.put("status", locationData.getLocationStatus().getStatus());
        message.put("latitude", locationData.getLocationStatus().getLatitude());
        message.put("longitude", locationData.getLocationStatus().getLongitude());
        message.put("address", locationData.getLocationStatus().getAddress());
        message.put("timestamp", locationData.getLocationStatus().getTimestamp().toString());
        message.put("isAtHome", locationData.isAtHome());

        return gson.toJson(message);
    }

    public String formatUserData(Object userData) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "USER_DATA");
        message.put("data", userData);
        message.put("timestamp", LocalDateTime.now().toString());

        return gson.toJson(message);
    }
}
