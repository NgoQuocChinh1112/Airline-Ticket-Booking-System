package com.example.flightbooking.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity // Quản lý hạng ghế (Phổ thông, thương gia, ..)
@Table(name = "aircraft_seat_configs")
public class CabinClass {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aircraft_id", nullable = false)
    private Aircraft aircraft;

    @Column(name = "seat_number", nullable = false)
    private String seatNumber; // VD: 1A, 12C

    @Column(name = "seat_class", nullable = false)
    private String seatClass; // VD: BUSINESS, ECONOMY

    public CabinClass() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Aircraft getAircraft() { return aircraft; }
    public void setAircraft(Aircraft aircraft) { this.aircraft = aircraft; }

    public String getSeatNumber() { return seatNumber; }
    public void setSeatNumber(String seatNumber) { this.seatNumber = seatNumber; }

    public String getSeatClass() { return seatClass; }
    public void setSeatClass(String seatClass) { this.seatClass = seatClass; }
}