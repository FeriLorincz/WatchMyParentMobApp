package com.feri.watchmyparent.mobile.domain.valueobjects;

import androidx.room.Ignore;
import java.util.Objects;

public class AddressUser {

    private String cityUser;
    private String villageUser;
    private String streetUser;
    private String numberUser;
    private String buildingUser;
    private String staircaseUser;
    private String floorUser;
    private String apartmentUser;
    private String postalCodeUser;
    private String countyUser;
    private String stateUser;
    private String countryUser;
    private String nearestAmbulanceCityUser;

    // Default Constructors
    public AddressUser() {}

    // Custom constructor - marked with @Ignore so Room won't use it
    @Ignore
    public AddressUser(String cityUser, String streetUser, String numberUser, String countryUser) {
        this.cityUser = cityUser;
        this.streetUser = streetUser;
        this.numberUser = numberUser;
        this.countryUser = countryUser;
    }

    // Getters and Setters
    public String getCityUser() { return cityUser; }
    public void setCityUser(String cityUser) { this.cityUser = cityUser; }

    public String getVillageUser() { return villageUser; }
    public void setVillageUser(String villageUser) { this.villageUser = villageUser; }

    public String getStreetUser() { return streetUser; }
    public void setStreetUser(String streetUser) { this.streetUser = streetUser; }

    public String getNumberUser() { return numberUser; }
    public void setNumberUser(String numberUser) { this.numberUser = numberUser; }

    public String getBuildingUser() { return buildingUser; }
    public void setBuildingUser(String buildingUser) { this.buildingUser = buildingUser; }

    public String getStaircaseUser() { return staircaseUser; }
    public void setStaircaseUser(String staircaseUser) { this.staircaseUser = staircaseUser; }

    public String getFloorUser() { return floorUser; }
    public void setFloorUser(String floorUser) { this.floorUser = floorUser; }

    public String getApartmentUser() { return apartmentUser; }
    public void setApartmentUser(String apartmentUser) { this.apartmentUser = apartmentUser; }

    public String getPostalCodeUser() { return postalCodeUser; }
    public void setPostalCodeUser(String postalCodeUser) { this.postalCodeUser = postalCodeUser; }

    public String getCountyUser() { return countyUser; }
    public void setCountyUser(String countyUser) { this.countyUser = countyUser; }

    public String getStateUser() { return stateUser; }
    public void setStateUser(String stateUser) { this.stateUser = stateUser; }

    public String getCountryUser() { return countryUser; }
    public void setCountryUser(String countryUser) { this.countryUser = countryUser; }

    public String getNearestAmbulanceCityUser() { return nearestAmbulanceCityUser; }
    public void setNearestAmbulanceCityUser(String nearestAmbulanceCityUser) { this.nearestAmbulanceCityUser = nearestAmbulanceCityUser; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddressUser that = (AddressUser) o;
        return Objects.equals(cityUser, that.cityUser) &&
                Objects.equals(streetUser, that.streetUser) &&
                Objects.equals(numberUser, that.numberUser) &&
                Objects.equals(countryUser, that.countryUser);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cityUser, streetUser, numberUser, countryUser);
    }
}
