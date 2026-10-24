package com.example.flightbooking.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity // Căn chỉnh giá vé theo khung giờ
@Table(name = "flight_pricing_rules")
public class FlightPricingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id", nullable = false)
    private Flight flight;

    @Column(name = "days_before_departure")
    private Integer daysBeforeDeparture; // Áp dụng khi còn cách ngày bay bao nhiêu ngày

    @Column(name = "occupancy_percentage")
    private Double occupancyPercentage; // Áp dụng khi chuyến bay đã đầy bao nhiêu % (VD: 0.8 = 80%)

    @Column(name = "price_multiplier", nullable = false, precision = 4, scale = 2)
    private BigDecimal priceMultiplier; // Hệ số nhân (VD: 1.25 = Tăng 25%)

    public FlightPricingRule() {}

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Flight getFlight() { return flight; }
    public void setFlight(Flight flight) { this.flight = flight; }
    public Integer getDaysBeforeDeparture() { return daysBeforeDeparture; }
    public void setDaysBeforeDeparture(Integer daysBeforeDeparture) { this.daysBeforeDeparture = daysBeforeDeparture; }
    public Double getOccupancyPercentage() { return occupancyPercentage; }
    public void setOccupancyPercentage(Double occupancyPercentage) { this.occupancyPercentage = occupancyPercentage; }
    public BigDecimal getPriceMultiplier() { return priceMultiplier; }
    public void setPriceMultiplier(BigDecimal priceMultiplier) { this.priceMultiplier = priceMultiplier; }
}