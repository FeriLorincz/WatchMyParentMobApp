package com.feri.watchmyparent.mobile.infrastructure.watch;

import com.feri.watchmyparent.mobile.domain.enums.SensorType;
import java.util.*;

public class WatchCapabilityRegistry {

    private static final Map<String, Set<SensorType>> DEVICE_CAPABILITIES = new HashMap<>();

    static {
        // Samsung Galaxy Watch 7 capabilities
        Set<SensorType> samsungWatch7Sensors = new HashSet<>();
        samsungWatch7Sensors.add(SensorType.HEART_RATE);
        samsungWatch7Sensors.add(SensorType.BLOOD_OXYGEN);
        samsungWatch7Sensors.add(SensorType.STEP_COUNT);
        samsungWatch7Sensors.add(SensorType.SLEEP);
        samsungWatch7Sensors.add(SensorType.BODY_TEMPERATURE);
        samsungWatch7Sensors.add(SensorType.STRESS);
        samsungWatch7Sensors.add(SensorType.ACCELEROMETER);
        samsungWatch7Sensors.add(SensorType.GYROSCOPE);
        samsungWatch7Sensors.add(SensorType.FALL_DETECTION);

        DEVICE_CAPABILITIES.put("samsung_galaxy_watch_7", samsungWatch7Sensors);
    }

    public static Set<SensorType> getSupportedSensors(String deviceId) {
        return DEVICE_CAPABILITIES.getOrDefault(deviceId, new HashSet<>());
    }

    public static boolean isSensorSupported(String deviceId, SensorType sensorType) {
        Set<SensorType> supportedSensors = getSupportedSensors(deviceId);
        return supportedSensors.contains(sensorType);
    }

    public static void registerDevice(String deviceId, Set<SensorType> supportedSensors) {
        DEVICE_CAPABILITIES.put(deviceId, supportedSensors);
    }
}
