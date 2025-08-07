package com.feri.watchmyparent.mobile.infrastructure.repositories;

import android.util.Log;

import com.feri.watchmyparent.mobile.domain.entities.User;
import com.feri.watchmyparent.mobile.domain.enums.UserType;
import com.feri.watchmyparent.mobile.domain.repositories.UserRepository;
import com.feri.watchmyparent.mobile.infrastructure.database.dao.UserDao;
import com.feri.watchmyparent.mobile.infrastructure.database.entities.UserEntity;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Singleton
public class UserRepositoryImpl implements UserRepository{

    private final UserDao userDao;
    private final Executor executor = Executors.newFixedThreadPool(4);

    @Inject
    public UserRepositoryImpl(UserDao userDao) {
        this.userDao = userDao;
    }

    @Override
    public CompletableFuture<User> save(User user) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                UserEntity entity = convertToEntity(user);
                userDao.insertUser(entity);
                Log.d("UserRepositoryImpl", "User saved successfully: " + user.getIdUser());
                return user;
            } catch (Exception e) {
                Log.e("UserRepositoryImpl", "Error saving user: " + user.getIdUser(), e);
                throw new RuntimeException("Failed to save user", e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Optional<User>> findById(String id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d("UserRepositoryImpl", "🔍 Căutare user cu ID: " + id);

                UserEntity entity = userDao.getUserById(id);

                if (entity != null) {
                    Log.d("UserRepositoryImpl", "✅ User găsit în Room DB: " + id);
                    return Optional.of(convertToDomain(entity));
                } else {
                    Log.d("UserRepositoryImpl", "⚠️ User negăsit în Room DB: " + id);

                    // SOLUȚIE PENTRU DEMO: Creăm un user în memorie dacă e demo-user-id
                    if ("demo-user-id".equals(id)) {
                        Log.d("UserRepositoryImpl", "🔧 Creez user demo în memorie");

                        User demoUser = new User();
                        demoUser.setIdUser("demo-user-id");
                        demoUser.setFirstNameUser("Demo");
                        demoUser.setLastNameUser("User");
                        demoUser.setEmailUser("demo@watchmyparent.com");
                        demoUser.setUserType(UserType.SENIOR);

                        return Optional.of(demoUser);
                    }

                    return Optional.empty();
                }
            } catch (Exception e) {
                Log.e("UserRepositoryImpl", "❌ Error finding user by id: " + id, e);
                e.printStackTrace();
                return Optional.empty();
            }
        }, executor);
    }

//    @Override
//    public CompletableFuture<Optional<User>> findById(String id) {
//        return CompletableFuture.supplyAsync(() -> {
//            try {
//                UserEntity entity = userDao.getUserById(id);
//                return entity != null ? Optional.of(convertToDomain(entity)) : Optional.empty();
//            } catch (Exception e) {
//                Log.e("UserRepositoryImpl", "Error finding user by id: " + id, e);
//                return Optional.empty();
//            }
//        }, executor);
//    }

    @Override
    public CompletableFuture<Optional<User>> findByEmail(String email) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                UserEntity entity = userDao.getUserByEmail(email);
                return entity != null ? Optional.of(convertToDomain(entity)) : Optional.empty();
            } catch (Exception e) {
                Log.e("UserRepositoryImpl", "Error finding user by email: " + email, e);
                return Optional.empty();
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> delete(String id) {
        return CompletableFuture.runAsync(() -> {
            try {
                userDao.deleteUserById(id);
                Log.d("UserRepositoryImpl", "User deleted successfully: " + id);
            } catch (Exception e) {
                Log.e("UserRepositoryImpl", "Error deleting user: " + id, e);
                throw new RuntimeException("Failed to delete user", e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Boolean> existsByEmail(String email) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return userDao.existsByEmail(email);
            } catch (Exception e) {
                Log.e("UserRepositoryImpl", "Error checking if user exists by email: " + email, e);
                return false;
            }
        }, executor);
    }

    private UserEntity convertToEntity(User user) {
        UserEntity entity = new UserEntity();
        entity.idUser = user.getIdUser();
        entity.firstNameUser = user.getFirstNameUser();
        entity.lastNameUser = user.getLastNameUser();
        entity.dateOfBirthUser = user.getDateOfBirthUser();
        entity.userType = user.getUserType();
        entity.addressUser = user.getAddressUser();
        entity.phoneNumberUser = user.getPhoneNumberUser();
        entity.emailUser = user.getEmailUser();
        entity.password = user.getPassword();
        entity.resetPasswordToken = user.getResetPasswordToken();
        entity.resetPasswordTokenExpiry = user.getResetPasswordTokenExpiry();
        entity.accountNonExpired = user.isAccountNonExpired();
        entity.accountNonLocked = user.isAccountNonLocked();
        entity.credentialsNonExpired = user.isCredentialsNonExpired();
        entity.enabled = user.isEnabled();
        entity.createdAt = user.getCreatedAt();
        entity.updatedAt = user.getUpdatedAt();
        return entity;
    }

    private User convertToDomain(UserEntity entity) {
        User user = new User();
        user.setIdUser(entity.idUser);
        user.setFirstNameUser(entity.firstNameUser);
        user.setLastNameUser(entity.lastNameUser);
        user.setDateOfBirthUser(entity.dateOfBirthUser);
        user.setUserType(entity.userType);
        user.setAddressUser(entity.addressUser);
        user.setPhoneNumberUser(entity.phoneNumberUser);
        user.setEmailUser(entity.emailUser);
        user.setPassword(entity.password);
        user.setResetPasswordToken(entity.resetPasswordToken);
        user.setResetPasswordTokenExpiry(entity.resetPasswordTokenExpiry);
        user.setAccountNonExpired(entity.accountNonExpired);
        user.setAccountNonLocked(entity.accountNonLocked);
        user.setCredentialsNonExpired(entity.credentialsNonExpired);
        user.setEnabled(entity.enabled);
        user.setCreatedAt(entity.createdAt);
        user.setUpdatedAt(entity.updatedAt);
        return user;
    }
}
