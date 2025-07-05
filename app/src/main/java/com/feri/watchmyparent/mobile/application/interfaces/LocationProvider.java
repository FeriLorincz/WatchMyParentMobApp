package com.feri.watchmyparent.mobile.application.interfaces;

import com.feri.watchmyparent.mobile.domain.valueobjects.LocationStatus;
import java.util.concurrent.CompletableFuture;

public interface LocationProvider {

    CompletableFuture<LocationStatus> getCurrentLocation();
    CompletableFuture<String> getAddressFromCoordinates(double latitude, double longitude);
    void startLocationUpdates(LocationUpdateListener listener);
    void stopLocationUpdates();

    interface LocationUpdateListener {
        void onLocationUpdate(LocationStatus location);
        void onError(Exception error);
    }
}
