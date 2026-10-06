package com.example.flightbooking.controller;

import com.example.flightbooking.dto.FlightSearchResult;
import com.example.flightbooking.entity.Flight;
import com.example.flightbooking.service.FlightService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/flights")
public class FlightController {

    private final FlightService flightService;

    public FlightController(FlightService flightService) {
        this.flightService = flightService;
    }

    // VD: /flights/search?sourceAirportCode=HAN&destAirportCode=SGN&departureDate=2026-09-25
    @GetMapping("/search")
    public List<FlightSearchResult> search(
            @RequestParam String sourceAirportCode,
            @RequestParam String destAirportCode,
            @RequestParam String departureDate,
            @RequestParam(required = false) String sortBy) {
        return flightService.search(sourceAirportCode, destAirportCode, departureDate, sortBy);
    }

    @GetMapping("/{id}")
    public Flight getById(@PathVariable String id) {
        return flightService.getById(UUID.fromString(id));
    }
}
