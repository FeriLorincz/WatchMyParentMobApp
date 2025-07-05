package com.feri.watchmyparent.mobile.domain.valueobjects;

import java.util.Objects;

public class AddressContact {

    private String cityContact;
    private String villageContact;
    private String streetContact;
    private String numberContact;
    private String buildingContact;
    private String staircaseContact;
    private String floorContact;
    private String apartmentContact;
    private String postalCodeContact;
    private String countyContact;
    private String stateContact;
    private String countryContact;

    // Constructors
    public AddressContact() {}

    public AddressContact(String cityContact, String streetContact, String numberContact, String countryContact) {
        this.cityContact = cityContact;
        this.streetContact = streetContact;
        this.numberContact = numberContact;
        this.countryContact = countryContact;
    }

    // Getters and Setters
    public String getCityContact() { return cityContact; }
    public void setCityContact(String cityContact) { this.cityContact = cityContact; }

    public String getVillageContact() { return villageContact; }
    public void setVillageContact(String villageContact) { this.villageContact = villageContact; }

    public String getStreetContact() { return streetContact; }
    public void setStreetContact(String streetContact) { this.streetContact = streetContact; }

    public String getNumberContact() { return numberContact; }
    public void setNumberContact(String numberContact) { this.numberContact = numberContact; }

    public String getBuildingContact() { return buildingContact; }
    public void setBuildingContact(String buildingContact) { this.buildingContact = buildingContact; }

    public String getStaircaseContact() { return staircaseContact; }
    public void setStaircaseContact(String staircaseContact) { this.staircaseContact = staircaseContact; }

    public String getFloorContact() { return floorContact; }
    public void setFloorContact(String floorContact) { this.floorContact = floorContact; }

    public String getApartmentContact() { return apartmentContact; }
    public void setApartmentContact(String apartmentContact) { this.apartmentContact = apartmentContact; }

    public String getPostalCodeContact() { return postalCodeContact; }
    public void setPostalCodeContact(String postalCodeContact) { this.postalCodeContact = postalCodeContact; }

    public String getCountyContact() { return countyContact; }
    public void setCountyContact(String countyContact) { this.countyContact = countyContact; }

    public String getStateContact() { return stateContact; }
    public void setStateContact(String stateContact) { this.stateContact = stateContact; }

    public String getCountryContact() { return countryContact; }
    public void setCountryContact(String countryContact) { this.countryContact = countryContact; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddressContact that = (AddressContact) o;
        return Objects.equals(cityContact, that.cityContact) &&
                Objects.equals(streetContact, that.streetContact) &&
                Objects.equals(numberContact, that.numberContact) &&
                Objects.equals(countryContact, that.countryContact);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cityContact, streetContact, numberContact, countryContact);
    }
}
