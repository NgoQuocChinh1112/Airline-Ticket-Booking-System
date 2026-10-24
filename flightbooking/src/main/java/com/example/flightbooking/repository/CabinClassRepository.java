package com.example.flightbooking.repository;

import com.example.flightbooking.entity.CabinClass;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface CabinClassRepository extends JpaRepository<CabinClass, UUID> {
}