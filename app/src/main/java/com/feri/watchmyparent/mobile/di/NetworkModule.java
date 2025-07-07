package com.feri.watchmyparent.mobile.di;

import com.feri.watchmyparent.mobile.infrastructure.kafka.HealthDataKafkaProducer;
import com.feri.watchmyparent.mobile.infrastructure.kafka.KafkaMessageFormatter;
import com.feri.watchmyparent.mobile.infrastructure.external.LocationServiceAdapter;
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

//   asta e cel bun!!!!
//    @Provides
//    @Singleton
//    public HealthDataKafkaProducer provideKafkaProducer() {
//        return new HealthDataKafkaProducer();
//    }

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
