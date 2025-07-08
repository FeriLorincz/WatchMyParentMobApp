package com.feri.watchmyparent.mobile.di;

import com.feri.watchmyparent.mobile.application.services.*;
import com.feri.watchmyparent.mobile.domain.repositories.*;
import com.feri.watchmyparent.mobile.infrastructure.database.PostgreSQLConfig;
import com.feri.watchmyparent.mobile.infrastructure.kafka.RealHealthDataKafkaProducer;
import com.feri.watchmyparent.mobile.infrastructure.services.PostgreSQLDataService;
import com.feri.watchmyparent.mobile.infrastructure.services.WatchConnectionService;
import com.feri.watchmyparent.mobile.infrastructure.watch.WatchManager;
import com.feri.watchmyparent.mobile.infrastructure.kafka.HealthDataKafkaProducer;
import com.feri.watchmyparent.mobile.infrastructure.kafka.KafkaMessageFormatter;
import com.feri.watchmyparent.mobile.infrastructure.external.LocationServiceAdapter;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public class ServiceModule {

    // ✅ Application Services - orchestrează business logic REAL
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

    // ✅ Infrastructure Services - real implementations
    @Provides
    @Singleton
    public PostgreSQLDataService providePostgreSQLDataService(PostgreSQLConfig postgreSQLConfig) {
        return new PostgreSQLDataService(postgreSQLConfig);
    }
}
