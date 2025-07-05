package com.feri.watchmyparent.mobile.domain.enums;

public enum SensorType {

    // Vital Signs (6)
    HEART_RATE("heart_rate", "Heart Rate", "bpm", CriticalityLevel.CRITICAL),
    BLOOD_OXYGEN("blood_oxygen", "Blood Oxygen", "%", CriticalityLevel.CRITICAL),
    BLOOD_PRESSURE("blood_pressure", "Blood Pressure", "mmHg", CriticalityLevel.CRITICAL),
    BODY_TEMPERATURE("body_temperature", "Body Temperature", "°C", CriticalityLevel.CRITICAL),
    STRESS("stress", "Stress Level", "score", CriticalityLevel.CRITICAL),
    FALL_DETECTION("fall_detection", "Fall Detection", "boolean", CriticalityLevel.CRITICAL),

    // Motion Sensors (8)
    ACCELEROMETER("accelerometer", "Accelerometer", "m/s²", CriticalityLevel.IMPORTANT),
    GYROSCOPE("gyroscope", "Gyroscope", "rad/s", CriticalityLevel.IMPORTANT),
    STEP_COUNT("step_count", "Step Count", "steps", CriticalityLevel.IMPORTANT),
    GRAVITY("gravity", "Gravity", "m/s²", CriticalityLevel.REGULAR),
    LINEAR_ACCELERATION("linear_acceleration", "Linear Acceleration", "m/s²", CriticalityLevel.REGULAR),
    ROTATION("rotation", "Rotation", "rad", CriticalityLevel.REGULAR),
    ORIENTATION("orientation", "Orientation", "degrees", CriticalityLevel.REGULAR),
    MAGNETIC_FIELD("magnetic_field", "Magnetic Field", "µT", CriticalityLevel.REGULAR),

    // Environment (3)
    HUMIDITY("humidity", "Humidity", "%", CriticalityLevel.REGULAR),
    LIGHT("light", "Light", "lux", CriticalityLevel.REGULAR),
    PROXIMITY("proximity", "Proximity", "cm", CriticalityLevel.REGULAR),

    // Samsung Health (3)
    BIA("bia", "BIA", "Ω", CriticalityLevel.LONG_TERM),
    SLEEP("sleep", "Sleep", "hours", CriticalityLevel.LONG_TERM),

    // Location (1)
    LOCATION("location", "Location", "GPS", CriticalityLevel.IMPORTANT);

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

    public static SensorType fromCode(String code) {
        for (SensorType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown sensor type: " + code);
    }
}
