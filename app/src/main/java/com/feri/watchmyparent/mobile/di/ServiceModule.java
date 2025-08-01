package com.feri.watchmyparent.mobile.di;

import android.content.Context;

import com.feri.watchmyparent.mobile.application.services.*;
import com.feri.watchmyparent.mobile.domain.repositories.*;
import com.feri.watchmyparent.mobile.infrastructure.database.PostgreSQLConfig;
import com.feri.watchmyparent.mobile.infrastructure.kafka.RealHealthDataKafkaProducer;
import com.feri.watchmyparent.mobile.infrastructure.services.PostgreSQLDataService;
import com.feri.watchmyparent.mobile.infrastructure.services.SamsungHealthDataService;
import com.feri.watchmyparent.mobile.infrastructure.services.SensorDataIntegrationService;
import com.feri.watchmyparent.mobile.infrastructure.services.WatchConnectionService;
import com.feri.watchmyparent.mobile.infrastructure.watch.RealSamsungHealthManager;
import com.feri.watchmyparent.mobile.infrastructure.watch.WatchManager;
import com.feri.watchmyparent.mobile.infrastructure.kafka.HealthDataKafkaProducer;
import com.feri.watchmyparent.mobile.infrastructure.kafka.KafkaMessageFormatter;
import com.feri.watchmyparent.mobile.infrastructure.external.LocationServiceAdapter;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public class ServiceModule {

    // Application Services - orchestrează business logic REAL
    @Provides
    @Singleton
    public WatchConnectionApplicationService provideWatchConnectionService(WatchManager watchManager) {
        return new WatchConnectionApplicationService(watchManager);
    }

    @Provides
    @Singleton
    public HealthDataApplicationService provideHealthDataService(
            UserRepository userRepository,
            SensorDataRepository sensorDataRepository,
            SensorConfigurationRepository configurationRepository,
            HealthDataKafkaProducer kafkaProducer,
            PostgreSQLDataService postgreSQLDataService,
            WatchManager watchManager) {
        return new HealthDataApplicationService(
                userRepository, sensorDataRepository, configurationRepository,
                kafkaProducer, postgreSQLDataService, watchManager);
    }

    @Provides
    @Singleton
    public LocationApplicationService provideLocationService(
            LocationDataRepository locationRepository,
            UserRepository userRepository,
            RealHealthDataKafkaProducer kafkaProducer,
            PostgreSQLDataService postgreSQLDataService) {
        return new LocationApplicationService(
                locationRepository, userRepository, kafkaProducer, postgreSQLDataService);
    }

    @Provides
    @Singleton
    public UserApplicationService provideUserService(
            UserRepository userRepository,
            SensorConfigurationRepository configurationRepository) {
        return new UserApplicationService(userRepository, configurationRepository);
    }

    @Provides
    @Singleton
    public SamsungHealthDataService provideSamsungHealthDataService(@ApplicationContext Context context) {
        return new SamsungHealthDataService(context);
    }

    @Provides
    @Singleton
    public SensorDataIntegrationService provideSensorDataIntegrationService(
            RealSamsungHealthManager watchManager,
            SamsungHealthDataService samsungHealthDataService,
            PostgreSQLDataService postgreSQLDataService) {
        return new SensorDataIntegrationService(watchManager, samsungHealthDataService, postgreSQLDataService);
    }

    // ✅ Infrastructure Services - real implementations - Single PostgreSQL Service with initialization
    @Provides
    @Singleton
    public PostgreSQLDataService providePostgreSQLDataService(PostgreSQLConfig postgreSQLConfig) {
        PostgreSQLDataService service = new PostgreSQLDataService(postgreSQLConfig);

        // Initialize tables on startup
        service.initializeTables()
                .thenAccept(success -> {
                    if (success) {
                        android.util.Log.d("ServiceModule", "✅ PostgreSQL tables initialized");
                    } else {
                        android.util.Log.w("ServiceModule", "⚠️ PostgreSQL table initialization failed");
                    }
                });

        return service;
    }

    //@Inject constructor binding for RealSamsungHealthManager
    @Provides
    @Singleton
    public RealSamsungHealthManager provideRealSamsungHealthManager(
            @ApplicationContext Context context,
            SamsungHealthDataService samsungHealthDataService) {
        return new RealSamsungHealthManager(context, samsungHealthDataService);
    }
}