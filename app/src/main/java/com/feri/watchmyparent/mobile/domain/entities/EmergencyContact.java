package com.feri.watchmyparent.mobile.domain.entities;

import com.feri.watchmyparent.mobile.domain.valueobjects.AddressContact;

import java.time.LocalDateTime;
import java.util.UUID;

public class EmergencyContact {

    private String idContact;
    private User user;
    private AddressContact addressContact;
    private String firstNameContact;
    private String lastNameContact;
    private String relationship;
    private String phoneNumberContact;
    private String emailContact;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public EmergencyContact() {
        this.idContact = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public EmergencyContact(String firstNameContact, String lastNameContact, String relationship, String phoneNumberContact) {
        this();
        this.firstNameContact = firstNameContact;
        this.lastNameContact = lastNameContact;
        this.relationship = relationship;
        this.phoneNumberContact = phoneNumberContact;
    }

    // Business Logic Methods
    public void updateContactInfo(String firstName, String lastName, String relationship, String phoneNumber, String email) {
        this.firstNameContact = firstName;
        this.lastNameContact = lastName;
        this.relationship = relationship;
        this.phoneNumberContact = phoneNumber;
        this.emailContact = email;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateAddress(AddressContact address) {
        this.addressContact = address;
        this.updatedAt = LocalDateTime.now();
    }

    public String getFullName() {
        return firstNameContact + " " + lastNameContact;
    }

    // Getters and Setters
    public String getIdContact() { return idContact; }
    public void setIdContact(String idContact) { this.idContact = idContact; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public AddressContact getAddressContact() { return addressContact; }
    public void setAddressContact(AddressContact addressContact) { this.addressContact = addressContact; }

    public String getFirstNameContact() { return firstNameContact; }
    public void setFirstNameContact(String firstNameContact) { this.firstNameContact = firstNameContact; }

    public String getLastNameContact() { return lastNameContact; }
    public void setLastNameContact(String lastNameContact) { this.lastNameContact = lastNameContact; }

    public String getRelationship() { return relationship; }
    public void setRelationship(String relationship) { this.relationship = relationship; }

    public String getPhoneNumberContact() { return phoneNumberContact; }
    public void setPhoneNumberContact(String phoneNumberContact) { this.phoneNumberContact = phoneNumberContact; }

    public String getEmailContact() { return emailContact; }
    public void setEmailContact(String emailContact) { this.emailContact = emailContact; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
