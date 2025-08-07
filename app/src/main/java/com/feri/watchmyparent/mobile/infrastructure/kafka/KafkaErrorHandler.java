package com.feri.watchmyparent.mobile.infrastructure.kafka;

import android.util.Log;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class KafkaErrorHandler {

    private final RealHealthDataKafkaProducer producer;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long INITIAL_RETRY_DELAY = 1000; // 1 second

    public KafkaErrorHandler(RealHealthDataKafkaProducer producer) {
        this.producer = producer;
    }

    public CompletableFuture<Boolean> handleFailedMessage(Object healthData, String userId, int attemptCount) {
        if (attemptCount >= MAX_RETRY_ATTEMPTS) {
            Log.e("KafkaErrorHandler", "Max retry attempts reached for message. Moving to dead letter queue.");
            return handleDeadLetter(healthData, userId);
        }

        long delay = INITIAL_RETRY_DELAY * (long) Math.pow(2, attemptCount); // Exponential backoff

        CompletableFuture<Boolean> future = new CompletableFuture<>();

        scheduler.schedule(() -> {
            Log.d("KafkaErrorHandler", "Retrying message transmission, attempt " + (attemptCount + 1));
            producer.sendHealthData(healthData, userId)
                    .thenAccept(success -> {
                        if (success) {
                            future.complete(true);
                        } else {
                            handleFailedMessage(healthData, userId, attemptCount + 1)
                                    .thenAccept(future::complete);
                        }
                    })
                    .exceptionally(throwable -> {
                        Log.e("KafkaErrorHandler", "Error during retry attempt. Moving to dead letter queue.");
                        handleFailedMessage(healthData, userId, attemptCount + 1)
                                .thenAccept(future::complete);
                        return null;
                    });
        }, delay, TimeUnit.MILLISECONDS);

        return future;
    }

    private CompletableFuture<Boolean> handleDeadLetter(Object healthData, String userId) {
        // Store in local database for later retry or manual intervention

        Log.w("TAG", String.format("Storing failed message in dead letter queue for user %s", userId));

        // In a real implementation, you would store this in a local database
        // For MVP, we'll just log the failure
        return CompletableFuture.completedFuture(false);
    }

    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }
}
