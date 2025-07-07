package com.feri.watchmyparent.mobile.application.services;

import android.util.Log;

import com.feri.watchmyparent.mobile.application.dto.LocationDataDTO;
import com.feri.watchmyparent.mobile.domain.entities.LocationData;
import com.feri.watchmyparent.mobile.domain.entities.User;
import com.feri.watchmyparent.mobile.domain.repositories.LocationDataRepository;
import com.feri.watchmyparent.mobile.domain.repositories.UserRepository;
import com.feri.watchmyparent.mobile.domain.valueobjects.AddressUser;
import com.feri.watchmyparent.mobile.domain.valueobjects.LocationStatus;
import com.feri.watchmyparent.mobile.infrastructure.external.LocationServiceAdapter;
import com.feri.watchmyparent.mobile.infrastructure.kafka.HealthDataKafkaProducer;
import com.feri.watchmyparent.mobile.infrastructure.kafka.KafkaMessageFormatter;
//import timber.log.Timber;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Singleton
public class LocationApplicationService {

    private static final String TAG = "LocationApplicationService";

    private final LocationDataRepository locationDataRepository;
    private final UserRepository userRepository;

    @Inject
    public LocationApplicationService(
            LocationDataRepository locationDataRepository,
            UserRepository userRepository) {
        this.locationDataRepository = locationDataRepository;
        this.userRepository = userRepository;
    }

    public CompletableFuture<Boolean> updateLocation(String userId, double latitude, double longitude, double accuracy) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Verificăm dacă utilizatorul există
                Optional<User> userOpt = userRepository.findById(userId).join();

                if (!userOpt.isPresent()) {
                    Log.w(TAG, "Cannot update location: User not found: " + userId);
                    return false;
                }

                User user = userOpt.get();

                // Creăm și salvăm noua locație
                LocationData locationData = new LocationData();
                locationData.setIdLocationData(java.util.UUID.randomUUID().toString());
                locationData.setUser(user);

                // Actualizăm status-ul locației
                LocationStatus locationStatus = new LocationStatus();
                locationStatus.setStatus("ACTIVE");
                locationStatus.setLatitude(latitude);
                locationStatus.setLongitude(longitude);
                locationStatus.setTimestamp(LocalDateTime.now());

                locationData.setLocationStatus(locationStatus);
                locationData.setHomeLatitude(latitude); // Implicit setăm aceeași locație ca home
                locationData.setHomeLongitude(longitude);

                locationDataRepository.save(locationData).join();
                Log.d(TAG, "Location updated for user " + userId);

                return true;
            } catch (Exception e) {
                Log.e(TAG, "Error updating location for user " + userId, e);
                return false;
            }
        });
    }

    public CompletableFuture<Optional<LocationData>> getLastLocation(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Verificăm dacă utilizatorul există
                Optional<User> userOpt = userRepository.findById(userId).join();

                if (!userOpt.isPresent()) {
                    Log.w(TAG, "Cannot get location: User not found: " + userId);
                    return Optional.empty();
                }

                return locationDataRepository.findByUserId(userId).join();
            } catch (Exception e) {
                Log.e(TAG, "Error getting last location for user " + userId, e);
                return Optional.empty();
            }
        });
    }
}



/*

    private final LocationServiceAdapter locationService;
    private final LocationDataRepository locationRepository;
    private final UserRepository userRepository;
    private final HealthDataKafkaProducer kafkaProducer;
    private final KafkaMessageFormatter messageFormatter;

    @Inject
    public LocationApplicationService(
            LocationServiceAdapter locationService,
            LocationDataRepository locationRepository,
            UserRepository userRepository,
            HealthDataKafkaProducer kafkaProducer,
            KafkaMessageFormatter messageFormatter) {
        this.locationService = locationService;
        this.locationRepository = locationRepository;
        this.userRepository = userRepository;
        this.kafkaProducer = kafkaProducer;
        this.messageFormatter = messageFormatter;
    }

    public CompletableFuture<LocationDataDTO> updateUserLocation(String userId) {
        return userRepository.findById(userId)
                .thenCompose(userOpt -> {
                    if (userOpt.isPresent()) {
                        throw new RuntimeException("User not found: " + userId);
                    }
                    User user = userOpt.get();

                    return locationService.getCurrentLocation()
                            .thenCompose(currentLocation -> processLocationUpdate(user, currentLocation));
                })
                .exceptionally(throwable -> {
                    //Timber.e(throwable, "Error updating location for user %s", userId);
                    Log.e("LocationApplicationService", "Error updating location for user " + userId + throwable.getMessage());
                    return null;
                });
    }

    private CompletableFuture<LocationDataDTO> processLocationUpdate(User user, LocationStatus currentLocation) {
        return locationRepository.findByUserId(user.getIdUser())
                .thenCompose(existingLocationOpt -> {
                    LocationData locationData;

                    if (existingLocationOpt.isPresent()) {
                        locationData = existingLocationOpt.get();
                    } else {
                        // Create new location data with home coordinates from user address
                        double[] homeCoordinates = getHomeCoordinatesFromAddress(user.getAddressUser());
                        locationData = new LocationData(user, homeCoordinates[0], homeCoordinates[1]);
                    }

                    // Update location with HOME/AWAY logic
                    locationData.updateLocation(
                            currentLocation.getLatitude(),
                            currentLocation.getLongitude(),
                            currentLocation.getAddress()
                    );

                    return saveAndTransmitLocation(locationData);
                });
    }

    private CompletableFuture<LocationDataDTO> saveAndTransmitLocation(LocationData locationData) {
        return locationRepository.save(locationData)
                .thenCompose(saved -> {
                    // Transmit to Kafka
                    String formattedMessage = messageFormatter.formatLocationData(saved);
                    return kafkaProducer.sendHealthData(formattedMessage, saved.getUser().getIdUser())
                            .thenApply(transmitted -> {
                                LocationDataDTO dto = convertToDTO(saved);
                                Log.d("LocationApplicationService", "Location updated for user " + saved.getUser().getIdUser() + ": " + (saved.isAtHome() ? "HOME" : "AWAY"));
                                return dto;
                            });
                });
    }

    public CompletableFuture<LocationDataDTO> getCurrentUserLocation(String userId) {
        return locationRepository.findByUserId(userId)
                .thenApply(locationOpt -> locationOpt.map(this::convertToDTO).orElse(null));
    }

    public CompletableFuture<Boolean> updateHomeLocation(String userId, double latitude, double longitude) {
        return userRepository.findById(userId)
                .thenCompose(userOpt -> {
                    if (userOpt.isPresent()) {
                        throw new RuntimeException("User not found: " + userId);
                    }

                    return locationRepository.findByUserId(userId)
                            .thenCompose(locationOpt -> {
                                LocationData locationData;
                                if (locationOpt.isPresent()) {
                                    locationData = locationOpt.get();
                                    locationData.updateHomeLocation(latitude, longitude);
                                } else {
                                    locationData = new LocationData(userOpt.get(), latitude, longitude);
                                }

                                return locationRepository.save(locationData)
                                        .thenApply(saved -> true);
                            });
                });
    }

    public void startLocationTracking(String userId, LocationServiceAdapter.LocationUpdateCallback callback) {
        locationService.startLocationUpdates(new LocationServiceAdapter.LocationUpdateCallback() {
            @Override
            public void onLocationUpdate(LocationStatus locationStatus) {
                updateUserLocation(userId)
                        .thenAccept(dto -> {
                            if (dto != null) {
                                callback.onLocationUpdate(locationStatus);
                            }
                        });
            }

            @Override
            public void onError(Exception error) {
                Log.e("LocationApplicationService", "Location tracking error for user " + userId, error);
                callback.onError(error);
            }
        });
    }

    public void stopLocationTracking() {
        locationService.stopLocationUpdates();
    }

    private double[] getHomeCoordinatesFromAddress(AddressUser address) {
        // In a real implementation, you would geocode the address
        // For MVP, return default coordinates (Oradea, Romania as specified)
        if (address == null) {
            return new double[]{47.0722, 21.9211}; // Oradea coordinates
        }

        // TODO: Implement address geocoding
        return new double[]{47.0722, 21.9211}; // Default to Oradea
    }

    private LocationDataDTO convertToDTO(LocationData locationData) {
        LocationStatus status = locationData.getLocationStatus();
        LocationDataDTO dto = new LocationDataDTO(
                locationData.getUser().getIdUser(),
                status.getStatus(),
                status.getLatitude(),
                status.getLongitude(),
                status.getAddress()
        );
        dto.setTimestamp(status.getTimestamp());
        dto.setAtHome(locationData.isAtHome());
        return dto;
    }
}
*/