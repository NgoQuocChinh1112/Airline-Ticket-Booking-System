package com.example.flightbooking.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "aircrafts")
public class Aircraft {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name; // VD: Airbus A321

    @Column(name = "total_seats", nullable = false)
    private Integer totalSeats;

    public Aircraft() {}

    public Aircraft(String name, Integer totalSeats) {
        this.name = name;
        this.totalSeats = totalSeats;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getTotalSeats() { return totalSeats; }
    public void setTotalSeats(Integer totalSeats) { this.totalSeats = totalSeats; }
}
