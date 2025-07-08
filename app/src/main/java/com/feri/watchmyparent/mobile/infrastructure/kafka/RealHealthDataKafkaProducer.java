package com.feri.watchmyparent.mobile.infrastructure.kafka;

import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSerializer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;

import java.time.LocalDateTime;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class RealHealthDataKafkaProducer {

    private static final String TAG = "RealKafkaProducer";

    // ✅ REAL Kafka Configuration
    //private static final String BOOTSTRAP_SERVERS = "10.0.2.2:9092"; // localhost pentru emulator
    private static final String BOOTSTRAP_SERVERS_DEBUG = "192.168.0.91:9092"; // Your computer IP
    private static final String BOOTSTRAP_SERVERS_PROD = "kafka.watchmyparent.com:9092"; // Production
    private static final String HEALTH_DATA_TOPIC = "health-data-topic";
    private static final String LOCATION_DATA_TOPIC = "location-data-topic";

    private final KafkaProducer<String, String> producer;
    private final Gson gson;
    private volatile boolean isConnected = false;
    private final String bootstrapServers;

    public RealHealthDataKafkaProducer() {
        // Choose bootstrap servers based on build configuration
        this.bootstrapServers = com.feri.watchmyparent.mobile.BuildConfig.DEBUG
                ? BOOTSTRAP_SERVERS_DEBUG
                : BOOTSTRAP_SERVERS_PROD;

        Log.d(TAG, "✅ Initializing REAL Kafka Producer");
        Log.d(TAG, "🔗 Bootstrap servers: " + bootstrapServers);

        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context)
                        -> context.serialize(src.toString()))
                .create();

        this.producer = createKafkaProducer();
        testConnection();
    }

    private KafkaProducer<String, String> createKafkaProducer() {
        Properties props = new Properties();

        // ✅ Real Kafka Producer Configuration with dynamic IP
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // Performance settings
        props.put(ProducerConfig.ACKS_CONFIG, "all"); // Wait for all replicas
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 1);
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);

        // Reliability settings
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);

        // Timeout settings for mobile networks
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000);

        try {
            KafkaProducer<String, String> kafkaProducer = new KafkaProducer<>(props);
            Log.d(TAG, "✅ Real Kafka Producer created successfully with servers: " + bootstrapServers);
            return kafkaProducer;
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to create Real Kafka Producer", e);
            throw new RuntimeException("Failed to initialize Real Kafka Producer", e);
        }
    }

    private void testConnection() {
        CompletableFuture.runAsync(() -> {
            try {
                // Test connection by sending a test message
                ProducerRecord<String, String> testRecord = new ProducerRecord<>(
                        HEALTH_DATA_TOPIC,
                        "test",
                        "{\"type\":\"connection_test\",\"timestamp\":\"" + LocalDateTime.now() + "\"}"
                );

                Future<RecordMetadata> future = producer.send(testRecord);
                RecordMetadata metadata = future.get(); // This will throw if connection fails

                isConnected = true;
                Log.d(TAG, "✅ Kafka connection test successful - Topic: " + metadata.topic() +
                        ", Partition: " + metadata.partition() + ", Offset: " + metadata.offset());

            } catch (Exception e) {
                isConnected = false;
                Log.e(TAG, "❌ Kafka connection test failed", e);
            }
        });
    }

    public CompletableFuture<Boolean> sendHealthData(Object healthData, String userId) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isConnected) {
                Log.w(TAG, "⚠️ Kafka not connected, attempting to reconnect...");
                testConnection();
                if (!isConnected) {
                    Log.e(TAG, "❌ Cannot send data - Kafka not available");
                    return false;
                }
            }

            try {
                String jsonData = gson.toJson(healthData);
                String messageKey = userId + "_" + System.currentTimeMillis();

                ProducerRecord<String, String> record = new ProducerRecord<>(
                        HEALTH_DATA_TOPIC,
                        messageKey,
                        jsonData
                );

                // Add headers for better message processing
                addMessageHeaders(record, healthData, userId);

                // ✅ Send to REAL Kafka
                Future<RecordMetadata> future = producer.send(record, (metadata, exception) -> {
                    if (exception == null) {
                        Log.d(TAG, String.format("✅ Health data sent successfully - Topic: %s, Partition: %d, Offset: %d",
                                metadata.topic(), metadata.partition(), metadata.offset()));
                    } else {
                        Log.e(TAG, "❌ Failed to send health data to Kafka", exception);
                    }
                });

                // Wait for send completion (with timeout)
                RecordMetadata metadata = future.get();

                Log.d(TAG, String.format("📊 REAL DATA SENT - User: %s, Topic: %s, Size: %d bytes",
                        userId, metadata.topic(), jsonData.length()));

                return true;

            } catch (Exception e) {
                Log.e(TAG, "❌ Error sending health data to real Kafka", e);
                isConnected = false; // Mark as disconnected to trigger reconnection
                return false;
            }
        });
    }

    public CompletableFuture<Boolean> sendLocationData(Object locationData, String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String jsonData = gson.toJson(locationData);
                String messageKey = userId + "_location_" + System.currentTimeMillis();

                ProducerRecord<String, String> record = new ProducerRecord<>(
                        LOCATION_DATA_TOPIC,
                        messageKey,
                        jsonData
                );

                addMessageHeaders(record, locationData, userId);

                Future<RecordMetadata> future = producer.send(record);
                RecordMetadata metadata = future.get();

                Log.d(TAG, String.format("📍 REAL LOCATION DATA SENT - User: %s, Offset: %d",
                        userId, metadata.offset()));

                return true;

            } catch (Exception e) {
                Log.e(TAG, "❌ Error sending location data to real Kafka", e);
                return false;
            }
        });
    }

    private void addMessageHeaders(ProducerRecord<String, String> record, Object data, String userId) {
        // Add metadata headers
        record.headers().add("user-id", userId.getBytes());
        record.headers().add("data-type", data.getClass().getSimpleName().getBytes());
        record.headers().add("timestamp", String.valueOf(System.currentTimeMillis()).getBytes());
        record.headers().add("source", "android-mobile-app".getBytes());
        record.headers().add("version", "1.0".getBytes());

        // Add priority based on data type
        String priority = data.getClass().getSimpleName().contains("SensorData") ? "HIGH" : "NORMAL";
        record.headers().add("priority", priority.getBytes());
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void close() {
        if (producer != null) {
            try {
                producer.flush(); // Ensure all messages are sent
                producer.close();
                Log.d(TAG, "✅ Real Kafka producer closed successfully");
            } catch (Exception e) {
                Log.e(TAG, "❌ Error closing Kafka producer", e);
            }
        }
        isConnected = false;
    }

    // Health check method
    public CompletableFuture<Boolean> healthCheck() {
        return CompletableFuture.supplyAsync(() -> {
            testConnection();
            return isConnected;
        });
    }
}
