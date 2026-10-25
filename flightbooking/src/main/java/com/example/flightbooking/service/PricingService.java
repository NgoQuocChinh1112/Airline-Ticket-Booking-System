package com.example.flightbooking.service;

import com.example.flightbooking.entity.Flight;
import com.example.flightbooking.entity.FlightPricingRule;
import com.example.flightbooking.entity.SeatStatus;
import com.example.flightbooking.exception.ApiException;
import com.example.flightbooking.repository.FlightRepository;
import com.example.flightbooking.repository.FlightSeatRepository;
import com.example.flightbooking.repository.PricingRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class PricingService {

    private final FlightRepository flightRepository;
    private final FlightSeatRepository flightSeatRepository;
    private final PricingRepository pricingRuleRepository;

    public PricingService(FlightRepository flightRepository,
                          FlightSeatRepository flightSeatRepository,
                          PricingRepository pricingRuleRepository) {
        this.flightRepository = flightRepository;
        this.flightSeatRepository = flightSeatRepository;
        this.pricingRuleRepository = pricingRuleRepository;
    }

    public BigDecimal calculateCurrentPrice(UUID flightId) {
        Flight flight = flightRepository.findById(flightId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy chuyến bay"));

        BigDecimal basePrice = flight.getPrice(); // Giá vé gốc

        // 1. Tính tỷ lệ lấp đầy ghế
        long totalSeats = flight.getAircraft().getTotalSeats();
        long bookedSeats = flightSeatRepository.findByFlightIdOrderBySeatNumberAsc(flightId).stream()
                .filter(s -> s.getStatus() == SeatStatus.BOOKED || s.getStatus() == SeatStatus.HELD)
                .count();
        double occupancyRate = totalSeats > 0 ? (double) bookedSeats / totalSeats : 0.0;

        // 2. Tính số ngày còn lại tới giờ khởi hành
        long daysUntilDeparture = ChronoUnit.DAYS.between(Instant.now(), flight.getDepartureTime());

        // 3. Tìm các Pricing Rules áp dụng cho chuyến bay này từ Database
        List<FlightPricingRule> rules = pricingRuleRepository.findByFlightId(flightId);

        BigDecimal maxMultiplier = BigDecimal.ONE; // Mặc định hệ số nhân = 1.0 (không tăng giá)

        for (FlightPricingRule rule : rules) {
            boolean matchOccupancy = rule.getOccupancyPercentage() == null
                    || occupancyRate >= rule.getOccupancyPercentage();
            boolean matchDays = rule.getDaysBeforeDeparture() == null
                    || daysUntilDeparture <= rule.getDaysBeforeDeparture();

            // Nếu khớp cả 2 điều kiện thì áp dụng hệ số nhân lớn nhất
            if (matchOccupancy && matchDays) {
                if (rule.getPriceMultiplier().compareTo(maxMultiplier) > 0) {
                    maxMultiplier = rule.getPriceMultiplier();
                }
            }
        }

        // 4. Giá cuối cùng = Giá gốc * Hệ số nhân lớn nhất
        return basePrice.multiply(maxMultiplier);
    }
}