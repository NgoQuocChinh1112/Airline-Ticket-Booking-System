package com.example.flightbooking.controller;

import com.example.flightbooking.dto.CreateBookingRequest;
import com.example.flightbooking.dto.HoldSeatRequest;
import com.example.flightbooking.entity.Booking;
import com.example.flightbooking.entity.FlightSeat;
import com.example.flightbooking.service.BookingService;
import jakarta.validation.Valid;
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
    public Booking createBooking(Authentication authentication, @Valid @RequestBody CreateBookingRequest req) {
        String userId = (String) authentication.getPrincipal();
        return bookingService.createBooking(userId, req);
    }

    // Trong thực tế endpoint này được gọi từ webhook cổng thanh toán (Stripe/VNPay).
    // Ở đây để permitAll và mock thanh toán thành công, phục vụ mục đích test sườn hệ thống.
    @PostMapping("/{id}/confirm")
    public Booking confirmBooking(@PathVariable String id) {
        return bookingService.confirmBooking(UUID.fromString(id));
    }

    @PostMapping("/{id}/cancel")
    public void cancelBooking(@PathVariable String id) {
        bookingService.cancelBooking(UUID.fromString(id));
    }
}
