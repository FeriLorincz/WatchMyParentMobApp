package com.feri.watchmyparent.mobile.domain.repositories;

import com.feri.watchmyparent.mobile.domain.entities.LocationData;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface LocationDataRepository {
    CompletableFuture<LocationData> save(LocationData locationData);
    CompletableFuture<Optional<LocationData>> findByUserId(String userId);
    CompletableFuture<Void> delete(String id);
}
