package com.feri.watchmyparent.mobile.domain.valueobjects;

import java.time.LocalDate;
import java.util.Objects;

public class Medication {

    private String idMedication;
    private String medicationName;
    private LocalDate medicationStartDate;
    private String medicationDosage;

    public Medication() {}

    public Medication(String medicationName, String medicationDosage, LocalDate medicationStartDate) {
        this.medicationName = medicationName;
        this.medicationDosage = medicationDosage;
        this.medicationStartDate = medicationStartDate;
    }

    // Getters and Setters
    public String getIdMedication() { return idMedication; }
    public void setIdMedication(String idMedication) { this.idMedication = idMedication; }

    public String getMedicationName() { return medicationName; }
    public void setMedicationName(String medicationName) { this.medicationName = medicationName; }

    public LocalDate getMedicationStartDate() { return medicationStartDate; }
    public void setMedicationStartDate(LocalDate medicationStartDate) { this.medicationStartDate = medicationStartDate; }

    public String getMedicationDosage() { return medicationDosage; }
    public void setMedicationDosage(String medicationDosage) { this.medicationDosage = medicationDosage; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Medication that = (Medication) o;
        return Objects.equals(medicationName, that.medicationName) &&
                Objects.equals(medicationStartDate, that.medicationStartDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(medicationName, medicationStartDate);
    }
}
