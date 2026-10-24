package com.example.flightbooking.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "aircrafts")
public class Aircraft {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name; // VD: Airbus A321

    private String registrationNumber; // Số hiệu máy bay: VN-A354

    @Column(name = "total_seats", nullable = false)
    private Integer totalSeats;

    // Bổ sung quan hệ 1-N với cấu hình ghế
    @OneToMany(mappedBy = "aircraft", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CabinClass> cabin = new ArrayList<>();

    public Aircraft() {}

    public Aircraft(String name, Integer totalSeats) {
        this.name = name;
        this.totalSeats = totalSeats;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }

    public Integer getTotalSeats() { return totalSeats; }
    public void setTotalSeats(Integer totalSeats) { this.totalSeats = totalSeats; }

    public List<CabinClass> getCabin() { return cabin; }
    public void setCabin(List<CabinClass> Cabin) { this.cabin = Cabin; }
}