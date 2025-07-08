package com.feri.watchmyparent.mobile.di;

import com.feri.watchmyparent.mobile.infrastructure.utils.DemoDataInitializer;
import com.feri.watchmyparent.mobile.domain.repositories.UserRepository;
import com.feri.watchmyparent.mobile.application.services.UserApplicationService;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public class UtilsModule {

    @Provides
    @Singleton
    public DemoDataInitializer provideDemoDataInitializer(
            UserRepository userRepository,
            UserApplicationService userApplicationService) {
        return new DemoDataInitializer(userRepository, userApplicationService);
    }
}
