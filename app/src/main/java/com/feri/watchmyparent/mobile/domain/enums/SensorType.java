package com.feri.watchmyparent.mobile.domain.enums;

public enum SensorType {

    // ✅ Samsung Health SDK Permitted Sensors (5 + STEP_COUNT din Exercise)
    HEART_RATE("heart_rate", "Heart Rate", "bpm", CriticalityLevel.CRITICAL),
    BLOOD_OXYGEN("blood_oxygen", "Blood Oxygen", "%", CriticalityLevel.CRITICAL),
    BLOOD_PRESSURE("blood_pressure", "Blood Pressure", "mmHg", CriticalityLevel.CRITICAL),
    BODY_TEMPERATURE("body_temperature", "Body Temperature", "°C", CriticalityLevel.CRITICAL), // Skin temperature
    SLEEP("sleep", "Sleep", "hours", CriticalityLevel.LONG_TERM),
    STEP_COUNT("step_count", "Step Count", "steps", CriticalityLevel.IMPORTANT), // Din Exercise - permis

    // ✅ Android Sensor API Only (11 - eliminat STEP_COUNT duplicat)
    ACCELEROMETER("accelerometer", "Accelerometer", "m/s²", CriticalityLevel.IMPORTANT),
    GYROSCOPE("gyroscope", "Gyroscope", "rad/s", CriticalityLevel.IMPORTANT),
    GRAVITY("gravity", "Gravity", "m/s²", CriticalityLevel.REGULAR),
    LINEAR_ACCELERATION("linear_acceleration", "Linear Acceleration", "m/s²", CriticalityLevel.REGULAR),
    ROTATION("rotation", "Rotation", "rad", CriticalityLevel.REGULAR),
    ORIENTATION("orientation", "Orientation", "degrees", CriticalityLevel.REGULAR),
    MAGNETIC_FIELD("magnetic_field", "Magnetic Field", "µT", CriticalityLevel.REGULAR),
    LIGHT("light", "Light", "lux", CriticalityLevel.REGULAR),
    PROXIMITY("proximity", "Proximity", "cm", CriticalityLevel.REGULAR),
    LOCATION("location", "Location", "GPS", CriticalityLevel.IMPORTANT),

    // ✅ Special Sensors (2)
    FALL_DETECTION("fall_detection", "Fall Detection", "boolean", CriticalityLevel.CRITICAL), // Via BroadcastReceiver
    STRESS("stress", "Stress Level", "score", CriticalityLevel.CRITICAL); // Din Samsung Health

    private final String code;
    private final String displayName;
    private final String unit;
    private final CriticalityLevel criticalityLevel;

    SensorType(String code, String displayName, String unit, CriticalityLevel criticalityLevel) {
        this.code = code;
        this.displayName = displayName;
        this.unit = unit;
        this.criticalityLevel = criticalityLevel;
    }

    public String getCode() { return code; }
    public String getDisplayName() { return displayName; }
    public String getUnit() { return unit; }
    public CriticalityLevel getCriticalityLevel() { return criticalityLevel; }

    // UPDATED: Verifică dacă senzorul este permis prin Samsung Health SDK
    //Bazat pe acordul primit: Blood glucose, Blood oxygen, Blood pressure, Exercise, Heart rate, Skin temperature, Sleep

    public boolean isSamsungHealthPermitted() {
        switch (this) {
            case HEART_RATE:        // Heart rate ✅
            case BLOOD_OXYGEN:      // Blood oxygen ✅
            case BLOOD_PRESSURE:    // Blood pressure ✅
            case BODY_TEMPERATURE:  // Skin temperature ✅
            case SLEEP:             // Sleep ✅
            case STEP_COUNT:        // Din Exercise ✅
            case STRESS:            // Poate fi inclus în monitoring general
                return true;
            case FALL_DETECTION:    // Special case - via BroadcastReceiver
                return false; // Nu prin SDK direct, ci prin broadcast
            default:
                return false; // Toate celelalte prin Android Sensor API
        }
    }

    // UPDATED: Obține maparea Samsung Health pentru senzor
    public String getSamsungHealthDataType() {
        switch (this) {
            case HEART_RATE:
                return "com.samsung.health.heart_rate";
            case BLOOD_OXYGEN:
                return "com.samsung.health.oxygen_saturation";
            case BLOOD_PRESSURE:
                return "com.samsung.health.blood_pressure";
            case BODY_TEMPERATURE:
                return "com.samsung.health.body_temperature"; // Skin temperature
            case SLEEP:
                return "com.samsung.health.sleep";
            case STEP_COUNT:
                return "com.samsung.health.exercise"; // Din Exercise data type
            case STRESS:
                return "com.samsung.health.stress"; // Dacă există
            default:
                return null; // Pentru Android Sensor API sensors
        }
    }

    //NEW: Verifică dacă senzorul necesită BroadcastReceiver
    public boolean requiresBroadcastReceiver() {
        return this == FALL_DETECTION;
    }

    //NEW: Obține acțiunea broadcast pentru senzor
    public String getBroadcastAction() {
        switch (this) {
            case FALL_DETECTION:
                return "com.samsung.health.FALL_DETECTION";
            default:
                return null;
        }
    }

    // NEW: Verifică dacă senzorul este disponibil pe Samsung Galaxy Watch 7
    public boolean isAvailableOnWatch() {
        switch (this) {
            // Samsung Health SDK sensors
            case HEART_RATE:
            case BLOOD_OXYGEN:
            case BLOOD_PRESSURE:
            case BODY_TEMPERATURE:
            case SLEEP:
            case STEP_COUNT:
            case STRESS:
            case FALL_DETECTION:
                return true;
            // Hardware sensors disponibili pe ceas
            case ACCELEROMETER:
            case GYROSCOPE:
            case LIGHT:
            case PROXIMITY:
                return true;
            // Nu sunt disponibili pe ceas (doar pe telefon)
            case GRAVITY:
            case LINEAR_ACCELERATION:
            case ROTATION:
            case ORIENTATION:
            case MAGNETIC_FIELD:
            case LOCATION:
                return false;
            default:
                return false;
        }
    }

    public static SensorType fromCode(String code) {
        for (SensorType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown sensor type: " + code);
    }
}