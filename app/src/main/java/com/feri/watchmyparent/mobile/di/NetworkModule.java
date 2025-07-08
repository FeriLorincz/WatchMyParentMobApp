package com.feri.watchmyparent.mobile.di;

import com.feri.watchmyparent.mobile.infrastructure.database.PostgreSQLConfig;
import com.feri.watchmyparent.mobile.infrastructure.kafka.HealthDataKafkaProducer;
import com.feri.watchmyparent.mobile.infrastructure.kafka.KafkaMessageFormatter;
import com.feri.watchmyparent.mobile.infrastructure.external.LocationServiceAdapter;
import com.feri.watchmyparent.mobile.infrastructure.kafka.RealHealthDataKafkaProducer;

import android.content.Context;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public class NetworkModule {

    @Provides
    @Singleton
    public HealthDataKafkaProducer provideKafkaProducer() {
        // Returnăm direct instanța noastră mock, fără a initializa KafkaProducer real
        return new HealthDataKafkaProducer();
    }

    @Provides
    @Singleton
    public RealHealthDataKafkaProducer provideRealKafkaProducer() {
        // Provide real Kafka producer for services that need it specifically
        return new RealHealthDataKafkaProducer();
    }

    @Provides
    @Singleton
    public KafkaMessageFormatter provideKafkaMessageFormatter() {
        return new KafkaMessageFormatter();
    }

    @Provides
    @Singleton
    public LocationServiceAdapter provideLocationServiceAdapter(@ApplicationContext Context context) {
        return new LocationServiceAdapter(context);
    }

    @Provides
    @Singleton
    public PostgreSQLConfig providePostgreSQLConfig() {
        // ✅ REAL POSTGRESQL IMPLEMENTATION
        return new PostgreSQLConfig();
    }
}
