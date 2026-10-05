package com.example.flightbooking.repository;

import com.example.flightbooking.entity.Flight;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface FlightRepository extends JpaRepository<Flight, UUID> {

    List<Flight> findBySourceAirportIdAndDestAirportIdAndDepartureTimeBetweenOrderByDepartureTimeAsc(
            UUID sourceAirportId, UUID destAirportId, Instant dayStart, Instant dayEnd);

    List<Flight> findBySourceAirportIdAndDestAirportIdAndDepartureTimeBetweenOrderByPriceAsc(
            UUID sourceAirportId, UUID destAirportId, Instant dayStart, Instant dayEnd);
}
