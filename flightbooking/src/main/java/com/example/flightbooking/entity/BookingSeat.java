package com.example.flightbooking.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "booking_seats")
public class BookingSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    // unique = true: 1 ghế chỉ được gắn vào TỐI ĐA 1 booking.
    // Đây là "lưới an toàn cuối cùng" chống bán trùng ghế ở tầng database.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_seat_id", nullable = false, unique = true)
    private FlightSeat flightSeat;

    public BookingSeat() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }

    public FlightSeat getFlightSeat() { return flightSeat; }
    public void setFlightSeat(FlightSeat flightSeat) { this.flightSeat = flightSeat; }
}
