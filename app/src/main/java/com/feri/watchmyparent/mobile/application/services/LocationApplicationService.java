package com.feri.watchmyparent.mobile.application.services;

import android.util.Log;

import com.feri.watchmyparent.mobile.application.dto.LocationDataDTO;
import com.feri.watchmyparent.mobile.application.interfaces.DataTransmissionService;
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

//MODIFICAT: LocationApplicationService pentru Kafka-only pipeline
//Eliminat PostgreSQL direct, folosește DataTransmissionService
@Singleton
public class LocationApplicationService {

    private static final String TAG = "LocationApplicationService";

    private final LocationDataRepository locationDataRepository;
    private final UserRepository userRepository;
    private final DataTransmissionService dataTransmissionService; // ✅ ÎNLOCUIT serviciile separate

    @Inject
    public LocationApplicationService(
            LocationDataRepository locationDataRepository,
            UserRepository userRepository,
            DataTransmissionService dataTransmissionService) { // ✅ CORECTAT
        this.locationDataRepository = locationDataRepository;
        this.userRepository = userRepository;
        this.dataTransmissionService = dataTransmissionService;

        Log.d(TAG, "✅ LocationApplicationService initialized with Kafka-only pipeline");
    }

    //Update user location with coordinates and accuracy - Update location prin Kafka-only pipeline
    public CompletableFuture<Boolean> updateLocation(String userId, double latitude, double longitude, double accuracy) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "🔄 Updating REAL location for user: " + userId);
                Log.d(TAG, "📍 Coordinates: " + latitude + ", " + longitude + " (accuracy: " + accuracy + "m)");

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
                locationStatus.setAddress("GPS Location: " + String.format("%.6f, %.6f", latitude, longitude));

                locationData.setLocationStatus(locationStatus);
                locationData.setHomeLatitude(latitude); // Implicit setăm aceeași locație ca home
                locationData.setHomeLongitude(longitude);
                locationData.setDeviceId("samsung_galaxy_watch_7_gps");

                // Salvează local în Room database
                locationDataRepository.save(locationData).join();
                Log.d(TAG, "💾 Location saved locally for user: " + userId);

                // ✅ Transmite prin Kafka-only pipeline (eliminat PostgreSQL direct)
                boolean transmitted = dataTransmissionService.transmitData(locationData, userId).join();

                if (transmitted) {
                    Log.d(TAG, "✅ Location transmitted successfully through Kafka pipeline");
                } else {
                    Log.w(TAG, "⚠️ Location transmission failed - stored offline for retry");
                }

                Log.d(TAG, "✅ REAL location processing completed for user " + userId);
                return true; // Returnăm true chiar dacă transmisia eșuează (se va încerca din nou)

            } catch (Exception e) {
                Log.e(TAG, "❌ Error updating REAL location for user " + userId, e);
                return false;
            }
        });
    }

    // Simplified location update cu coordonate GPS simulate in mod realist
    public CompletableFuture<LocationDataDTO> updateUserLocation(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "🔄 Updating location for user: " + userId);

                // Coordonate GPS realiste pentru zona Oradea, România
                double baseLatitude = 47.0722; // Centrul Oradei
                double baseLongitude = 21.9211;

                // Variație realistă în raza de ~500m din centrul orașului
                double latVariation = (Math.random() - 0.5) * 0.005; // ~550m nord-sud
                double lngVariation = (Math.random() - 0.5) * 0.008; // ~550m est-vest

                double currentLat = baseLatitude + latVariation;
                double currentLng = baseLongitude + lngVariation;
                double accuracy = 5.0 + Math.random() * 10.0; // 5-15m accuracy (realistic GPS)

                Log.d(TAG, "📍 Generated GPS coordinates: " +
                        String.format("%.6f, %.6f", currentLat, currentLng) +
                        " (accuracy: " + String.format("%.1f", accuracy) + "m)");

                boolean updated = updateLocation(userId, currentLat, currentLng, accuracy).join();

                if (updated) {
                    Optional<LocationData> locationOpt = getLastLocation(userId).join();
                    if (locationOpt.isPresent()) {
                        LocationDataDTO dto = convertToDTO(locationOpt.get());
                        Log.d(TAG, "✅ Location DTO created: " + dto.getStatus() + " at " + dto.getFormattedCoordinates());
                        return dto;
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

    //Get current user location as DTO
    public CompletableFuture<LocationDataDTO> getCurrentUserLocation(String userId) {
        return getLastLocation(userId)
                .thenApply(locationOpt -> {
                    if (locationOpt.isPresent()) {
                        LocationDataDTO dto = convertToDTO(locationOpt.get());
                        Log.d(TAG, "📍 Retrieved current location for user " + userId + ": " + dto.getStatus());
                        return dto;
                    } else {
                        Log.w(TAG, "⚠️ No location found for user " + userId + " - returning default");
                        return createDefaultLocationDTO(userId);
                    }
                });
    }

    //Get last location for user
    public CompletableFuture<Optional<LocationData>> getLastLocation(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Verificăm dacă utilizatorul există
                Optional<User> userOpt = userRepository.findById(userId).join();

                if (!userOpt.isPresent()) {
                    Log.w(TAG, "❌ Cannot get location: User not found: " + userId);
                    return Optional.empty();
                }

                Optional<LocationData> locationOpt = locationDataRepository.findByUserId(userId).join();

                if (locationOpt.isPresent()) {
                    Log.d(TAG, "📍 Found location data for user: " + userId);
                } else {
                    Log.d(TAG, "📍 No location data found for user: " + userId);
                }

                return locationOpt;

            } catch (Exception e) {
                Log.e(TAG, "❌ Error getting last location for user " + userId, e);
                return Optional.empty();
            }
        });
    }

    //Update home location for user, prin Kafka-only pipeline
    public CompletableFuture<Boolean> updateHomeLocation(String userId, double latitude, double longitude) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "🏠 Updating home location for user: " + userId);
                Log.d(TAG, "📍 Home coordinates: " + String.format("%.6f, %.6f", latitude, longitude));

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
                    Log.d(TAG, "📝 Updated existing location data with new home coordinates");
                } else {
                    locationData = new LocationData(userOpt.get(), latitude, longitude);
                    locationData.setDeviceId("samsung_galaxy_watch_7_gps");
                    Log.d(TAG, "🆕 Created new location data with home coordinates");
                }

                // Salvează local în Room database
                locationDataRepository.save(locationData).join();
                Log.d(TAG, "💾 Home location saved locally for user: " + userId);

                // ✅ Transmite prin Kafka-only pipeline
                boolean transmitted = dataTransmissionService.transmitData(locationData, userId).join();

                if (transmitted) {
                    Log.d(TAG, "✅ Home location transmitted successfully through Kafka pipeline");
                } else {
                    Log.w(TAG, "⚠️ Home location transmission failed - stored offline for retry");
                }

                Log.d(TAG, "✅ REAL home location processing completed for user " + userId);
                return true;

            } catch (Exception e) {
                Log.e(TAG, "❌ Error updating REAL home location for user " + userId, e);
                return false;
            }
        });
    }

    // ADĂUGAT: Obține numărul de transmisii location în așteptare
    public CompletableFuture<Integer> getPendingLocationTransmissions(String userId) {
        return dataTransmissionService.getPendingTransmissionCount(userId)
                .thenApply(count -> {
                    Log.d(TAG, "📊 Pending location transmissions for user " + userId + ": " + count);
                    return count;
                });
    }

    // ADĂUGAT: Retry failed location transmissions
    public CompletableFuture<Boolean> retryFailedLocationTransmissions(String userId) {
        Log.d(TAG, "🔄 Retrying failed location transmissions for user: " + userId);

        return dataTransmissionService.retryFailedTransmissions(userId)
                .thenApply(success -> {
                    if (success) {
                        Log.d(TAG, "✅ Location transmission retry successful for user: " + userId);
                    } else {
                        Log.w(TAG, "⚠️ Location transmission retry partially failed for user: " + userId);
                    }
                    return success;
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
            dto.setLatitude(locationData.getHomeLatitude());
            dto.setLongitude(locationData.getHomeLongitude());
            dto.setAddress("Home location");
            dto.setTimestamp(LocalDateTime.now());
        }

        dto.setAtHome(locationData.isAtHome());
        dto.setHomeLatitude(locationData.getHomeLatitude());
        dto.setHomeLongitude(locationData.getHomeLongitude());
        dto.setRadiusMeters(locationData.getRadiusMeters());

        return dto;
    }

    private LocationDataDTO createDefaultLocationDTO(String userId) {
        LocationDataDTO dto = new LocationDataDTO();
        dto.setUserId(userId);
        dto.setStatus("UNKNOWN");
        dto.setLatitude(47.0722); // Centrul Oradei ca default
        dto.setLongitude(21.9211);
        dto.setAddress("Oradea, Bihor County, RO (Default)");
        dto.setTimestamp(LocalDateTime.now());
        dto.setAtHome(false);
        dto.setHomeLatitude(47.0722);
        dto.setHomeLongitude(21.9211);
        dto.setRadiusMeters(50.0);

        Log.d(TAG, "🏠 Created default location DTO for user: " + userId);
        return dto;
    }

    /**
     * ✅ ADĂUGAT: Service status pentru debugging
     */
    public String getServiceStatus() {
        StringBuilder status = new StringBuilder();
        status.append("LocationApplicationService (Kafka-Only):\n");
        status.append("- Data Transmission: ✅ Kafka-Only Pipeline\n");
        status.append("- Local Storage: ✅ Room Database\n");
        status.append("- GPS Simulation: ✅ Oradea, RO coordinates\n");
        status.append("- Retry Logic: ✅ Automatic via DataTransmissionService");

        return status.toString();
    }
}