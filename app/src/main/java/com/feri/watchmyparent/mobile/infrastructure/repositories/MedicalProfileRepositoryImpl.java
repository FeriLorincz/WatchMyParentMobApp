package com.feri.watchmyparent.mobile.infrastructure.repositories;

import com.feri.watchmyparent.mobile.domain.entities.MedicalProfile;
import com.feri.watchmyparent.mobile.domain.entities.User;
import com.feri.watchmyparent.mobile.domain.repositories.MedicalProfileRepository;
import com.feri.watchmyparent.mobile.infrastructure.database.dao.MedicalProfileDao;
import com.feri.watchmyparent.mobile.infrastructure.database.entities.MedicalProfileEntity;
import timber.log.Timber;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Singleton
public class MedicalProfileRepositoryImpl implements MedicalProfileRepository {

    private final MedicalProfileDao medicalProfileDao;
    private final Executor executor = Executors.newFixedThreadPool(4);

    @Inject
    public MedicalProfileRepositoryImpl(MedicalProfileDao medicalProfileDao) {
        this.medicalProfileDao = medicalProfileDao;
    }

    @Override
    public CompletableFuture<MedicalProfile> save(MedicalProfile medicalProfile) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                MedicalProfileEntity entity = convertToEntity(medicalProfile);
                medicalProfileDao.insertMedicalProfile(entity);
                Timber.d("Medical profile saved: %s for user %s",
                        medicalProfile.getIdMedicalProfile(), medicalProfile.getUser().getIdUser());
                return medicalProfile;
            } catch (Exception e) {
                Timber.e(e, "Error saving medical profile");
                throw new RuntimeException("Failed to save medical profile", e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Optional<MedicalProfile>> findById(String id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                MedicalProfileEntity entity = medicalProfileDao.getMedicalProfileById(id);
                return entity != null ? Optional.of(convertToDomain(entity)) : Optional.empty();
            } catch (Exception e) {
                Timber.e(e, "Error finding medical profile by id: %s", id);
                return Optional.empty();
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Optional<MedicalProfile>> findByUserId(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                MedicalProfileEntity entity = medicalProfileDao.getMedicalProfileByUser(userId);
                return entity != null ? Optional.of(convertToDomain(entity)) : Optional.empty();
            } catch (Exception e) {
                Timber.e(e, "Error finding medical profile by user: %s", userId);
                return Optional.empty();
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> delete(String id) {
        return CompletableFuture.runAsync(() -> {
            try {
                medicalProfileDao.deleteMedicalProfileById(id);
                Timber.d("Medical profile deleted: %s", id);
            } catch (Exception e) {
                Timber.e(e, "Error deleting medical profile: %s", id);
                throw new RuntimeException("Failed to delete medical profile", e);
            }
        }, executor);
    }

    private MedicalProfileEntity convertToEntity(MedicalProfile profile) {
        MedicalProfileEntity entity = new MedicalProfileEntity();
        entity.idMedicalProfile = profile.getIdMedicalProfile();
        entity.userId = profile.getUser().getIdUser();
        entity.currentDiseases = profile.getCurrentDiseases();
        entity.sensorModifyingConditions = profile.getSensorModifyingConditions();
        entity.hasAthleticHistory = profile.isHasAthleticHistory();
        entity.athleticHistoryDetails = profile.getAthleticHistoryDetails();
        entity.medications = profile.getMedications();
        entity.gdprConsent = profile.isGdprConsent();
        entity.disclaimerAccepted = profile.isDisclaimerAccepted();
        entity.emergencyEntryPermission = profile.isEmergencyEntryPermission();
        entity.createdAt = profile.getCreatedAt();
        entity.updatedAt = profile.getUpdatedAt();
        return entity;
    }

    private MedicalProfile convertToDomain(MedicalProfileEntity entity) {
        // Create basic user object
        User user = new User();
        user.setIdUser(entity.userId);

        MedicalProfile profile = new MedicalProfile();
        profile.setIdMedicalProfile(entity.idMedicalProfile);
        profile.setUser(user);
        profile.setCurrentDiseases(entity.currentDiseases);
        profile.setSensorModifyingConditions(entity.sensorModifyingConditions);
        profile.setHasAthleticHistory(entity.hasAthleticHistory);
        profile.setAthleticHistoryDetails(entity.athleticHistoryDetails);
        profile.setMedications(entity.medications);
        profile.setGdprConsent(entity.gdprConsent);
        profile.setDisclaimerAccepted(entity.disclaimerAccepted);
        profile.setEmergencyEntryPermission(entity.emergencyEntryPermission);
        profile.setCreatedAt(entity.createdAt);
        profile.setUpdatedAt(entity.updatedAt);

        return profile;
    }
}
