package com.feri.watchmyparent.mobile.di;

import android.content.Context;
import com.feri.watchmyparent.mobile.infrastructure.watch.WatchManager;
import com.feri.watchmyparent.mobile.infrastructure.watch.WatchManagerFactory;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public class WatchModule {

    @Provides
    @Singleton
    public WatchManager provideWatchManager(@ApplicationContext Context context) {
        return WatchManagerFactory.createWatchManager(context);
    }
}
