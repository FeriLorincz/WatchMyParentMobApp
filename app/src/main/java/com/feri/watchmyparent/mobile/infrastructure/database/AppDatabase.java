package com.feri.watchmyparent.mobile.infrastructure.database;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import android.content.Context;
import com.feri.watchmyparent.mobile.infrastructure.database.entities.*;
import com.feri.watchmyparent.mobile.infrastructure.database.dao.*;
import com.feri.watchmyparent.mobile.infrastructure.database.converters.*;

@Database(
        entities = {
                UserEntity.class,
                SensorDataEntity.class,
                SensorConfigurationEntity.class,
                LocationDataEntity.class,
                EmergencyContactEntity.class,
                MedicalProfileEntity.class
        },
        version = 1,
        exportSchema = false
)
@TypeConverters({
        DateTimeConverter.class,
        SensorTypeConverter.class,
        UserTypeConverter.class,
        TransmissionStatusConverter.class,
        LocationStatusConverter.class,
        StringSetConverter.class,
        MedicationSetConverter.class
})
public abstract class AppDatabase extends RoomDatabase{

    // DAOs
    public abstract UserDao userDao();
    public abstract SensorDataDao sensorDataDao();
    public abstract SensorConfigurationDao sensorConfigurationDao();
    public abstract LocationDataDao locationDataDao();
    public abstract EmergencyContactDao emergencyContactDao();
    public abstract MedicalProfileDao medicalProfileDao();

    // Singleton instance
    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "watchmyparent_database"
                            )
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
