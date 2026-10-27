package com.example.flightbooking.dto;

import jakarta.validation.constraints.NotBlank;

public class CreatePaymentRequest {

    @NotBlank(message = "bookingId không được để trống")
    private String bookingId;

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }
}