package com.example.flightbooking.service;

import com.example.flightbooking.repository.AirportRepository;
import com.example.flightbooking.repository.FlightRepository;
import com.example.flightbooking.repository.FlightSeatRepository;
import org.springframework.stereotype.Service;

@Service
public class FlightService {

    private final FlightRepository flightRepository;
    private final AirportRepository airportRepository;
    private final FlightSeatRepository flightSeatRepository;

    public FlightService(FlightRepository flightRepository, AirportRepository airportRepository,
                         FlightSeatRepository flightSeatRepository) {
        this.flightRepository = flightRepository;
        this.airportRepository = airportRepository;
        this.flightSeatRepository = flightSeatRepository;
    }
}
