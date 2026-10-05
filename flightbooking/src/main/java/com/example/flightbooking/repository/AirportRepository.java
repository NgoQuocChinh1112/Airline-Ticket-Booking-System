package com.example.flightbooking.repository;

import com.example.flightbooking.entity.Airport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AirportRepository extends JpaRepository<Airport, UUID> {
    Optional<Airport> findByCode(String code);
}
