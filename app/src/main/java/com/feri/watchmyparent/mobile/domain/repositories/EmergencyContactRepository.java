package com.feri.watchmyparent.mobile.domain.repositories;

import com.feri.watchmyparent.mobile.domain.entities.EmergencyContact;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface EmergencyContactRepository {

    CompletableFuture<EmergencyContact> save(EmergencyContact emergencyContact);
    CompletableFuture<Optional<EmergencyContact>> findById(String id);
    CompletableFuture<List<EmergencyContact>> findByUserId(String userId);
    CompletableFuture<Void> delete(String id);
}
