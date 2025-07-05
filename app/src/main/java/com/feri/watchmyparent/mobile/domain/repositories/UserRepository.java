package com.feri.watchmyparent.mobile.domain.repositories;

import com.feri.watchmyparent.mobile.domain.entities.User;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface UserRepository {
    CompletableFuture<User> save(User user);
    CompletableFuture<Optional<User>> findById(String id);
    CompletableFuture<Optional<User>> findByEmail(String email);
    CompletableFuture<Void> delete(String id);
    CompletableFuture<Boolean> existsByEmail(String email);
}
