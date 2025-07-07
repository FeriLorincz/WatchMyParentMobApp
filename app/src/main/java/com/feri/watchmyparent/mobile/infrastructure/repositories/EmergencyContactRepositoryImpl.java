package com.feri.watchmyparent.mobile.infrastructure.repositories;

import android.util.Log;

import com.feri.watchmyparent.mobile.domain.entities.EmergencyContact;
import com.feri.watchmyparent.mobile.domain.entities.User;
import com.feri.watchmyparent.mobile.domain.repositories.EmergencyContactRepository;
import com.feri.watchmyparent.mobile.infrastructure.database.dao.EmergencyContactDao;
import com.feri.watchmyparent.mobile.infrastructure.database.entities.EmergencyContactEntity;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Singleton
public class EmergencyContactRepositoryImpl implements EmergencyContactRepository{

    private final EmergencyContactDao emergencyContactDao;
    private final Executor executor = Executors.newFixedThreadPool(4);

    @Inject
    public EmergencyContactRepositoryImpl(EmergencyContactDao emergencyContactDao) {
        this.emergencyContactDao = emergencyContactDao;
    }

    @Override
    public CompletableFuture<EmergencyContact> save(EmergencyContact emergencyContact) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                EmergencyContactEntity entity = convertToEntity(emergencyContact);
                emergencyContactDao.insertEmergencyContact(entity);
                Log.d("EmergencyContactRepositoryImpl", "Emergency contact saved: " + emergencyContact.getIdContact() + " for user " + emergencyContact.getUser().getIdUser());
                return emergencyContact;
            } catch (Exception e) {
                Log.e("EmergencyContactRepositoryImpl", "Error saving emergency contact", e);
                throw new RuntimeException("Failed to save emergency contact", e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Optional<EmergencyContact>> findById(String id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                EmergencyContactEntity entity = emergencyContactDao.getEmergencyContactById(id);
                return entity != null ? Optional.of(convertToDomain(entity)) : Optional.empty();
            } catch (Exception e) {
                Log.e("EmergencyContactRepositoryImpl", "Error finding emergency contact by id: " + id, e);
                return Optional.empty();
            }
        }, executor);
    }

    @Override
    public CompletableFuture<List<EmergencyContact>> findByUserId(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<EmergencyContactEntity> entities = emergencyContactDao.getEmergencyContactsByUser(userId);
                return entities.stream()
                        .map(this::convertToDomain)
                        .collect(Collectors.toList());
            } catch (Exception e) {
                Log.e("EmergencyContactRepositoryImpl", "Error finding emergency contacts by user: " + userId, e);
                throw new RuntimeException("Failed to find emergency contacts", e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> delete(String id) {
        return CompletableFuture.runAsync(() -> {
            try {
                emergencyContactDao.deleteEmergencyContactById(id);
                Log.d("EmergencyContactRepositoryImpl", "Emergency contact deleted: " + id);
            } catch (Exception e) {
                Log.e("EmergencyContactRepositoryImpl", "Error deleting emergency contact: " + id, e);
                throw new RuntimeException("Failed to delete emergency contact", e);
            }
        }, executor);
    }

    private EmergencyContactEntity convertToEntity(EmergencyContact contact) {
        EmergencyContactEntity entity = new EmergencyContactEntity();
        entity.idContact = contact.getIdContact();
        entity.userId = contact.getUser().getIdUser();
        entity.addressContact = contact.getAddressContact();
        entity.firstNameContact = contact.getFirstNameContact();
        entity.lastNameContact = contact.getLastNameContact();
        entity.relationship = contact.getRelationship();
        entity.phoneNumberContact = contact.getPhoneNumberContact();
        entity.emailContact = contact.getEmailContact();
        entity.createdAt = contact.getCreatedAt();
        entity.updatedAt = contact.getUpdatedAt();
        return entity;
    }

    private EmergencyContact convertToDomain(EmergencyContactEntity entity) {
        // Create basic user object
        User user = new User();
        user.setIdUser(entity.userId);

        EmergencyContact contact = new EmergencyContact();
        contact.setIdContact(entity.idContact);
        contact.setUser(user);
        contact.setAddressContact(entity.addressContact);
        contact.setFirstNameContact(entity.firstNameContact);
        contact.setLastNameContact(entity.lastNameContact);
        contact.setRelationship(entity.relationship);
        contact.setPhoneNumberContact(entity.phoneNumberContact);
        contact.setEmailContact(entity.emailContact);
        contact.setCreatedAt(entity.createdAt);
        contact.setUpdatedAt(entity.updatedAt);

        return contact;
    }
}
