package com.feri.watchmyparent.mobile.di;

import android.content.Context;
import androidx.room.Room;
import com.feri.watchmyparent.mobile.infrastructure.database.AppDatabase;
import com.feri.watchmyparent.mobile.infrastructure.database.dao.EmergencyContactDao;
import com.feri.watchmyparent.mobile.infrastructure.database.dao.LocationDataDao;
import com.feri.watchmyparent.mobile.infrastructure.database.dao.MedicalProfileDao;
import com.feri.watchmyparent.mobile.infrastructure.database.dao.SensorConfigurationDao;
import com.feri.watchmyparent.mobile.infrastructure.database.dao.SensorDataDao;
import com.feri.watchmyparent.mobile.infrastructure.database.dao.UserDao;
import com.feri.watchmyparent.mobile.infrastructure.repositories.*;
import com.feri.watchmyparent.mobile.domain.repositories.*;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public class DatabaseModule {

    @Provides
    @Singleton
    public AppDatabase provideAppDatabase(@ApplicationContext Context context) {
        return Room.databaseBuilder(
                        context,
                        AppDatabase.class,
                        "watchmyparent_database"
                )
                .fallbackToDestructiveMigration()
                .build();
    }

    // Provide DAOs
    @Provides
    @Singleton
    public UserDao provideUserDao(AppDatabase database) {
        return database.userDao();
    }

    @Provides
    @Singleton
    public SensorDataDao provideSensorDataDao(AppDatabase database) {
        return database.sensorDataDao();
    }

    @Provides
    @Singleton
    public SensorConfigurationDao provideSensorConfigurationDao(AppDatabase database) {
        return database.sensorConfigurationDao();
    }

    @Provides
    @Singleton
    public LocationDataDao provideLocationDataDao(AppDatabase database) {
        return database.locationDataDao();
    }

    @Provides
    @Singleton
    public EmergencyContactDao provideEmergencyContactDao(AppDatabase database) {
        return database.emergencyContactDao();
    }

    @Provides
    @Singleton
    public MedicalProfileDao provideMedicalProfileDao(AppDatabase database) {
        return database.medicalProfileDao();
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