package com.example.flightbooking.dto;

public class SeatDto {
    private String id;
    private String seatNumber;
    private String status;

    public SeatDto(String id, String seatNumber, String status) {
        this.id = id;
        this.seatNumber = seatNumber;
        this.status = status;
    }

    public String getId() { return id; }
    public String getSeatNumber() { return seatNumber; }
    public String getStatus() { return status; }
}
