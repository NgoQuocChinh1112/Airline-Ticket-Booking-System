package com.example.flightbooking.dto;

import jakarta.validation.constraints.NotBlank;

public class HoldSeatRequest {

    @NotBlank
    private String flightId;

    @NotBlank
    private String seatId;

    public String getFlightId() { return flightId; }
    public void setFlightId(String flightId) { this.flightId = flightId; }

    public String getSeatId() { return seatId; }
    public void setSeatId(String seatId) { this.seatId = seatId; }
}
