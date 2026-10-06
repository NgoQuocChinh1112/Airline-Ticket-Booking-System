package com.example.flightbooking.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class CreateBookingRequest {

    @NotBlank
    private String flightId;

    @NotEmpty
    private List<String> seatIds;

    @NotEmpty
    @Valid
    private List<PassengerDto> passengers;

    public String getFlightId() { return flightId; }
    public void setFlightId(String flightId) { this.flightId = flightId; }

    public List<String> getSeatIds() { return seatIds; }
    public void setSeatIds(List<String> seatIds) { this.seatIds = seatIds; }

    public List<PassengerDto> getPassengers() { return passengers; }
    public void setPassengers(List<PassengerDto> passengers) { this.passengers = passengers; }
}
