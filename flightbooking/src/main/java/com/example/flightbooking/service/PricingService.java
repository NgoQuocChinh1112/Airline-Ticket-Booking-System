package com.example.flightbooking.service;

import com.example.flightbooking.entity.Flight;
import com.example.flightbooking.entity.FlightPricingRule;
import com.example.flightbooking.entity.FlightSeat;
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

    public BigDecimal calculateMultiplier(UUID flightId) {
        Flight flight = flightRepository.findById(flightId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy chuyến bay"));

        // Tính tỷ lệ lấp đầy ghế
        long totalSeats = flight.getTotalSeats() != null ? flight.getTotalSeats() : 60;
        List<FlightSeat> seats = flightSeatRepository.findByFlightIdOrderBySeatNumberAsc(flightId);

        long bookedSeats = seats.stream()
                .filter(s -> s.getStatus() == SeatStatus.BOOKED || s.getStatus() == SeatStatus.HELD)
                .count();
        double occupancyRate = totalSeats > 0 ? (double) bookedSeats / totalSeats : 0.0;

        // Tính số ngày còn lại tới giờ khởi hành
        long daysUntilDeparture = ChronoUnit.DAYS.between(Instant.now(), flight.getDepartureTime());

        // Tìm các quy tắc định giá (Pricing Rules) cho chuyến bay này
        List<FlightPricingRule> rules = pricingRuleRepository.findByFlightId(flightId);

        BigDecimal maxMultiplier = BigDecimal.ONE;
        for (FlightPricingRule rule : rules) {
            boolean matchOccupancy = rule.getOccupancyPercentage() == null
                    || occupancyRate >= rule.getOccupancyPercentage();
            boolean matchDays = rule.getDaysBeforeDeparture() == null
                    || daysUntilDeparture <= rule.getDaysBeforeDeparture();

            if (matchOccupancy && matchDays) {
                if (rule.getPriceMultiplier().compareTo(maxMultiplier) > 0) {
                    maxMultiplier = rule.getPriceMultiplier();
                }
            }
        }
        return maxMultiplier;
    }

    /**
     * 2. Tính giá vé khởi điểm (Giá ghế thấp nhất x Hệ số nhân) để hiển thị trên giao diện Tìm kiếm.
     */
    public BigDecimal calculateCurrentPrice(UUID flightId) {
        List<FlightSeat> seats = flightSeatRepository.findByFlightIdOrderBySeatNumberAsc(flightId);

        // Lấy đơn giá vé thấp nhất trong danh sách các ghế của chuyến bay
        BigDecimal minSeatPrice = seats.stream()
                .map(FlightSeat::getPrice)
                .filter(price -> price != null)
                .min(BigDecimal::compareTo)
                .orElse(new BigDecimal("1200000"));

        BigDecimal multiplier = calculateMultiplier(flightId);

        return minSeatPrice.multiply(multiplier);
    }

    /**
     * 3. Tính giá vé thực tế cho 1 ghế cụ thể sau khi nhân hệ số động.
     */
    public BigDecimal calculateSeatPrice(FlightSeat seat) {
        BigDecimal multiplier = calculateMultiplier(seat.getFlight().getId());
        return seat.getPrice().multiply(multiplier);
    }
}