package com.feri.watchmyparent.mobile.di;

import com.feri.watchmyparent.mobile.application.services.*;
import com.feri.watchmyparent.mobile.domain.repositories.*;
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

    @Provides
    @Singleton
    public WatchConnectionApplicationService provideWatchConnectionService(WatchManager watchManager) {
        return new WatchConnectionApplicationService(watchManager);
    }

    @Provides
    @Singleton
    public HealthDataApplicationService provideHealthDataService(
            WatchManager watchManager,
            SensorDataRepository sensorDataRepository,
            SensorConfigurationRepository configurationRepository,
            UserRepository userRepository,
            HealthDataKafkaProducer kafkaProducer,
            KafkaMessageFormatter messageFormatter) {
        return new HealthDataApplicationService(
                watchManager, sensorDataRepository, configurationRepository,
                userRepository, kafkaProducer, messageFormatter);
    }

    @Provides
    @Singleton
    public LocationApplicationService provideLocationService(
            LocationServiceAdapter locationService,
            LocationDataRepository locationRepository,
            UserRepository userRepository,
            HealthDataKafkaProducer kafkaProducer,
            KafkaMessageFormatter messageFormatter) {
        return new LocationApplicationService(
                locationService, locationRepository, userRepository,
                kafkaProducer, messageFormatter);
    }

    @Provides
    @Singleton
    public UserApplicationService provideUserService(
            UserRepository userRepository,
            SensorConfigurationRepository configurationRepository) {
        return new UserApplicationService(userRepository, configurationRepository);
    }
}
