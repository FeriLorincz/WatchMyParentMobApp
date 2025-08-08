package com.feri.watchmyparent.mobile.di;

import com.feri.watchmyparent.mobile.infrastructure.database.PostgreSQLConfig;
import com.feri.watchmyparent.mobile.infrastructure.kafka.KafkaMessageFormatter;
import com.feri.watchmyparent.mobile.infrastructure.external.LocationServiceAdapter;
import com.feri.watchmyparent.mobile.infrastructure.kafka.RealHealthDataKafkaProducer;

import android.content.Context;
import android.util.Log;

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
    public RealHealthDataKafkaProducer provideRealKafkaProducer() {
        try {
            Log.d("NetworkModule", "Creating RealHealthDataKafkaProducer for Kafka-only pipeline");
            return new RealHealthDataKafkaProducer();
        } catch (Exception e) {
            Log.e("NetworkModule", "Error creating RealHealthDataKafkaProducer", e);
            throw new RuntimeException("Failed to create RealHealthDataKafkaProducer", e);
        }
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
}