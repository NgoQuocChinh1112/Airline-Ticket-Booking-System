package com.example.flightbooking.repository;

import com.example.flightbooking.entity.FlightPricingRule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface PricingRepository extends JpaRepository<FlightPricingRule, UUID> {
    List<FlightPricingRule> findByFlightId(UUID flightId);
}