package com.example.flightbooking.repository;

import com.example.flightbooking.entity.Aircraft;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AircraftRepository extends JpaRepository<Aircraft, UUID> {
}
