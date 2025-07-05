package com.feri.watchmyparent.mobile.infrastructure.kafka;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSerializer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import timber.log.Timber;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

public class HealthDataKafkaProducer {

    private final KafkaProducer<String, String> producer;
    private final Gson gson;
    private final String topicName;

    public HealthDataKafkaProducer() {
        this.producer = new KafkaProducer<>(KafkaProducerConfig.getProducerProperties());
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context)
                        -> context.serialize(src.toString()))
                .create();
        this.topicName = KafkaProducerConfig.getHealthDataTopic();
    }

    public CompletableFuture<Boolean> sendHealthData(Object healthData, String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String jsonData = gson.toJson(healthData);
                ProducerRecord<String, String> record = new ProducerRecord<>(
                        topicName,
                        userId, // Use userId as partition key
                        jsonData
                );

                // Add headers for message priority based on data type
                addMessageHeaders(record, healthData);

                producer.send(record, (RecordMetadata metadata, Exception exception) -> {
                    if (exception == null) {
                        Timber.d("Health data sent successfully to topic %s, partition %d, offset %d",
                                metadata.topic(), metadata.partition(), metadata.offset());
                    } else {
                        Timber.e(exception, "Failed to send health data to Kafka");
                    }
                });

                return true;

            } catch (Exception e) {
                Timber.e(e, "Error sending health data to Kafka");
                return false;
            }
        });
    }

    private void addMessageHeaders(ProducerRecord<String, String> record, Object healthData) {
        // Add priority headers based on data type
        String dataType = healthData.getClass().getSimpleName();
        record.headers().add("data-type", dataType.getBytes());
        record.headers().add("timestamp", String.valueOf(System.currentTimeMillis()).getBytes());

        // Add priority based on criticality
        if (dataType.contains("SensorData")) {
            record.headers().add("priority", "HIGH".getBytes());
        } else {
            record.headers().add("priority", "NORMAL".getBytes());
        }
    }

    public void close() {
        if (producer != null) {
            producer.close();
            Timber.d("Kafka producer closed");
        }
    }
}
