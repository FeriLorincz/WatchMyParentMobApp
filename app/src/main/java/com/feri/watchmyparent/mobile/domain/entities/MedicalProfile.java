package com.feri.watchmyparent.mobile.domain.entities;

import com.feri.watchmyparent.mobile.domain.valueobjects.Medication;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MedicalProfile {

    private String idMedicalProfile;
    private User user;
    private Set<String> currentDiseases = new HashSet<>();
    private Set<String> sensorModifyingConditions = new HashSet<>();
    private boolean hasAthleticHistory;
    private String athleticHistoryDetails;
    private Set<Medication> medications = new HashSet<>();
    private boolean gdprConsent;
    private boolean disclaimerAccepted;
    private boolean emergencyEntryPermission;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public MedicalProfile() {
        this.idMedicalProfile = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public MedicalProfile(User user) {
        this();
        this.user = user;
    }

    // Business Logic Methods
    public void addDisease(String disease) {
        this.currentDiseases.add(disease);
        this.updatedAt = LocalDateTime.now();
    }

    public void removeDisease(String disease) {
        this.currentDiseases.remove(disease);
        this.updatedAt = LocalDateTime.now();
    }

    public void addMedication(Medication medication) {
        this.medications.add(medication);
        this.updatedAt = LocalDateTime.now();
    }

    public void removeMedication(Medication medication) {
        this.medications.remove(medication);
        this.updatedAt = LocalDateTime.now();
    }

    public void updateAthleticHistory(boolean hasHistory, String details) {
        this.hasAthleticHistory = hasHistory;
        this.athleticHistoryDetails = details;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateConsents(boolean gdprConsent, boolean disclaimerAccepted, boolean emergencyEntryPermission) {
        this.gdprConsent = gdprConsent;
        this.disclaimerAccepted = disclaimerAccepted;
        this.emergencyEntryPermission = emergencyEntryPermission;
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getIdMedicalProfile() { return idMedicalProfile; }
    public void setIdMedicalProfile(String idMedicalProfile) { this.idMedicalProfile = idMedicalProfile; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Set<String> getCurrentDiseases() { return currentDiseases; }
    public void setCurrentDiseases(Set<String> currentDiseases) { this.currentDiseases = currentDiseases; }

    public Set<String> getSensorModifyingConditions() { return sensorModifyingConditions; }
    public void setSensorModifyingConditions(Set<String> sensorModifyingConditions) { this.sensorModifyingConditions = sensorModifyingConditions; }

    public boolean isHasAthleticHistory() { return hasAthleticHistory; }
    public void setHasAthleticHistory(boolean hasAthleticHistory) { this.hasAthleticHistory = hasAthleticHistory; }

    public String getAthleticHistoryDetails() { return athleticHistoryDetails; }
    public void setAthleticHistoryDetails(String athleticHistoryDetails) { this.athleticHistoryDetails = athleticHistoryDetails; }

    public Set<Medication> getMedications() { return medications; }
    public void setMedications(Set<Medication> medications) { this.medications = medications; }

    public boolean isGdprConsent() { return gdprConsent; }
    public void setGdprConsent(boolean gdprConsent) { this.gdprConsent = gdprConsent; }

    public boolean isDisclaimerAccepted() { return disclaimerAccepted; }
    public void setDisclaimerAccepted(boolean disclaimerAccepted) { this.disclaimerAccepted = disclaimerAccepted; }

    public boolean isEmergencyEntryPermission() { return emergencyEntryPermission; }
    public void setEmergencyEntryPermission(boolean emergencyEntryPermission) { this.emergencyEntryPermission = emergencyEntryPermission; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
