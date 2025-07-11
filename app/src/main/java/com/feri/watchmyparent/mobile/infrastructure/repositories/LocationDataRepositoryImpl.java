package com.feri.watchmyparent.mobile.infrastructure.repositories;

import android.util.Log;

import com.feri.watchmyparent.mobile.domain.entities.LocationData;
import com.feri.watchmyparent.mobile.domain.entities.User;
import com.feri.watchmyparent.mobile.domain.repositories.LocationDataRepository;
import com.feri.watchmyparent.mobile.infrastructure.database.dao.LocationDataDao;
import com.feri.watchmyparent.mobile.infrastructure.database.entities.LocationDataEntity;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Singleton
public class LocationDataRepositoryImpl implements LocationDataRepository {

    private final LocationDataDao locationDataDao;
    private final Executor executor = Executors.newFixedThreadPool(4);

    @Inject
    public LocationDataRepositoryImpl(LocationDataDao locationDataDao) {
        this.locationDataDao = locationDataDao;
    }

    @Override
    public CompletableFuture<LocationData> save(LocationData locationData) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                LocationDataEntity entity = convertToEntity(locationData);
                locationDataDao.insertLocationData(entity);
                Log.d("LocationDataRepository", "Location data saved for user: " + locationData.getUser().getIdUser());
                return locationData;
            } catch (Exception e) {
                Log.e("LocationDataRepository", "Error saving location data", e);
                throw new RuntimeException("Failed to save location data", e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Optional<LocationData>> findByUserId(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                LocationDataEntity entity = locationDataDao.getLocationDataByUser(userId);
                return entity != null ? Optional.of(convertToDomain(entity)) : Optional.empty();
            } catch (Exception e) {
                Log.e("LocationDataRepository", "Error finding location data by user: " + userId, e);
                return Optional.empty();
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> delete(String id) {
        return CompletableFuture.runAsync(() -> {
            try {
                locationDataDao.deleteLocationDataById(id);
                Log.d("LocationDataRepository", "Location data deleted: " + id);
            } catch (Exception e) {
                Log.e("LocationDataRepository", "Error deleting location data", e);
                throw new RuntimeException("Failed to delete location data", e);
            }
        }, executor);
    }

    private LocationDataEntity convertToEntity(LocationData locationData) {
        LocationDataEntity entity = new LocationDataEntity();
        entity.idLocationData = locationData.getIdLocationData();
        entity.userId = locationData.getUser().getIdUser();
        entity.locationStatus = locationData.getLocationStatus();
        entity.homeLatitude = locationData.getHomeLatitude();
        entity.homeLongitude = locationData.getHomeLongitude();
        entity.radiusMeters = locationData.getRadiusMeters();
        entity.deviceId = locationData.getDeviceId();
        entity.createdAt = locationData.getCreatedAt();
        entity.updatedAt = locationData.getUpdatedAt();
        return entity;
    }

    private LocationData convertToDomain(LocationDataEntity entity) {
        // Create basic user object
        User user = new User();
        user.setIdUser(entity.userId);

        LocationData locationData = new LocationData();
        locationData.setIdLocationData(entity.idLocationData);
        locationData.setUser(user);
        locationData.setLocationStatus(entity.locationStatus);
        locationData.setHomeLatitude(entity.homeLatitude);
        locationData.setHomeLongitude(entity.homeLongitude);
        locationData.setRadiusMeters(entity.radiusMeters);
        locationData.setDeviceId(entity.deviceId);
        locationData.setCreatedAt(entity.createdAt);
        locationData.setUpdatedAt(entity.updatedAt);

        return locationData;
    }
}
