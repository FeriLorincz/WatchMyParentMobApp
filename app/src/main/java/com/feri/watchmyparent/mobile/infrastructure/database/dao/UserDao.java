package com.feri.watchmyparent.mobile.infrastructure.database.dao;

import androidx.room.*;
import com.feri.watchmyparent.mobile.infrastructure.database.entities.UserEntity;
import java.util.List;

@Dao
public interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertUser(UserEntity user);

    @Update
    int updateUser(UserEntity user);

    @Delete
    int deleteUser(UserEntity user);

    @Query("SELECT * FROM users WHERE idUser = :userId")
    UserEntity getUserById(String userId);

    @Query("SELECT * FROM users WHERE emailUser = :email")
    UserEntity getUserByEmail(String email);

    @Query("SELECT COUNT(*) > 0 FROM users WHERE emailUser = :email")
    boolean existsByEmail(String email);

    @Query("SELECT * FROM users")
    List<UserEntity> getAllUsers();

    @Query("DELETE FROM users WHERE idUser = :userId")
    int deleteUserById(String userId);
}
