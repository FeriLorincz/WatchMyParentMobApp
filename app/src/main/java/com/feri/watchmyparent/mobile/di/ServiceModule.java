package com.feri.watchmyparent.mobile.di;

import android.content.Context;

import com.feri.watchmyparent.mobile.application.interfaces.DataTransmissionService;
import com.feri.watchmyparent.mobile.application.services.*;
import com.feri.watchmyparent.mobile.domain.repositories.*;
import com.feri.watchmyparent.mobile.infrastructure.kafka.RealHealthDataKafkaProducer;
import com.feri.watchmyparent.mobile.infrastructure.services.*;
import com.feri.watchmyparent.mobile.infrastructure.watch.RealSamsungHealthManager;
import com.feri.watchmyparent.mobile.infrastructure.watch.WatchManager;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public abstract class ServiceModule {

    // ✅ Application Services - ACTUALIZAT pentru Kafka-only
    @Provides
    @Singleton
    public static WatchConnectionApplicationService provideWatchConnectionService(WatchManager watchManager) {
        return new WatchConnectionApplicationService(watchManager);
    }

    // ✅ FIX PRINCIPAL: HealthDataApplicationService cu constructorul corect
    @Provides
    @Singleton
    public static HealthDataApplicationService provideHealthDataService(
            UserRepository userRepository,
            SensorDataRepository sensorDataRepository,
            DataTransmissionService dataTransmissionService, // ✅ CORECTAT
            SensorDataIntegrationService sensorDataIntegrationService) { // ✅ CORECTAT
        return new HealthDataApplicationService(
                userRepository,
                sensorDataRepository,
                dataTransmissionService,
                sensorDataIntegrationService);
    }

    // ✅ CORECTAT: LocationApplicationService
    @Provides
    @Singleton
    public static LocationApplicationService provideLocationService(
            LocationDataRepository locationRepository,
            UserRepository userRepository,
            DataTransmissionService dataTransmissionService) { // ✅ CORECTAT
        return new LocationApplicationService(
                locationRepository, userRepository, dataTransmissionService);
    }

    @Provides
    @Singleton
    public static UserApplicationService provideUserService(
            UserRepository userRepository,
            SensorConfigurationRepository configurationRepository) {
        return new UserApplicationService(userRepository, configurationRepository);
    }

    // ✅ Infrastructure Services - NOI pentru Kafka-only pipeline
    @Provides
    @Singleton
    public static OfflineDataManager provideOfflineDataManager(@ApplicationContext Context context) {
        return new OfflineDataManager(context);
    }

    @Provides
    @Singleton
    public static KafkaHealthCheckService provideKafkaHealthCheckService(
            RealHealthDataKafkaProducer kafkaProducer) {
        return new KafkaHealthCheckService(kafkaProducer);
    }

    @Provides
    @Singleton
    public static KafkaRetryService provideKafkaRetryService(
            RealHealthDataKafkaProducer kafkaProducer,
            KafkaHealthCheckService healthCheckService,
            OfflineDataManager offlineDataManager) {
        return new KafkaRetryService(kafkaProducer, healthCheckService, offlineDataManager);
    }

    @Provides
    @Singleton
    public static NetworkStateManager provideNetworkStateManager(@ApplicationContext Context context) {
        return new NetworkStateManager(context);
    }

    // ✅ Data Transmission Service - Implementarea interface-ului
    @Binds
    @Singleton
    public abstract DataTransmissionService bindDataTransmissionService(
            DataTransmissionServiceImpl dataTransmissionServiceImpl);

    @Provides
    @Singleton
    public static DataTransmissionServiceImpl provideDataTransmissionServiceImpl(
            RealHealthDataKafkaProducer kafkaProducer,
            KafkaHealthCheckService kafkaHealthService,
            KafkaRetryService retryService,
            OfflineDataManager offlineDataManager,
            NetworkStateManager networkStateManager) {
        return new DataTransmissionServiceImpl(
                kafkaProducer, kafkaHealthService, retryService,
                offlineDataManager, networkStateManager);
    }

    // ✅ Servicii existente - ACTUALIZAT pentru Kafka-only
    @Provides
    @Singleton
    public static SamsungHealthDataService provideSamsungHealthDataService(@ApplicationContext Context context) {
        return new SamsungHealthDataService(context);
    }

    @Provides
    @Singleton
    public static SensorDataIntegrationService provideSensorDataIntegrationService(
            RealSamsungHealthManager watchManager,
            SamsungHealthDataService samsungHealthDataService,
            DataTransmissionService dataTransmissionService) { // ✅ ÎNLOCUIT PostgreSQL
        return new SensorDataIntegrationService(watchManager, samsungHealthDataService, dataTransmissionService);
    }

    @Provides
    @Singleton
    public static RealSamsungHealthManager provideRealSamsungHealthManager(
            @ApplicationContext Context context,
            SamsungHealthDataService samsungHealthDataService) {
        return new RealSamsungHealthManager(context, samsungHealthDataService);
    }

    // ✅ ELIMINAT: PostgreSQLDataService nu mai e necesar pentru pipeline-ul principal
    // Dacă e necesar pentru teste de conectivitate, poate fi adăugat separat
}