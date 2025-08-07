package com.feri.watchmyparent.mobile.application.services;

import android.util.Log;

import com.feri.watchmyparent.mobile.application.dto.LocationDataDTO;
import com.feri.watchmyparent.mobile.domain.entities.LocationData;
import com.feri.watchmyparent.mobile.domain.entities.User;
import com.feri.watchmyparent.mobile.domain.repositories.LocationDataRepository;
import com.feri.watchmyparent.mobile.domain.repositories.UserRepository;
import com.feri.watchmyparent.mobile.domain.valueobjects.LocationStatus;
import com.feri.watchmyparent.mobile.infrastructure.kafka.RealHealthDataKafkaProducer;
import com.feri.watchmyparent.mobile.infrastructure.services.PostgreSQLDataService;
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
    private final RealHealthDataKafkaProducer kafkaProducer;
    private final PostgreSQLDataService postgreSQLDataService;

    @Inject
    public LocationApplicationService(
            LocationDataRepository locationDataRepository,
            UserRepository userRepository,
            RealHealthDataKafkaProducer kafkaProducer,
            PostgreSQLDataService postgreSQLDataService) {
        this.locationDataRepository = locationDataRepository;
        this.userRepository = userRepository;
        this.kafkaProducer = kafkaProducer;
        this.postgreSQLDataService = postgreSQLDataService;
    }

    /**
     * Update user location with coordinates and accuracy - REAL IMPLEMENTATION
     */
    public CompletableFuture<Boolean> updateLocation(String userId, double latitude, double longitude, double accuracy) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "🔄 Updating REAL location for user: " + userId);

                // Verificăm dacă utilizatorul există
                Optional<User> userOpt = userRepository.findById(userId).join();

                if (!userOpt.isPresent()) {
                    Log.w(TAG, "❌ Cannot update location: User not found: " + userId);
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
                locationStatus.setAddress("Address from coordinates: " + latitude + ", " + longitude);

                locationData.setLocationStatus(locationStatus);
                locationData.setHomeLatitude(latitude); // Implicit setăm aceeași locație ca home
                locationData.setHomeLongitude(longitude);

                // Save to local Room database
                locationDataRepository.save(locationData).join();

                // ✅ Send to REAL Kafka
                kafkaProducer.sendLocationData(locationData, userId);

                // ✅ Save to REAL PostgreSQL
                postgreSQLDataService.insertLocationData(locationData);

                Log.d(TAG, "✅ REAL location updated for user " + userId + " at " + latitude + ", " + longitude);
                return true;

            } catch (Exception e) {
                Log.e(TAG, "❌ Error updating REAL location for user " + userId, e);
                return false;
            }
        });
    }

    /**
     * Update user location - simplified method for backward compatibility
     */
    public CompletableFuture<LocationDataDTO> updateUserLocation(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "🔄 Simulating location update for MVP (using simulated GPS coordinates)");

                // For MVP, we'll simulate a location update
                // In real implementation, this would get GPS coordinates
                double simulatedLat = 47.0722 + (Math.random() - 0.5) * 0.01; // Oradea area
                double simulatedLng = 21.9211 + (Math.random() - 0.5) * 0.01;

                boolean updated = updateLocation(userId, simulatedLat, simulatedLng, 10.0).join();

                if (updated) {
                    Optional<LocationData> locationOpt = getLastLocation(userId).join();
                    if (locationOpt.isPresent()) {
                        return convertToDTO(locationOpt.get());
                    }
                }

                // Return default DTO if update failed
                return createDefaultLocationDTO(userId);

            } catch (Exception e) {
                Log.e(TAG, "❌ Error in updateUserLocation for user " + userId, e);
                return createDefaultLocationDTO(userId);
            }
        });
    }

    /**
     * Get current user location as DTO
     */
    public CompletableFuture<LocationDataDTO> getCurrentUserLocation(String userId) {
        return getLastLocation(userId)
                .thenApply(locationOpt -> {
                    if (locationOpt.isPresent()) {
                        return convertToDTO(locationOpt.get());
                    } else {
                        return createDefaultLocationDTO(userId);
                    }
                });
    }

    /**
     * Get last location for user
     */
    public CompletableFuture<Optional<LocationData>> getLastLocation(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Verificăm dacă utilizatorul există
                Optional<User> userOpt = userRepository.findById(userId).join();

                if (!userOpt.isPresent()) {
                    Log.w(TAG, "❌ Cannot get location: User not found: " + userId);
                    return Optional.empty();
                }

                return locationDataRepository.findByUserId(userId).join();

            } catch (Exception e) {
                Log.e(TAG, "❌ Error getting last location for user " + userId, e);
                return Optional.empty();
            }
        });
    }

    /**
     * Update home location for user
     */
    public CompletableFuture<Boolean> updateHomeLocation(String userId, double latitude, double longitude) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Optional<User> userOpt = userRepository.findById(userId).join();
                if (!userOpt.isPresent()) {
                    Log.w(TAG, "❌ Cannot update home location: User not found: " + userId);
                    return false;
                }

                Optional<LocationData> locationOpt = locationDataRepository.findByUserId(userId).join();
                LocationData locationData;

                if (locationOpt.isPresent()) {
                    locationData = locationOpt.get();
                    locationData.updateHomeLocation(latitude, longitude);
                } else {
                    locationData = new LocationData(userOpt.get(), latitude, longitude);
                }

                // Save to local Room database
                locationDataRepository.save(locationData).join();

                // ✅ Send to REAL Kafka
                kafkaProducer.sendLocationData(locationData, userId);

                // ✅ Save to REAL PostgreSQL
                postgreSQLDataService.insertLocationData(locationData);

                Log.d(TAG, "✅ REAL home location updated for user " + userId);
                return true;

            } catch (Exception e) {
                Log.e(TAG, "❌ Error updating REAL home location for user " + userId, e);
                return false;
            }
        });
    }

    // ✅ Helper methods for DTO conversion
    private LocationDataDTO convertToDTO(LocationData locationData) {
        LocationDataDTO dto = new LocationDataDTO();
        dto.setUserId(locationData.getUser().getIdUser());

        if (locationData.getLocationStatus() != null) {
            dto.setStatus(locationData.getLocationStatus().getStatus());
            dto.setLatitude(locationData.getLocationStatus().getLatitude());
            dto.setLongitude(locationData.getLocationStatus().getLongitude());
            dto.setAddress(locationData.getLocationStatus().getAddress());
            dto.setTimestamp(locationData.getLocationStatus().getTimestamp());
        } else {
            dto.setStatus("UNKNOWN");
            dto.setLatitude(0.0);
            dto.setLongitude(0.0);
            dto.setAddress("No location available");
            dto.setTimestamp(LocalDateTime.now());
        }

        dto.setAtHome(locationData.isAtHome());
        return dto;
    }

    private LocationDataDTO createDefaultLocationDTO(String userId) {
        LocationDataDTO dto = new LocationDataDTO();
        dto.setUserId(userId);
        dto.setStatus("UNKNOWN");
        dto.setLatitude(0.0);
        dto.setLongitude(0.0);
        dto.setAddress("No location data available");
        dto.setTimestamp(LocalDateTime.now());
        dto.setAtHome(false);
        return dto;
    }
}