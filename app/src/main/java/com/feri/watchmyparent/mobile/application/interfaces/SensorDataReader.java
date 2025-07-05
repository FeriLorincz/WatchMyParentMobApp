package com.feri.watchmyparent.mobile.application.interfaces;

import com.feri.watchmyparent.mobile.domain.enums.SensorType;
import com.feri.watchmyparent.mobile.domain.valueobjects.SensorReading;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface SensorDataReader {

    CompletableFuture<List<SensorReading>> readSensorData(List<SensorType> sensorTypes);
    CompletableFuture<SensorReading> readSingleSensor(SensorType sensorType);
    CompletableFuture<Boolean> isSensorAvailable(SensorType sensorType);
}
