package com.feri.watchmyparent.mobile.infrastructure.utils;

import android.util.Log;
import com.feri.watchmyparent.mobile.application.services.UserApplicationService;
import com.feri.watchmyparent.mobile.application.dto.UserRegistrationDTO;
import com.feri.watchmyparent.mobile.domain.enums.UserType;
import com.feri.watchmyparent.mobile.domain.entities.User;
import com.feri.watchmyparent.mobile.domain.repositories.UserRepository;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

@Singleton
public class DemoDataInitializer {

    private static final String TAG = "DemoDataInitializer";
    private static final String DEMO_USER_ID = "demo-user-id";

    private final UserRepository userRepository;
    private final UserApplicationService userApplicationService;

    @Inject
    public DemoDataInitializer(UserRepository userRepository, UserApplicationService userApplicationService) {
        this.userRepository = userRepository;
        this.userApplicationService = userApplicationService;
    }

    public CompletableFuture<Boolean> initializeDemoData() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Checking if demo user exists...");

                // Check if demo user already exists
                boolean userExists = userRepository.findById(DEMO_USER_ID)
                        .thenApply(userOpt -> userOpt.isPresent())
                        .join();

                if (userExists) {
                    Log.d(TAG, "Demo user already exists");
                    return true;
                }

                Log.d(TAG, "Creating demo user...");

                // Create demo user
                User demoUser = createDemoUser();
                userRepository.save(demoUser).join();

                Log.d(TAG, "✅ Demo user created successfully: " + DEMO_USER_ID);
                return true;

            } catch (Exception e) {
                Log.e(TAG, "❌ Error initializing demo data", e);
                return false;
            }
        });
    }

    private User createDemoUser() {
        User user = new User();
        user.setIdUser(DEMO_USER_ID);
        user.setFirstNameUser("Demo");
        user.setLastNameUser("User");
        user.setEmailUser("demo@watchmyparent.com");
        user.setPassword("demo123"); // In production, this should be hashed
        user.setUserType(UserType.SENIOR);
        user.setDateOfBirthUser(LocalDate.of(1950, 1, 1));
        user.setPhoneNumberUser("+40123456789");
        user.setEnabled(true);
        user.setAccountNonExpired(true);
        user.setAccountNonLocked(true);
        user.setCredentialsNonExpired(true);

        return user;
    }

    public CompletableFuture<Boolean> createDemoUserIfNeeded() {
        return userRepository.findById(DEMO_USER_ID)
                .thenCompose(userOpt -> {
                    if (userOpt.isPresent()) {
                        Log.d(TAG, "Demo user already exists");
                        return CompletableFuture.completedFuture(true);
                    } else {
                        Log.d(TAG, "Creating demo user...");
                        return createDemoUserAsync();
                    }
                });
    }

    private CompletableFuture<Boolean> createDemoUserAsync() {
        UserRegistrationDTO registrationDTO = new UserRegistrationDTO();
        registrationDTO.setFirstName("Demo");
        registrationDTO.setLastName("User");
        registrationDTO.setEmail("demo@watchmyparent.com");
        registrationDTO.setPassword("demo123");
        registrationDTO.setConfirmPassword("demo123");
        registrationDTO.setUserType(UserType.SENIOR);
        registrationDTO.setDateOfBirth(LocalDate.of(1950, 1, 1));
        registrationDTO.setPhoneNumber("+40123456789");

        return userApplicationService.registerUser(registrationDTO)
                .thenApply(user -> {
                    // Update the user ID to our demo ID
                    user.setIdUser(DEMO_USER_ID);
                    userRepository.save(user).join();
                    Log.d(TAG, "✅ Demo user created with ID: " + DEMO_USER_ID);
                    return true;
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "❌ Failed to create demo user", throwable);
                    return false;
                });
    }
}
