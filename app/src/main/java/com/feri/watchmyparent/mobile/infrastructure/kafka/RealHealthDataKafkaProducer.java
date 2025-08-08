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

        // Bootstrap servers configuration
        private static final String BOOTSTRAP_SERVERS_DEBUG = "192.168.0.91:9092";
        private static final String BOOTSTRAP_SERVERS_PROD = "kafka.watchmyparent.com:9092";
        private static final String HEALTH_DATA_TOPIC = "health-data-topic";
        private static final String LOCATION_DATA_TOPIC = "location-data-topic";

        // Use our custom Android-compatible implementation instead of KafkaProducer
        private final AndroidCompatibleKafkaProducer producer;
        private final Gson gson;
        private volatile boolean isConnected = false;
        private final String bootstrapServers;

        public RealHealthDataKafkaProducer() {
            this.bootstrapServers = com.feri.watchmyparent.mobile.BuildConfig.DEBUG
                    ? BOOTSTRAP_SERVERS_DEBUG
                    : BOOTSTRAP_SERVERS_PROD;

            Log.d(TAG, "✅ Initializing REAL Kafka Producer");
            Log.d(TAG, "🔗 Bootstrap servers: " + bootstrapServers);

            this.gson = new GsonBuilder()
                    .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context)
                            -> context.serialize(src.toString()))
                    .create();

            // Initialize our Android-compatible producer
            this.producer = new AndroidCompatibleKafkaProducer(bootstrapServers);
            testConnection();
        }

        private void testConnection() {
            CompletableFuture.runAsync(() -> {
                try {
                    isConnected = producer.isConnected();
                    if (isConnected) {
                        Log.d(TAG, "✅ Kafka connection test successful");
                    } else {
                        Log.w(TAG, "⚠️ Kafka connection test failed");
                    }
                } catch (Exception e) {
                    isConnected = false;
                    Log.e(TAG, "❌ Kafka connection test failed", e);
                }
            });
        }

        public CompletableFuture<Boolean> sendHealthData(Object healthData, String userId) {
            Log.d(TAG, "🔄 Attempting to send health data to Kafka for user: " + userId);
            Log.d(TAG, "📦 Data to send: " + gson.toJson(healthData));
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
                    // Use our compatible producer to send data
                    boolean success = producer.sendHealthData(healthData, userId).join();

                    if (success) {
                        Log.d(TAG, String.format("📊 REAL DATA SENT - User: %s, Topic: %s",
                                userId, HEALTH_DATA_TOPIC));
                    }

                    return success;
                } catch (Exception e) {
                    Log.e(TAG, "❌ Error sending health data to real Kafka", e);
                    isConnected = false; // Mark as disconnected to trigger reconnection
                    return false;
                }
            });
        }

        public CompletableFuture<Boolean> sendLocationData(Object locationData, String userId) {
            return sendHealthData(locationData, userId); // Reuse the same method for now
        }

        public boolean isConnected() {
            return isConnected;
        }

        public void close() {
            if (producer != null) {
                try {
                    producer.close();
                    Log.d(TAG, "✅ Real Kafka producer closed successfully");
                } catch (Exception e) {
                    Log.e(TAG, "❌ Error closing Kafka producer", e);
                }
            }
            isConnected = false;
        }

        public CompletableFuture<Boolean> healthCheck() {
            return CompletableFuture.supplyAsync(() -> {
                testConnection();
                return isConnected;
            });
        }
    }