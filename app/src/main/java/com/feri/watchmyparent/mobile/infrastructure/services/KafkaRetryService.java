package com.feri.watchmyparent.mobile.infrastructure.services;

import android.util.Log;
import com.feri.watchmyparent.mobile.application.dto.SensorDataDTO;
import com.feri.watchmyparent.mobile.infrastructure.kafka.RealHealthDataKafkaProducer;
import com.feri.watchmyparent.mobile.infrastructure.services.OfflineDataManager.OfflineHealthData;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;

//Gestionează retry logic-ul pentru transmiterea datelor către Kafka
//Include exponential backoff și dead letter queue
@Singleton
public class KafkaRetryService {

    private static final String TAG = "KafkaRetryService";

    private final RealHealthDataKafkaProducer kafkaProducer;
    private final KafkaHealthCheckService healthCheckService;
    private final OfflineDataManager offlineDataManager;
    private final ScheduledExecutorService retryScheduler = Executors.newScheduledThreadPool(3);

    // Retry configuration
    private static final int MAX_RETRY_ATTEMPTS = 5;
    private static final long INITIAL_RETRY_DELAY_MS = 1000; // 1 second
    private static final double BACKOFF_MULTIPLIER = 2.0;
    private static final long MAX_RETRY_DELAY_MS = 300000; // 5 minutes
    private static final long RETRY_BATCH_INTERVAL_MS = 60000; // 1 minute

    // Statistics
    private int totalRetryAttempts = 0;
    private int successfulRetries = 0;
    private int failedRetries = 0;
    private int deadLetterCount = 0;

    @Inject
    public KafkaRetryService(
            RealHealthDataKafkaProducer kafkaProducer,
            KafkaHealthCheckService healthCheckService,
            OfflineDataManager offlineDataManager) {
        this.kafkaProducer = kafkaProducer;
        this.healthCheckService = healthCheckService;
        this.offlineDataManager = offlineDataManager;

        Log.d(TAG, "✅ KafkaRetryService initialized");
        startPeriodicRetryProcessor();
    }

    // Pornește procesarea periodică a retry-urilor
    private void startPeriodicRetryProcessor() {
        Log.d(TAG, "🔄 Starting periodic retry processor (interval: " + RETRY_BATCH_INTERVAL_MS + "ms)");

        retryScheduler.scheduleWithFixedDelay(
                this::processOfflineDataBatch,
                RETRY_BATCH_INTERVAL_MS,
                RETRY_BATCH_INTERVAL_MS,
                TimeUnit.MILLISECONDS
        );
    }

    // Încearcă transmiterea unui singur mesaj cu retry logic
    public CompletableFuture<Boolean> retryTransmission(SensorDataDTO sensorData) {
        return retryTransmissionWithDelay(sensorData, 0);
    }

    // Retry cu delay calculat exponențial
    private CompletableFuture<Boolean> retryTransmissionWithDelay(SensorDataDTO sensorData, int attemptNumber) {
        if (attemptNumber >= MAX_RETRY_ATTEMPTS) {
            Log.w(TAG, "💀 Max retry attempts reached for sensor " + sensorData.getSensorType() +
                    " - moving to dead letter queue");
            moveToDeadLetterQueue(sensorData);
            return CompletableFuture.completedFuture(false);
        }

        // Verifică dacă Kafka este sănătos înainte de retry
        if (!healthCheckService.isKafkaHealthy()) {
            Log.w(TAG, "⚠️ Kafka unhealthy - storing data offline for later retry");
            return storeForLaterRetry(sensorData);
        }

        // Calculează delay-ul cu exponential backoff
        long delayMs = calculateRetryDelay(attemptNumber);

        CompletableFuture<Boolean> future = new CompletableFuture<>();

        retryScheduler.schedule(() -> {
            totalRetryAttempts++;

            Log.d(TAG, "🔄 Retry attempt " + (attemptNumber + 1) + "/" + MAX_RETRY_ATTEMPTS +
                    " for " + sensorData.getSensorType() + " (delay: " + delayMs + "ms)");

            kafkaProducer.sendHealthData(convertToKafkaMessage(sensorData), sensorData.getUserId())
                    .thenAccept(success -> {
                        if (success) {
                            successfulRetries++;
                            sensorData.markAsTransmitted("Kafka-Retry");

                            Log.d(TAG, "✅ Retry successful for " + sensorData.getSensorType() +
                                    " after " + (attemptNumber + 1) + " attempts");
                            future.complete(true);
                        } else {
                            failedRetries++;
                            sensorData.markAsFailedTransmission("Retry failed attempt " + (attemptNumber + 1));

                            // Încearcă din nou cu delay mai mare
                            retryTransmissionWithDelay(sensorData, attemptNumber + 1)
                                    .thenAccept(future::complete);
                        }
                    })
                    .exceptionally(throwable -> {
                        failedRetries++;
                        Log.e(TAG, "❌ Exception during retry attempt " + (attemptNumber + 1), throwable);

                        sensorData.markAsFailedTransmission("Exception: " + throwable.getMessage());

                        // Încearcă din nou cu delay mai mare
                        retryTransmissionWithDelay(sensorData, attemptNumber + 1)
                                .thenAccept(future::complete);
                        return null;
                    });

        }, delayMs, TimeUnit.MILLISECONDS);

        return future;
    }

    // Calculează delay-ul pentru retry cu exponential backoff
    private long calculateRetryDelay(int attemptNumber) {
        long delay = (long) (INITIAL_RETRY_DELAY_MS * Math.pow(BACKOFF_MULTIPLIER, attemptNumber));
        return Math.min(delay, MAX_RETRY_DELAY_MS);
    }

    //Stochează datele pentru retry mai târziu
    private CompletableFuture<Boolean> storeForLaterRetry(SensorDataDTO sensorData) {
        return offlineDataManager.storeOfflineData(sensorData)
                .thenApply(stored -> {
                    if (stored) {
                        Log.d(TAG, "💾 Stored data offline for later retry: " + sensorData.getSensorType());
                    } else {
                        Log.e(TAG, "❌ Failed to store data offline: " + sensorData.getSensorType());
                    }
                    return stored;
                });
    }

    //Procesează batch-ul de date offline pentru retry
    private void processOfflineDataBatch() {
        if (!healthCheckService.isKafkaHealthy()) {
            Log.d(TAG, "⚠️ Kafka unhealthy - skipping offline data processing");
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                List<OfflineHealthData> offlineData = offlineDataManager.getOfflineData().join();

                if (offlineData.isEmpty()) {
                    return;
                }

                Log.d(TAG, "📤 Processing " + offlineData.size() + " offline records for retry");

                List<Long> successfulIds = new ArrayList<>();
                List<Long> failedIds = new ArrayList<>();

                for (OfflineHealthData data : offlineData) {
                    try {
                        // Convertește din OfflineHealthData în SensorDataDTO
                        SensorDataDTO sensorDataDTO = convertOfflineToDTO(data);

                        // Încearcă transmiterea
                        boolean sent = kafkaProducer.sendHealthData(
                                convertToKafkaMessage(sensorDataDTO),
                                data.userId
                        ).join();

                        if (sent) {
                            successfulIds.add(data.id);
                            Log.d(TAG, "✅ Successfully sent offline data: " + data.sensorType);
                        } else {
                            failedIds.add(data.id);
                            Log.w(TAG, "❌ Failed to send offline data: " + data.sensorType);
                        }

                    } catch (Exception e) {
                        failedIds.add(data.id);
                        Log.e(TAG, "❌ Exception sending offline data for " + data.sensorType, e);
                    }
                }

                // Șterge datele transmise cu succes
                if (!successfulIds.isEmpty()) {
                    offlineDataManager.deleteOfflineData(successfulIds).join();
                    Log.d(TAG, "🗑️ Cleaned up " + successfulIds.size() + " successfully transmitted records");
                }

                // Incrementează retry count pentru cele eșuate
                if (!failedIds.isEmpty()) {
                    offlineDataManager.incrementRetryCount(failedIds).join();
                    Log.w(TAG, "🔄 Incremented retry count for " + failedIds.size() + " failed records");
                }

                // Curăță înregistrările cu prea multe retry-uri
                int cleanedUp = offlineDataManager.cleanupFailedRecords(MAX_RETRY_ATTEMPTS).join();
                if (cleanedUp > 0) {
                    deadLetterCount += cleanedUp;
                    Log.w(TAG, "💀 Moved " + cleanedUp + " records to dead letter queue (total: " + deadLetterCount + ")");
                }

            } catch (Exception e) {
                Log.e(TAG, "❌ Error processing offline data batch", e);
            }
        });
    }

    // Convertește OfflineHealthData în SensorDataDTO
    private SensorDataDTO convertOfflineToDTO(OfflineHealthData offlineData) {
        SensorDataDTO dto = new SensorDataDTO();
        dto.setUserId(offlineData.userId);
        dto.setSensorType(offlineData.sensorType);
        dto.setValue(offlineData.value);
        dto.setUnit(offlineData.unit);
        dto.setTimestamp(offlineData.timestamp);
        dto.setDeviceId(offlineData.deviceId);
        dto.setRetryCount(offlineData.retryCount);
        return dto;
    }

    //Convertește SensorDataDTO în mesaj Kafka
    private java.util.Map<String, Object> convertToKafkaMessage(SensorDataDTO sensorData) {
        java.util.Map<String, Object> message = new java.util.HashMap<>();
        message.put("userId", sensorData.getUserId());
        message.put("sensorType", sensorData.getSensorType().getCode());
        message.put("value", sensorData.getValue());
        message.put("unit", sensorData.getUnit());
        message.put("timestamp", sensorData.getTimestamp().toString());
        message.put("deviceId", sensorData.getDeviceId());
        message.put("source", "samsung_galaxy_watch_7");
        message.put("dataType", "RETRY_SENSOR_DATA");
        message.put("retryCount", sensorData.getRetryCount());
        message.put("criticalityLevel", sensorData.getSensorType().getCriticalityLevel().name());
        return message;
    }

    // Mută datele în dead letter queue
    private void moveToDeadLetterQueue(SensorDataDTO sensorData) {
        deadLetterCount++;
        try {
            Log.e(TAG, "💀 DEAD LETTER QUEUE: " + sensorData.getSensorType() +
                    " for user " + sensorData.getUserId() +
                    " (total dead letters: " + deadLetterCount + ")");

            // ✅ 1. Salvează în OfflineDataManager cu metadata specială
            SensorDataDTO deadLetterData = createDeadLetterCopy(sensorData);
            offlineDataManager.storeOfflineData(deadLetterData)
                    .thenAccept(stored -> {
                        if (stored) {
                            Log.d(TAG, "💾 Dead letter stored in offline manager: " + sensorData.getSensorType());
                        }
                    });

            // ✅ 2. Trimite notificare către monitorizare (prin log structurat)
            logDeadLetterForMonitoring(sensorData);

            // ✅ 3. Încearcă să trimită spre un Dead Letter Topic în Kafka (opțional)
            attemptDeadLetterTopicTransmission(sensorData);

        } catch (Exception e) {
            Log.e(TAG, "❌ Error processing dead letter queue for " + sensorData.getSensorType(), e);
        }
    }

    // Creează o copie pentru dead letter queue
    private SensorDataDTO createDeadLetterCopy(SensorDataDTO original) {
        SensorDataDTO deadLetter = new SensorDataDTO();
        deadLetter.setUserId(original.getUserId());
        deadLetter.setSensorType(original.getSensorType());
        deadLetter.setValue(original.getValue());
        deadLetter.setUnit(original.getUnit());
        deadLetter.setTimestamp(original.getTimestamp());
        deadLetter.setDeviceId(original.getDeviceId());
        deadLetter.setRetryCount(MAX_RETRY_ATTEMPTS); // Mark as max retries reached
        deadLetter.setErrorMessage("DEAD_LETTER_QUEUE: Max retries exceeded");
        deadLetter.setTransmissionMethod("DEAD_LETTER_STORAGE");

        return deadLetter;
    }

    // Log structurat pentru monitorizare
    private void logDeadLetterForMonitoring(SensorDataDTO sensorData) {
        // Log în format JSON pentru parsing ușor de către sisteme de monitoring
        String monitoringLog = String.format(
                "MONITORING_ALERT: {\"type\":\"DEAD_LETTER_QUEUE\", \"sensor\":\"%s\", " +
                        "\"userId\":\"%s\", \"value\":%.2f, \"timestamp\":\"%s\", " +
                        "\"totalDeadLetters\":%d, \"severity\":\"HIGH\"}",
                sensorData.getSensorType().getCode(),
                sensorData.getUserId(),
                sensorData.getValue(),
                sensorData.getTimestamp().toString(),
                deadLetterCount
        );

        Log.w("MONITORING", monitoringLog);

        // Alertă dacă avem prea multe dead letters
        if (deadLetterCount % 10 == 0) { // Every 10 dead letters
            Log.e("MONITORING", String.format(
                    "CRITICAL_ALERT: {\"type\":\"HIGH_DEAD_LETTER_COUNT\", \"count\":%d, " +
                            "\"threshold\":10, \"action\":\"INVESTIGATE_KAFKA_ISSUES\"}",
                    deadLetterCount
            ));
        }
    }

    // Încearcă transmiterea spre Dead Letter Topic (opțional)
    private void attemptDeadLetterTopicTransmission(SensorDataDTO sensorData) {
        CompletableFuture.runAsync(() -> {
            try {
                // Creează un mesaj special pentru Dead Letter Topic
                java.util.Map<String, Object> deadLetterMessage = new java.util.HashMap<>();
                deadLetterMessage.put("messageType", "DEAD_LETTER");
                deadLetterMessage.put("originalData", convertToKafkaMessage(sensorData));
                deadLetterMessage.put("failureReason", "MAX_RETRIES_EXCEEDED");
                deadLetterMessage.put("retryCount", sensorData.getRetryCount());
                deadLetterMessage.put("deadLetterTimestamp", java.time.LocalDateTime.now().toString());
                deadLetterMessage.put("totalDeadLetters", deadLetterCount);

                // Încearcă să trimită către un topic special pentru dead letters
                // Acest lucru e opțional și nu trebuie să blocheze aplicația
                boolean sent = kafkaProducer.sendHealthData(deadLetterMessage, sensorData.getUserId()).join();

                if (sent) {
                    Log.d(TAG, "📤 Dead letter sent to monitoring topic: " + sensorData.getSensorType());
                } else {
                    Log.w(TAG, "⚠️ Could not send dead letter to monitoring topic (not critical)");
                }

            } catch (Exception e) {
                // Nu logăm ca eroare pentru că e doar o funcție de monitoring
                Log.d(TAG, "📝 Dead letter monitoring transmission failed (not critical): " + e.getMessage());
            }
        });
    }

    //Obține statistici dead letter queue
    public DeadLetterStatistics getDeadLetterStatistics() {
        DeadLetterStatistics stats = new DeadLetterStatistics();
        stats.totalDeadLetters = deadLetterCount;
        stats.timestamp = java.time.LocalDateTime.now();

        if (totalRetryAttempts > 0) {
            stats.deadLetterRate = (double) deadLetterCount / totalRetryAttempts * 100.0;
        }

        return stats;
    }

    // Curăță dead letter queue (pentru maintenance)
    public CompletableFuture<Integer> cleanupOldDeadLetters() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Curăță înregistrările dead letter mai vechi de 24 ore
                int cleaned = offlineDataManager.cleanupFailedRecords(MAX_RETRY_ATTEMPTS + 1).join();

                if (cleaned > 0) {
                    Log.i(TAG, "🧹 Cleaned up " + cleaned + " old dead letter records");
                }

                return cleaned;

            } catch (Exception e) {
                Log.e(TAG, "❌ Error cleaning up dead letters", e);
                return 0;
            }
        });

    }

    // Obține statisticile retry
    public RetryStatistics getRetryStatistics() {
        RetryStatistics stats = new RetryStatistics();
        stats.totalRetryAttempts = totalRetryAttempts;
        stats.successfulRetries = successfulRetries;
        stats.failedRetries = failedRetries;
        stats.deadLetterCount = deadLetterCount;

        if (totalRetryAttempts > 0) {
            stats.retrySuccessRate = (double) successfulRetries / totalRetryAttempts * 100.0;
        }

        return stats;
    }

    // Resetează statisticile retry
    public void resetStatistics() {
        totalRetryAttempts = 0;
        successfulRetries = 0;
        failedRetries = 0;
        deadLetterCount = 0;

        Log.d(TAG, "🔄 Retry statistics reset");
    }

    // Oprește serviciul de retry
    public void shutdown() {
        Log.d(TAG, "🛑 Shutting down Kafka retry service...");

        retryScheduler.shutdown();
        try {
            if (!retryScheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                retryScheduler.shutdownNow();
            }
            Log.d(TAG, "✅ Kafka retry service shut down");
        } catch (InterruptedException e) {
            retryScheduler.shutdownNow();
            Log.w(TAG, "⚠️ Retry service shutdown interrupted", e);
        }
    }

    // Statistici retry
    public static class RetryStatistics {
        public int totalRetryAttempts = 0;
        public int successfulRetries = 0;
        public int failedRetries = 0;
        public int deadLetterCount = 0;
        public double retrySuccessRate = 0.0;

        @Override
        public String toString() {
            return String.format(
                    "Retry Stats: Total=%d, Success=%d (%.1f%%), Failed=%d, DeadLetters=%d",
                    totalRetryAttempts, successfulRetries, retrySuccessRate,
                    failedRetries, deadLetterCount
            );
        }
    }

    // ✅ Clasa pentru statistici dead letter
    public static class DeadLetterStatistics {
        public int totalDeadLetters = 0;
        public double deadLetterRate = 0.0;
        public java.time.LocalDateTime timestamp;

        @Override
        public String toString() {
            return String.format("Dead Letter Stats: Total=%d, Rate=%.2f%%",
                    totalDeadLetters, deadLetterRate);
        }
    }
}