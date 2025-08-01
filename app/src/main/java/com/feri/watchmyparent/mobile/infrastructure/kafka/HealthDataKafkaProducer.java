package com.feri.watchmyparent.mobile.infrastructure.kafka;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSerializer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

public class HealthDataKafkaProducer {

    private static final String TAG = "HealthDataKafkaProducer";
    private final Gson gson;
    private final String topicName;

    public HealthDataKafkaProducer() {
        Log.d(TAG, "Inițializare mock Kafka Producer pentru mediul Android");

        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context)
                        -> context.serialize(src.toString()))
                .create();

        // Simulăm numele topicului
        this.topicName = "health_data";

        Log.d(TAG, "Mock Kafka Producer inițializat cu succes");
    }

    public CompletableFuture<Boolean> sendHealthData(Object healthData, String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String jsonData = gson.toJson(healthData);

                // În loc să trimitem la Kafka, doar logăm ce ar fi fost trimis
                Log.d(TAG, String.format("MOCK KAFKA: Ar fi trimis date de sănătate la topicul %s", topicName));
                Log.d(TAG, String.format("MOCK KAFKA: UserId: %s", userId));
                Log.d(TAG, String.format("MOCK KAFKA: Data: %s", jsonData));

                // Simulăm headers
                String dataType = healthData.getClass().getSimpleName();
                String priority = dataType.contains("SensorData") ? "HIGH" : "NORMAL";
                Log.d(TAG, String.format("MOCK KAFKA: Headers - data-type: %s, priority: %s", dataType, priority));

                // Simulăm o trimitere reușită
                Log.d(TAG, "MOCK KAFKA: Datele de sănătate au fost trimise cu succes");

                return true;
            } catch (Exception e) {
                Log.e(TAG, "MOCK KAFKA: Eroare la simularea trimiterii datelor de sănătate", e);
                return false;
            }
        });
    }

    public void close() {
        // Nu este nevoie să închidem nimic, doar logăm
        Log.d(TAG, "MOCK KAFKA: Închidere producer simulată");
    }
}