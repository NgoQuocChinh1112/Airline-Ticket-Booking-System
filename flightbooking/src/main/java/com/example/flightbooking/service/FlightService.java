package com.example.flightbooking.service;

import com.example.flightbooking.dto.FlightSearchResult;
import com.example.flightbooking.entity.Airport;
import com.example.flightbooking.entity.Flight;
import com.example.flightbooking.exception.ApiException;
import com.example.flightbooking.repository.AirportRepository;
import com.example.flightbooking.repository.FlightRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FlightService {

    private final FlightRepository flightRepository;
    private final AirportRepository airportRepository;
    private final PricingService pricingService;

    public FlightService(FlightRepository flightRepository,
                         AirportRepository airportRepository,
                         PricingService pricingService) {
        this.flightRepository = flightRepository;
        this.airportRepository = airportRepository;
        this.pricingService = pricingService;
    }

    public List<FlightSearchResult> search(String sourceCode,
                                           String destCode,
                                           String departureDate,
                                           String sortBy) {
        Airport source = airportRepository.findByCode(sourceCode.toUpperCase())
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy sân bay đi: " + sourceCode));
        Airport dest = airportRepository.findByCode(destCode.toUpperCase())
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy sân bay đến: " + destCode));

        LocalDate date = LocalDate.parse(departureDate);
        Instant dayStart = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant dayEnd = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        // Tìm kiếm các chuyến bay theo thời gian khởi hành
        List<Flight> flights = flightRepository
                .findBySourceAirportIdAndDestAirportIdAndDepartureTimeBetweenOrderByDepartureTimeAsc(
                        source.getId(), dest.getId(), dayStart, dayEnd);

        List<FlightSearchResult> results = flights.stream().map(f -> {
            // Giá khởi điểm = (Giá ghế thấp nhất) x Hệ số động
            BigDecimal dynamicStartingPrice = pricingService.calculateCurrentPrice(f.getId());
            long availableSeats = f.getAvailableSeats() != null ? f.getAvailableSeats() : 60;

            return new FlightSearchResult(f, dynamicStartingPrice, availableSeats);
        }).collect(Collectors.toList());

        // Nếu client chọn sortBy = "price", tiến hành sắp xếp danh sách kết quả theo giá khởi điểm
        if ("price".equalsIgnoreCase(sortBy)) {
            results.sort(Comparator.comparing(FlightSearchResult::getPrice));
        }

        return results;
    }

    public Flight getById(UUID flightId) {
        return flightRepository.findById(flightId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy chuyến bay"));
    }
}