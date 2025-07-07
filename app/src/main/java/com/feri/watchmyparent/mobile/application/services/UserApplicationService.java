package com.feri.watchmyparent.mobile.application.services;

import android.util.Log;

import com.feri.watchmyparent.mobile.application.dto.UserRegistrationDTO;
import com.feri.watchmyparent.mobile.domain.entities.User;
import com.feri.watchmyparent.mobile.domain.entities.MedicalProfile;
import com.feri.watchmyparent.mobile.domain.entities.SensorConfiguration;
import com.feri.watchmyparent.mobile.domain.enums.SensorType;
import com.feri.watchmyparent.mobile.domain.repositories.UserRepository;
import com.feri.watchmyparent.mobile.domain.repositories.SensorConfigurationRepository;
//import timber.log.Timber;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Singleton
public class UserApplicationService {

    private final UserRepository userRepository;
    private final SensorConfigurationRepository configurationRepository;

    @Inject
    public UserApplicationService(
            UserRepository userRepository,
            SensorConfigurationRepository configurationRepository) {
        this.userRepository = userRepository;
        this.configurationRepository = configurationRepository;
    }

    public CompletableFuture<User> registerUser(UserRegistrationDTO registrationDTO) {
        if (!registrationDTO.isValid()) {
            throw new IllegalArgumentException("Invalid registration data");
        }

        return userRepository.existsByEmail(registrationDTO.getEmail())
                .thenCompose(exists -> {
                    if (exists) {
                        throw new RuntimeException("Email already registered");
                    }

                    User user = new User(
                            registrationDTO.getFirstName(),
                            registrationDTO.getLastName(),
                            registrationDTO.getEmail(),
                            registrationDTO.getUserType()
                    );

                    user.setPassword(registrationDTO.getPassword()); // Should be hashed
                    user.setDateOfBirthUser(registrationDTO.getDateOfBirth());
                    user.setPhoneNumberUser(registrationDTO.getPhoneNumber());

                    // Create default medical profile
                    MedicalProfile medicalProfile = new MedicalProfile(user);
                    user.setMedicalProfile(medicalProfile);

                    return userRepository.save(user)
                            .thenCompose(savedUser -> createDefaultSensorConfigurations(savedUser));
                });
    }

    private CompletableFuture<User> createDefaultSensorConfigurations(User user) {
        List<SensorType> allSensorTypes = Arrays.asList(SensorType.values());

        // Creează configurații pentru fiecare tip de senzor
        CompletableFuture<?>[] configFutures = allSensorTypes
                .stream()
                .map(sensorType -> {
                    SensorConfiguration config = new SensorConfiguration(
                            user,
                            sensorType,
                            sensorType.getCriticalityLevel().getDefaultFrequencySeconds()
                    );
                    return configurationRepository.save(config);
                })
                .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(configFutures)
                .thenApply(ignored -> {
                    Log.d("Created default sensor configurations for user %s", user.getIdUser());
                    return user;
                });
    }

    public CompletableFuture<User> findUserById(String userId) {
        return userRepository.findById(userId)
                .thenApply(userOpt -> userOpt.orElseThrow(() ->
                        new RuntimeException("User not found: " + userId)));
    }

    public CompletableFuture<User> findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .thenApply(userOpt -> userOpt.orElseThrow(() ->
                        new RuntimeException("User not found with email: " + email)));
    }

    public CompletableFuture<User> updateUser(User user) {
        return userRepository.save(user);
    }
}
