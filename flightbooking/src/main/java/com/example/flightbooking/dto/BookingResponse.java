package com.example.flightbooking.dto;

import com.example.flightbooking.entity.Booking;
import com.example.flightbooking.entity.BookingStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class BookingResponse {
    private UUID id;
    private BookingStatus status;
    private BigDecimal totalPrice;
    private Instant createdAt;

    public BookingResponse(Booking booking) {
        this.id = booking.getId();
        this.status = booking.getStatus();
        this.totalPrice = booking.getTotalPrice();
        this.createdAt = booking.getCreatedAt();
    }

    // Getters
    public UUID getId() { return id; }
    public BookingStatus getStatus() { return status; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public Instant getCreatedAt() { return createdAt; }
}