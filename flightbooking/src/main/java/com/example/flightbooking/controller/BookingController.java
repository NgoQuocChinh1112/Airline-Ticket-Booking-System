package com.example.flightbooking.controller;

import com.example.flightbooking.dto.BookingResponse;
import com.example.flightbooking.dto.CreateBookingRequest;
import com.example.flightbooking.dto.HoldSeatRequest;
import com.example.flightbooking.entity.Booking;
import com.example.flightbooking.entity.FlightSeat;
import com.example.flightbooking.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/hold-seat")
    public FlightSeat holdSeat(Authentication authentication, @Valid @RequestBody HoldSeatRequest req) {
        String userId = (String) authentication.getPrincipal();
        return bookingService.holdSeat(userId, req);
    }

    @PostMapping
    public BookingResponse createBooking(Authentication authentication, @Valid @RequestBody CreateBookingRequest req) {
        String userId = (String) authentication.getPrincipal();
        Booking booking = bookingService.createBooking(userId, req);
        return new BookingResponse(booking);
    }

    @PostMapping("/{id}/confirm")
    public BookingResponse confirmBooking(@PathVariable String id) {
        Booking booking = bookingService.confirmBooking(UUID.fromString(id));
        return new BookingResponse(booking);
    }

    @PostMapping("/{id}/cancel")
    public void cancelBooking(@PathVariable String id) {
        bookingService.cancelBooking(UUID.fromString(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelBookingDelete(@PathVariable String id) {
        bookingService.cancelBooking(UUID.fromString(id));
        return ResponseEntity.noContent().build();
    }
}