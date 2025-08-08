package com.feri.watchmyparent.mobile.application.services;

import android.util.Log;
import com.feri.watchmyparent.mobile.application.dto.SensorDataDTO;
import com.feri.watchmyparent.mobile.application.interfaces.DataTransmissionService;
import com.feri.watchmyparent.mobile.infrastructure.kafka.RealHealthDataKafkaProducer;
import com.feri.watchmyparent.mobile.infrastructure.services.KafkaHealthCheckService;
import com.feri.watchmyparent.mobile.infrastructure.services.KafkaRetryService;
import com.feri.watchmyparent.mobile.infrastructure.services.OfflineDataManager;
import com.feri.watchmyparent.mobile.infrastructure.services.NetworkStateManager;
import java.util.concurrent.CompletableFuture;
import java.util.Map;
import java.util.HashMap;
import javax.inject.Inject;
import javax.inject.Singleton;

//Implementarea completă a DataTransmissionService pentru pipeline Kafka-only
// Orchestrează transmiterea datelor cu retry logic și buffering offline
@Singleton
public class DataTransmissionServiceImpl implements DataTransmissionService {

    private static final String TAG = "DataTransmissionService";

    private final RealHealthDataKafkaProducer kafkaProducer;
    private final KafkaHealthCheckService kafkaHealthService;
    private final KafkaRetryService retryService;
    private final OfflineDataManager offlineDataManager;
    private final NetworkStateManager networkStateManager;

    // Statistics
    private int totalTransmissions = 0;
    private int successfulTransmissions = 0;
    private int failedTransmissions = 0;
    private int offlineTransmissions = 0;

    @Inject
    public DataTransmissionServiceImpl(
            RealHealthDataKafkaProducer kafkaProducer,
            KafkaHealthCheckService kafkaHealthService,
            KafkaRetryService retryService,
            OfflineDataManager offlineDataManager,
            NetworkStateManager networkStateManager) {

        this.kafkaProducer = kafkaProducer;
        this.kafkaHealthService = kafkaHealthService;
        this.retryService = retryService;
        this.offlineDataManager = offlineDataManager;
        this.networkStateManager = networkStateManager;

        Log.d(TAG, "✅ DataTransmissionService initialized with Kafka-only pipeline");
    }

    @Override
    public CompletableFuture<Boolean> transmitData(Object data, String userId) {
        return CompletableFuture.supplyAsync(() -> {
            totalTransmissions++;

            try {
                // Validează input-ul
                if (data == null || userId == null || userId.isEmpty()) {
                    Log.e(TAG, "❌ Invalid input: data=" + data + ", userId=" + userId);
                    failedTransmissions++;
                    return false;
                }

                Log.d(TAG, "📤 Transmitting data for user: " + userId +
                        " (type: " + data.getClass().getSimpleName() + ")");

                // Verifică starea rețelei
                if (!networkStateManager.isNetworkAvailable()) {
                    Log.w(TAG, "🌐 No network - storing data offline");
                    return handleOfflineTransmission(data, userId);
                }

                // Verifică starea Kafka
                if (!kafkaHealthService.isKafkaHealthy()) {
                    Log.w(TAG, "⚠️ Kafka unhealthy - attempting retry or offline storage");
                    return handleUnhealthyKafka(data, userId);
                }

                // Transmite prin Kafka
                return transmitToKafka(data, userId);

            } catch (Exception e) {
                failedTransmissions++;
                Log.e(TAG, "❌ Error during data transmission for user " + userId, e);

                // Încearcă să salveze offline ca fallback
                return handleOfflineTransmission(data, userId);
            }
        });
    }

    //Transmite datele direct către Kafka
    private boolean transmitToKafka(Object data, String userId) {
        try {
            // Convertește în mesaj Kafka
            Map<String, Object> kafkaMessage = convertToKafkaMessage(data, userId);

            // Trimite prin Kafka
            boolean sent = kafkaProducer.sendHealthData(kafkaMessage, userId).join();

            if (sent) {
                successfulTransmissions++;
                Log.d(TAG, "✅ Successfully transmitted data to Kafka for user: " + userId);
                return true;
            } else {
                failedTransmissions++;
                Log.w(TAG, "❌ Failed to transmit data to Kafka for user: " + userId);

                // Încearcă retry sau salvare offline
                return handleFailedKafkaTransmission(data, userId);
            }

        } catch (Exception e) {
            failedTransmissions++;
            Log.e(TAG, "❌ Exception during Kafka transmission for user " + userId, e);
            return handleFailedKafkaTransmission(data, userId);
        }
    }

    // Gestionează transmisia eșuată către Kafka
    private boolean handleFailedKafkaTransmission(Object data, String userId) {
        if (data instanceof SensorDataDTO) {
            SensorDataDTO sensorData = (SensorDataDTO) data;

            // Încearcă retry logic
            try {
                boolean retrySuccess = retryService.retryTransmission(sensorData).join();
                if (retrySuccess) {
                    Log.d(TAG, "✅ Retry successful for " + sensorData.getSensorType());
                    return true;
                }
            } catch (Exception e) {
                Log.e(TAG, "❌ Retry failed for " + sensorData.getSensorType(), e);
            }
        }

        // Dacă retry-ul eșuează, salvează offline
        return handleOfflineTransmission(data, userId);
    }

    //Gestionează transmisia când Kafka nu e sănătos
    private boolean handleUnhealthyKafka(Object data, String userId) {
        // Încearcă o singură dată transmisia directă (poate Kafka să fie iar funcțional)
        try {
            Map<String, Object> kafkaMessage = convertToKafkaMessage(data, userId);
            boolean sent = kafkaProducer.sendHealthData(kafkaMessage, userId).join();

            if (sent) {
                successfulTransmissions++;
                Log.d(TAG, "✅ Direct transmission successful despite unhealthy Kafka status");
                return true;
            }
        } catch (Exception e) {
            Log.d(TAG, "📝 Direct transmission failed as expected - storing offline");
        }

        // Salvează offline pentru procesare ulterioară
        return handleOfflineTransmission(data, userId);
    }

    // Gestionează stocarea offline
    private boolean handleOfflineTransmission(Object data, String userId) {
        try {
            if (data instanceof SensorDataDTO) {
                SensorDataDTO sensorData = (SensorDataDTO) data;
                boolean stored = offlineDataManager.storeOfflineData(sensorData).join();

                if (stored) {
                    offlineTransmissions++;
                    Log.d(TAG, "💾 Data stored offline for user: " + userId +
                            " (sensor: " + sensorData.getSensorType() + ")");
                    return true;
                } else {
                    Log.e(TAG, "❌ Failed to store data offline for user: " + userId);
                    return false;
                }
            } else {
                Log.w(TAG, "⚠️ Cannot store non-SensorDataDTO offline: " + data.getClass().getSimpleName());
                return false;
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Error storing data offline for user " + userId, e);
            return false;
        }
    }

    // Convertește data în mesaj Kafka
    private Map<String, Object> convertToKafkaMessage(Object data, String userId) {
        Map<String, Object> message = new HashMap<>();

        if (data instanceof SensorDataDTO) {
            SensorDataDTO sensorData = (SensorDataDTO) data;

            message.put("userId", userId);
            message.put("sensorType", sensorData.getSensorType().getCode());
            message.put("value", sensorData.getValue());
            message.put("unit", sensorData.getUnit());
            message.put("timestamp", sensorData.getTimestamp().toString());
            message.put("deviceId", sensorData.getDeviceId());
            message.put("source", "samsung_galaxy_watch_7");
            message.put("dataType", "REAL_SENSOR_DATA");
            message.put("criticalityLevel", sensorData.getSensorType().getCriticalityLevel().name());
            message.put("transmissionMethod", "kafka_only_pipeline");

            if (sensorData.getSensorType().isSamsungHealthPermitted()) {
                message.put("dataSource", "samsung_health_sdk");
            } else {
                message.put("dataSource", "android_sensor_api");
            }

        } else if (data instanceof Map) {
            // Dacă data e deja un Map (pentru alte tipuri de date)
            Map<String, Object> dataMap = (Map<String, Object>) data;
            message.putAll(dataMap);
            message.put("userId", userId);
            message.put("transmissionMethod", "kafka_only_pipeline");

        } else {
            // Generic object conversion
            message.put("userId", userId);
            message.put("data", data);
            message.put("dataType", "GENERIC_DATA");
            message.put("timestamp", java.time.LocalDateTime.now().toString());
            message.put("transmissionMethod", "kafka_only_pipeline");
        }

        return message;
    }

    @Override
    public CompletableFuture<Boolean> retryFailedTransmissions(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "🔄 Retrying failed transmissions for user: " + userId);

                // Obține datele offline pentru utilizator
                return offlineDataManager.getOfflineData()
                        .thenCompose(offlineData -> {
                            // Filtrează datele pentru utilizatorul specificat
                            java.util.List<OfflineDataManager.OfflineHealthData> userOfflineData =
                                    offlineData.stream()
                                            .filter(data -> userId.equals(data.userId))
                                            .collect(java.util.stream.Collectors.toList());

                            if (userOfflineData.isEmpty()) {
                                Log.d(TAG, "📋 No offline data to retry for user: " + userId);
                                return CompletableFuture.completedFuture(true);
                            }

                            Log.d(TAG, "📤 Retrying " + userOfflineData.size() + " offline records for user: " + userId);

                            // Procesează fiecare înregistrare offline
                            java.util.List<CompletableFuture<Boolean>> retryFutures = new java.util.ArrayList<>();

                            for (OfflineDataManager.OfflineHealthData offlineRecord : userOfflineData) {
                                // Convertește în SensorDataDTO
                                SensorDataDTO sensorData = convertOfflineToDTO(offlineRecord);

                                // Adaugă la lista de retry-uri
                                CompletableFuture<Boolean> retryFuture = transmitData(sensorData, userId);
                                retryFutures.add(retryFuture);
                            }

                            // Așteaptă completarea tuturor retry-urilor
                            return CompletableFuture.allOf(retryFutures.toArray(new CompletableFuture[0]))
                                    .thenApply(ignored -> {
                                        long successfulRetries = retryFutures.stream()
                                                .mapToLong(future -> future.join() ? 1 : 0)
                                                .sum();

                                        Log.d(TAG, "✅ Retry completed for user " + userId + ": " +
                                                successfulRetries + "/" + retryFutures.size() + " successful");

                                        return successfulRetries > 0;
                                    });
                        }).join();

            } catch (Exception e) {
                Log.e(TAG, "❌ Error retrying failed transmissions for user " + userId, e);
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<Integer> getPendingTransmissionCount(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return offlineDataManager.getOfflineData()
                        .thenApply(offlineData -> {
                            long count = offlineData.stream()
                                    .filter(data -> userId.equals(data.userId))
                                    .count();

                            Log.d(TAG, "📊 Pending transmissions for user " + userId + ": " + count);
                            return (int) count;
                        }).join();

            } catch (Exception e) {
                Log.e(TAG, "❌ Error getting pending transmission count for user " + userId, e);
                return 0;
            }
        });
    }

    // Convertește OfflineHealthData în SensorDataDTO
    private SensorDataDTO convertOfflineToDTO(OfflineDataManager.OfflineHealthData offlineData) {
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

    // ✅ Obține statisticile de transmisie
    public TransmissionStatistics getTransmissionStatistics() {
        TransmissionStatistics stats = new TransmissionStatistics();
        stats.totalTransmissions = totalTransmissions;
        stats.successfulTransmissions = successfulTransmissions;
        stats.failedTransmissions = failedTransmissions;
        stats.offlineTransmissions = offlineTransmissions;

        if (totalTransmissions > 0) {
            stats.successRate = (double) successfulTransmissions / totalTransmissions * 100.0;
            stats.offlineRate = (double) offlineTransmissions / totalTransmissions * 100.0;
        }

        return stats;
    }

    // Obține statusul complet al serviciului
    public CompletableFuture<ServiceStatus> getServiceStatus() {
        return CompletableFuture.supplyAsync(() -> {
            ServiceStatus status = new ServiceStatus();

            // Status general
            status.isKafkaHealthy = kafkaHealthService.isKafkaHealthy();
            status.isNetworkAvailable = networkStateManager.isNetworkAvailable();
            status.networkType = networkStateManager.getCurrentNetworkType();

            // Statistici
            status.transmissionStats = getTransmissionStatistics();
            status.retryStats = retryService.getRetryStatistics();

            // Statusuri componente
            try {
                status.kafkaHealthDetails = kafkaHealthService.getDetailedHealthStatus().join();
                status.networkDetails = networkStateManager.getDetailedNetworkStatus();
                status.offlineStats = offlineDataManager.getOfflineStatistics().join();
            } catch (Exception e) {
                Log.e(TAG, "❌ Error getting detailed service status", e);
            }

            return status;
        });
    }

    // Resetează toate statisticil
    public void resetStatistics() {
        totalTransmissions = 0;
        successfulTransmissions = 0;
        failedTransmissions = 0;
        offlineTransmissions = 0;

        retryService.resetStatistics();
        kafkaHealthService.resetHealthStatistics();

        Log.d(TAG, "🔄 All transmission statistics reset");
    }

    // Statistici de transmisie
    public static class TransmissionStatistics {
        public int totalTransmissions = 0;
        public int successfulTransmissions = 0;
        public int failedTransmissions = 0;
        public int offlineTransmissions = 0;
        public double successRate = 0.0;
        public double offlineRate = 0.0;

        @Override
        public String toString() {
            return String.format(
                    "Transmission Stats: Total=%d, Success=%d (%.1f%%), Failed=%d, Offline=%d (%.1f%%)",
                    totalTransmissions, successfulTransmissions, successRate,
                    failedTransmissions, offlineTransmissions, offlineRate
            );
        }
    }

    // Status complet al serviciului
    public static class ServiceStatus {
        public boolean isKafkaHealthy = false;
        public boolean isNetworkAvailable = false;
        public String networkType = "Unknown";
        public TransmissionStatistics transmissionStats;
        public KafkaRetryService.RetryStatistics retryStats;
        public KafkaHealthCheckService.KafkaHealthStatus kafkaHealthDetails;
        public NetworkStateManager.NetworkStatus networkDetails;
        public OfflineDataManager.OfflineStatistics offlineStats;

        public String getSummary() {
            return String.format(
                    "DataTransmissionService: Kafka=%s, Network=%s %s, Success=%.1f%%",
                    isKafkaHealthy ? "✅" : "❌",
                    isNetworkAvailable ? "✅" : "❌",
                    networkType,
                    transmissionStats != null ? transmissionStats.successRate : 0.0
            );
        }

        public boolean isHealthy() {
            return isKafkaHealthy && isNetworkAvailable &&
                    (transmissionStats == null || transmissionStats.successRate > 80.0);
        }
    }
}