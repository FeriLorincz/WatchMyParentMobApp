package com.feri.watchmyparent.mobile.infrastructure.services;

import android.util.Log;
import com.feri.watchmyparent.mobile.infrastructure.kafka.RealHealthDataKafkaProducer;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;

// Monitorizează starea conexiunii Kafka și furnizează metrici
@Singleton
public class KafkaHealthCheckService {

    private static final String TAG = "KafkaHealthCheck";

    private final RealHealthDataKafkaProducer kafkaProducer;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    // Status tracking
    private boolean isKafkaHealthy = false;
    private LocalDateTime lastSuccessfulConnection = null;
    private LocalDateTime lastFailedConnection = null;
    private int consecutiveFailures = 0;
    private int totalChecks = 0;
    private int successfulChecks = 0;

    // Configuration
    private static final long HEALTH_CHECK_INTERVAL = 30; // 30 seconds
    private static final int MAX_CONSECUTIVE_FAILURES = 5;
    private static final int CONNECTION_TIMEOUT_SECONDS = 10;

    @Inject
    public KafkaHealthCheckService(RealHealthDataKafkaProducer kafkaProducer) {
        this.kafkaProducer = kafkaProducer;

        Log.d(TAG, "✅ KafkaHealthCheckService initialized");
        startHealthChecks();
    }

    // Pornește monitorizarea periodică a stării Kafka
    private void startHealthChecks() {
        Log.d(TAG, "🔄 Starting periodic Kafka health checks (interval: " + HEALTH_CHECK_INTERVAL + "s)");

        scheduler.scheduleWithFixedDelay(this::performHealthCheck,
                0, HEALTH_CHECK_INTERVAL, TimeUnit.SECONDS);
    }

    // Execută un health check pentru Kafka
    private void performHealthCheck() {
        CompletableFuture.runAsync(() -> {
            try {
                totalChecks++;
                boolean currentlyHealthy = kafkaProducer.healthCheck().join();

                updateHealthStatus(currentlyHealthy);

                if (currentlyHealthy) {
                    successfulChecks++;
                    consecutiveFailures = 0;
                    lastSuccessfulConnection = LocalDateTime.now();

                    if (!isKafkaHealthy) {
                        Log.i(TAG, "✅ Kafka connection RESTORED after " + consecutiveFailures + " failures");
                    }

                } else {
                    consecutiveFailures++;
                    lastFailedConnection = LocalDateTime.now();

                    if (isKafkaHealthy) {
                        Log.w(TAG, "⚠️ Kafka connection LOST - starting failure count");
                    }

                    Log.w(TAG, "❌ Kafka health check failed (consecutive: " + consecutiveFailures + ")");
                }

                isKafkaHealthy = currentlyHealthy;

                // Log periodic status
                if (totalChecks % 10 == 0) { // Every 5 minutes
                    logHealthStatistics();
                }

            } catch (Exception e) {
                consecutiveFailures++;
                lastFailedConnection = LocalDateTime.now();
                isKafkaHealthy = false;
                Log.e(TAG, "❌ Health check exception (consecutive: " + consecutiveFailures + ")", e);
            }
        });
    }

    //Actualizează statusul de sănătate
    private void updateHealthStatus(boolean healthy) {
        // Kafka e considerat sănătos dacă:
        // 1. Ultima verificare a fost cu succes, SAU
        // 2. Nu avem mai mult de MAX_CONSECUTIVE_FAILURES eșecuri consecutive
        boolean wasHealthy = isKafkaHealthy;
        isKafkaHealthy = healthy && (consecutiveFailures < MAX_CONSECUTIVE_FAILURES);

        if (wasHealthy && !isKafkaHealthy) {
            Log.e(TAG, "💔 Kafka marked as UNHEALTHY after " + consecutiveFailures + " consecutive failures");
        } else if (!wasHealthy && isKafkaHealthy) {
            Log.i(TAG, "💚 Kafka marked as HEALTHY");
        }
    }

    // Verifică dacă Kafka este sănătos
    public boolean isKafkaHealthy() {
        return isKafkaHealthy;
    }

    //Obține statusul detaliat al conexiunii Kafka
    public CompletableFuture<KafkaHealthStatus> getDetailedHealthStatus() {
        return CompletableFuture.supplyAsync(() -> {
            KafkaHealthStatus status = new KafkaHealthStatus();
            status.isHealthy = isKafkaHealthy;
            status.consecutiveFailures = consecutiveFailures;
            status.lastSuccessfulConnection = lastSuccessfulConnection;
            status.lastFailedConnection = lastFailedConnection;
            status.totalChecks = totalChecks;
            status.successfulChecks = successfulChecks;

            // Calculează uptime percentage
            if (totalChecks > 0) {
                status.uptimePercentage = (double) successfulChecks / totalChecks * 100.0;
            }

            // Calculează timpul de la ultima conexiune cu succes
            if (lastSuccessfulConnection != null) {
                status.minutesSinceLastSuccess = ChronoUnit.MINUTES.between(
                        lastSuccessfulConnection, LocalDateTime.now());
            } else {
                status.minutesSinceLastSuccess = -1; // Never connected
            }

            return status;
        });
    }

    // Execută un health check la cerere
    public CompletableFuture<Boolean> performManualHealthCheck() {
        return CompletableFuture.supplyAsync(() -> {
            Log.d(TAG, "🔍 Performing manual Kafka health check...");

            try {
                boolean healthy = kafkaProducer.healthCheck().join();

                Log.d(TAG, "🔍 Manual health check result: " + (healthy ? "✅ HEALTHY" : "❌ UNHEALTHY"));
                return healthy;

            } catch (Exception e) {
                Log.e(TAG, "❌ Manual health check failed", e);
                return false;
            }
        });
    }

    // Resetează statisticile de sănătate
    public void resetHealthStatistics() {
        totalChecks = 0;
        successfulChecks = 0;
        consecutiveFailures = 0;
        lastSuccessfulConnection = null;
        lastFailedConnection = null;

        Log.d(TAG, "🔄 Health statistics reset");
    }

    // Logează statisticile de sănătate
    private void logHealthStatistics() {
        double uptimePercentage = totalChecks > 0 ? (double) successfulChecks / totalChecks * 100.0 : 0.0;

        Log.i(TAG, "📊 Kafka Health Statistics:");
        Log.i(TAG, "   Status: " + (isKafkaHealthy ? "✅ HEALTHY" : "❌ UNHEALTHY"));
        Log.i(TAG, "   Uptime: " + String.format("%.2f", uptimePercentage) + "% (" +
                successfulChecks + "/" + totalChecks + ")");
        Log.i(TAG, "   Consecutive Failures: " + consecutiveFailures);

        if (lastSuccessfulConnection != null) {
            long minutesAgo = ChronoUnit.MINUTES.between(lastSuccessfulConnection, LocalDateTime.now());
            Log.i(TAG, "   Last Success: " + minutesAgo + " minutes ago");
        }
    }

    // Oprește serviciul de health check
    public void shutdown() {
        Log.d(TAG, "🛑 Shutting down Kafka health check service...");

        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            Log.d(TAG, "✅ Kafka health check service shut down");
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Log.w(TAG, "⚠️ Health check service shutdown interrupted", e);
        }
    }

    // Clasa pentru statusul detaliat Kafka
    public static class KafkaHealthStatus {
        public boolean isHealthy = false;
        public int consecutiveFailures = 0;
        public LocalDateTime lastSuccessfulConnection = null;
        public LocalDateTime lastFailedConnection = null;
        public int totalChecks = 0;
        public int successfulChecks = 0;
        public double uptimePercentage = 0.0;
        public long minutesSinceLastSuccess = -1;

        public String getSummary() {
            return String.format(
                    "Kafka Health: %s | Uptime: %.1f%% | Consecutive Failures: %d | Last Success: %s",
                    isHealthy ? "✅" : "❌",
                    uptimePercentage,
                    consecutiveFailures,
                    minutesSinceLastSuccess >= 0 ? minutesSinceLastSuccess + "m ago" : "Never"
            );
        }

        public boolean isInCriticalState() {
            return !isHealthy && consecutiveFailures >= MAX_CONSECUTIVE_FAILURES;
        }

        public boolean needsAttention() {
            return !isHealthy || consecutiveFailures >= 3 || uptimePercentage < 95.0;
        }
    }
}