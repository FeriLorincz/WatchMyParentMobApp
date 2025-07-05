package com.feri.watchmyparent.mobile.di;

import com.feri.watchmyparent.mobile.domain.repositories.*;
import com.feri.watchmyparent.mobile.infrastructure.repositories.*;
import com.feri.watchmyparent.mobile.infrastructure.database.dao.*;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public class RepositoryModule {

    @Provides
    @Singleton
    public UserRepository provideUserRepository(UserDao userDao) {
        return new UserRepositoryImpl(userDao);
    }

    @Provides
    @Singleton
    public SensorDataRepository provideSensorDataRepository(SensorDataDao sensorDataDao) {
        // FIXARE: SensorDataRepositoryImpl nu are nevoie de UserRepository în constructor
        return new SensorDataRepositoryImpl(sensorDataDao);
    }

    @Provides
    @Singleton
    public SensorConfigurationRepository provideSensorConfigurationRepository(SensorConfigurationDao sensorConfigurationDao) {
        return new SensorConfigurationRepositoryImpl(sensorConfigurationDao);
    }

    @Provides
    @Singleton
    public LocationDataRepository provideLocationDataRepository(LocationDataDao locationDataDao) {
        return new LocationDataRepositoryImpl(locationDataDao);
    }

    @Provides
    @Singleton
    public EmergencyContactRepository provideEmergencyContactRepository(EmergencyContactDao emergencyContactDao) {
        return new EmergencyContactRepositoryImpl(emergencyContactDao);
    }

    @Provides
    @Singleton
    public MedicalProfileRepository provideMedicalProfileRepository(MedicalProfileDao medicalProfileDao) {
        return new MedicalProfileRepositoryImpl(medicalProfileDao);
    }
}
