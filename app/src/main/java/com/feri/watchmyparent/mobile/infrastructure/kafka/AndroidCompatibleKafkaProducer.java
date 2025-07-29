package com.feri.watchmyparent.mobile.infrastructure.kafka;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSerializer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

//Android-compatible Kafka Producer that doesn't rely on JMX

public class AndroidCompatibleKafkaProducer {

    private static final String TAG = "AndroidKafkaProducer";

    private final String bootstrapServers;
    private final Gson gson;
    private boolean isConnected = false;
    private final String topicName = "health-data-topic";

    public AndroidCompatibleKafkaProducer(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context)
                        -> context.serialize(src.toString()))
                .create();

        // Setăm calea pentru log-uri
        System.setProperty("kafka.logs.dir", "C:/Kafka/logs");

        testConnection();
    }

    private void testConnection() {
        CompletableFuture.runAsync(() -> {
            try {
                // Just check if the server is reachable
                String[] serverParts = bootstrapServers.split(":");
                if (serverParts.length != 2) {
                    Log.e(TAG, "Invalid bootstrap server format: " + bootstrapServers);
                    isConnected = false;
                    return;
                }

                String host = serverParts[0];
                int port = Integer.parseInt(serverParts[1]);

                java.net.Socket socket = new java.net.Socket();
                socket.connect(new java.net.InetSocketAddress(host, port), 5000);
                socket.close();

                isConnected = true;
                Log.d(TAG, "✅ Successfully connected to Kafka server: " + bootstrapServers);
            } catch (Exception e) {
                isConnected = false;
                Log.e(TAG, "❌ Failed to connect to Kafka server: " + e.getMessage());
            }
        });
    }

    public CompletableFuture<Boolean> sendHealthData(Object healthData, String userId) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isConnected) {
                Log.w(TAG, "⚠️ Not connected to Kafka server, attempting reconnect...");
                testConnection();
                if (!isConnected) {
                    Log.e(TAG, "❌ Cannot send data - Kafka not available");
                    return false;
                }
            }

            try {
                String jsonData = gson.toJson(healthData);
                String messageKey = userId + "_" + System.currentTimeMillis();

                // Extract host and port from bootstrap servers
                String[] serverParts = bootstrapServers.split(":");
                if (serverParts.length != 2) {
                    Log.e(TAG, "Invalid bootstrap server format: " + bootstrapServers);
                    return false;
                }

                String host = serverParts[0];
                int port = Integer.parseInt(serverParts[1]);

                // Folosim HTTP pentru a trimite către Kafka REST Proxy dacă este disponibil
                // Altfel revenim la abordarea cu socket
                boolean sent = false;
                try {
                    sent = sendViaRESTProxy(host, jsonData, messageKey);
                } catch (Exception e) {
                    Log.w(TAG, "⚠️ REST Proxy not available, falling back to direct socket");
                    sent = sendViaSocket(host, port, jsonData, messageKey);
                }

                if (sent) {
                    Log.d(TAG, "📤 Sent data to Kafka: " + topicName);
                    Log.d(TAG, "📤 Message key: " + messageKey);
                    Log.d(TAG, "📤 Data size: " + jsonData.length() + " bytes");
                }

                return sent;
            } catch (Exception e) {
                Log.e(TAG, "❌ Error sending health data: " + e.getMessage(), e);
                return false;
            }
        });
    }

    private boolean sendViaRESTProxy(String host, String jsonData, String messageKey) throws IOException {
        URL url = new URL("http://" + host + ":8082/topics/" + topicName);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/vnd.kafka.json.v2+json");
        connection.setDoOutput(true);

        String requestBody = "{\"records\":[{\"key\":\"" + messageKey + "\",\"value\":" + jsonData + "}]}";

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()))) {
            writer.write(requestBody);
        }

        int responseCode = connection.getResponseCode();
        return responseCode >= 200 && responseCode < 300;
    }

    private boolean sendViaSocket(String host, int port, String jsonData, String messageKey) {
        try {
            // Socket approach
            java.net.Socket socket = new java.net.Socket(host, port);

            // Construim un mesaj pentru Kafka
            String message = messageKey + ":" + jsonData + "\n";

            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {
                writer.write(message);
                writer.flush();
            }

            socket.close();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Socket error: " + e.getMessage(), e);
            return false;
        }
    }


    public boolean isConnected() {
        return isConnected;
    }

    public CompletableFuture<Boolean> healthCheck() {
        return CompletableFuture.supplyAsync(() -> {
            testConnection();
            return isConnected;
        });
    }

    public void close() {
        // Nothing to close in this implementation
        Log.d(TAG, "Producer closed");
    }
}
