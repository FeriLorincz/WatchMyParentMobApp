package com.feri.watchmyparent.mobile.domain.repositories;

import com.feri.watchmyparent.mobile.domain.entities.MedicalProfile;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface MedicalProfileRepository {

    CompletableFuture<MedicalProfile> save(MedicalProfile medicalProfile);
    CompletableFuture<Optional<MedicalProfile>> findById(String id);
    CompletableFuture<Optional<MedicalProfile>> findByUserId(String userId);
    CompletableFuture<Void> delete(String id);
}
