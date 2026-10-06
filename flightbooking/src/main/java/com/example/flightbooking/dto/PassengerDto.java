package com.example.flightbooking.dto;

import jakarta.validation.constraints.NotBlank;

public class PassengerDto {

    @NotBlank
    private String fullName;

    @NotBlank
    private String passportNumber;

    // Dạng "YYYY-MM-DD"
    @NotBlank
    private String dob;

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPassportNumber() { return passportNumber; }
    public void setPassportNumber(String passportNumber) { this.passportNumber = passportNumber; }

    public String getDob() { return dob; }
    public void setDob(String dob) { this.dob = dob; }
}
