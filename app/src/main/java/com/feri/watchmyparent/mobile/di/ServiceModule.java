package com.feri.watchmyparent.mobile.di;

import com.feri.watchmyparent.mobile.application.services.*;
import com.feri.watchmyparent.mobile.domain.repositories.*;
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

    // ✅ Application Services - orchestrează business logic
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
            HealthDataKafkaProducer kafkaProducer) {
        return new HealthDataApplicationService(
                userRepository, sensorDataRepository, configurationRepository, kafkaProducer);
    }

    @Provides
    @Singleton
    public LocationApplicationService provideLocationService(
            LocationDataRepository locationRepository,
            UserRepository userRepository) {
        return new LocationApplicationService(locationRepository, userRepository);
    }

    @Provides
    @Singleton
    public UserApplicationService provideUserService(
            UserRepository userRepository,
            SensorConfigurationRepository configurationRepository) {
        return new UserApplicationService(userRepository, configurationRepository);
    }

//    // ✅ Infrastructure Services - păstrate pentru compatibilitate
//    // NOTĂ: Acestea vor fi eliminate treptat în favoarea Application Services
//    @Provides
//    @Singleton
//    public WatchConnectionService provideWatchConnectionService(
//            android.content.Context context,
//            com.feri.watchmyparent.mobile.infrastructure.watch.SamsungWatchManager samsungWatchManager) {
//        return new WatchConnectionService(context, samsungWatchManager);
//    }
}
