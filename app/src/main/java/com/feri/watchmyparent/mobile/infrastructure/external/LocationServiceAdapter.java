package com.feri.watchmyparent.mobile.infrastructure.external;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Looper;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.*;
import com.feri.watchmyparent.mobile.domain.valueobjects.LocationStatus;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class LocationServiceAdapter {

    private static final String TAG = "LocationServiceAdapter";
    private final Context context;
    private final FusedLocationProviderClient fusedLocationClient;
    private final Geocoder geocoder;
    private LocationCallback locationCallback;

    public LocationServiceAdapter(Context context) {
        this.context = context;
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        this.geocoder = new Geocoder(context, Locale.getDefault());
    }

    public CompletableFuture<LocationStatus> getCurrentLocation() {
        CompletableFuture<LocationStatus> future = new CompletableFuture<>();

        if (!hasLocationPermission()) {
            future.completeExceptionally(new SecurityException("Location permission not granted"));
            return future;
        }

        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            processLocation(location, future);
                        } else {
                            requestNewLocationData(future);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to get last known location", e);
                        future.completeExceptionally(e);
                    });
        } catch (SecurityException e) {
            future.completeExceptionally(e);
        }

        return future;
    }

    private void requestNewLocationData(CompletableFuture<LocationStatus> future) {
        try {
            LocationRequest locationRequest = new LocationRequest.Builder(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    10000L // interval in milliseconds
            )
                    .setMinUpdateIntervalMillis(5000L) // fastest interval
                    .setMaxUpdates(1) // single update
                    .build();

            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    Location location = locationResult.getLastLocation();
                    if (location != null) {
                        processLocation(location, future);
                    } else {
                        future.completeExceptionally(new RuntimeException("Unable to get current location"));
                    }
                    fusedLocationClient.removeLocationUpdates(this);
                }
            };

            fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
            );

        } catch (SecurityException e) {
            future.completeExceptionally(e);
        }
    }

    private void processLocation(Location location, CompletableFuture<LocationStatus> future) {
        try {
            getAddressFromLocation(location.getLatitude(), location.getLongitude())
                    .thenAccept(address -> {
                        LocationStatus locationStatus = new LocationStatus(
                                "AWAY", // Will be determined by the domain service
                                location.getLatitude(),
                                location.getLongitude(),
                                address
                        );
                        future.complete(locationStatus);
                    })
                    .exceptionally(throwable -> {
                        Log.e(TAG, "Error getting address from location", throwable);
                        // Fallback cu coordonate dacă adresa nu poate fi obținută
                        LocationStatus locationStatus = new LocationStatus(
                                "AWAY",
                                location.getLatitude(),
                                location.getLongitude(),
                                "Address not available"
                        );
                        future.complete(locationStatus);
                        return null;
                    });

        } catch (Exception e) {
            Log.e(TAG, "Error processing location", e);
            future.completeExceptionally(e);
        }
    }

    public CompletableFuture<String> getAddressFromLocation(double latitude, double longitude) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    StringBuilder addressBuilder = new StringBuilder();

                    if (address.getThoroughfare() != null) {
                        addressBuilder.append(address.getThoroughfare()).append(" ");
                    }
                    if (address.getSubThoroughfare() != null) {
                        addressBuilder.append(address.getSubThoroughfare()).append(", ");
                    }
                    if (address.getLocality() != null) {
                        addressBuilder.append(address.getLocality()).append(", ");
                    }
                    if (address.getCountryName() != null) {
                        addressBuilder.append(address.getCountryName());
                    }

                    return addressBuilder.toString().trim().replaceAll(",$", "");
                }
                return "Address not found";
            } catch (IOException e) {
                Log.e(TAG, "Error getting address from coordinates", e);
                return "Unknown location";
            }
        });
    }

    public void startLocationUpdates(LocationUpdateCallback callback) {
        if (!hasLocationPermission()) {
            callback.onError(new SecurityException("Location permission not granted"));
            return;
        }

        try {
            // Use LocationRequest.Builder for newer API
            LocationRequest locationRequest = new LocationRequest.Builder(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    600000L // 10 minutes
            )
                    .setMinUpdateIntervalMillis(300000L) // 5 minutes
                    .build();

            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    Location location = locationResult.getLastLocation();
                    if (location != null) {
                        getAddressFromLocation(location.getLatitude(), location.getLongitude())
                                .thenAccept(address -> {
                                    LocationStatus locationStatus = new LocationStatus(
                                            "AWAY",
                                            location.getLatitude(),
                                            location.getLongitude(),
                                            address
                                    );
                                    callback.onLocationUpdate(locationStatus);
                                });
                    }
                }
            };

            fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
            );

        } catch (SecurityException e) {
            callback.onError(e);
        }
    }

    public void stopLocationUpdates() {
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            locationCallback = null;
        }
    }

    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;
    }

    public interface LocationUpdateCallback {
        void onLocationUpdate(LocationStatus locationStatus);
        void onError(Exception error);
    }
}