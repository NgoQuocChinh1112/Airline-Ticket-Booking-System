package com.example.flightbooking.service;

import com.example.flightbooking.dto.FlightSearchResult;
import com.example.flightbooking.entity.Airport;
import com.example.flightbooking.entity.Flight;
import com.example.flightbooking.entity.SeatStatus;
import com.example.flightbooking.exception.ApiException;
import com.example.flightbooking.repository.AirportRepository;
import com.example.flightbooking.repository.FlightRepository;
import com.example.flightbooking.repository.FlightSeatRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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

    public List<FlightSearchResult> search(String sourceCode, String destCode, String departureDate, String sortBy) {
        Airport source = airportRepository.findByCode(sourceCode.toUpperCase())
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy sân bay đi: " + sourceCode));
        Airport dest = airportRepository.findByCode(destCode.toUpperCase())
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy sân bay đến: " + destCode));

        LocalDate date = LocalDate.parse(departureDate);
        Instant dayStart = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant dayEnd = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<Flight> flights = "price".equalsIgnoreCase(sortBy)
                ? flightRepository.findBySourceAirportIdAndDestAirportIdAndDepartureTimeBetweenOrderByPriceAsc(
                source.getId(), dest.getId(), dayStart, dayEnd)
                : flightRepository.findBySourceAirportIdAndDestAirportIdAndDepartureTimeBetweenOrderByDepartureTimeAsc(
                source.getId(), dest.getId(), dayStart, dayEnd);

        return flights.stream().map(f -> {
            long available = flightSeatRepository.findByFlightIdOrderBySeatNumberAsc(f.getId()).stream()
                    .filter(s -> s.getStatus() == SeatStatus.AVAILABLE)
                    .count();
            return new FlightSearchResult(
                    f.getId().toString(), f.getFlightNumber(),
                    f.getSourceAirport().getCode(), f.getSourceAirport().getCity(),
                    f.getDestAirport().getCode(), f.getDestAirport().getCity(),
                    f.getAircraft().getName(),
                    f.getDepartureTime(), f.getArrivalTime(), f.getPrice(), available);
        }).collect(Collectors.toList());
    }

    public Flight getById(UUID flightId) {
        return flightRepository.findById(flightId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy chuyến bay"));
    }
}
