package com.example.flightbooking.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class FlightSearchResult {
    private String id;
    private String flightNumber;
    private String sourceCode;
    private String sourceCity;
    private String destCode;
    private String destCity;
    private String aircraft;
    private Instant departureTime;
    private Instant arrivalTime;
    private BigDecimal price;
    private long availableSeats;

    public FlightSearchResult(String id, String flightNumber, String sourceCode, String sourceCity,
                              String destCode, String destCity, String aircraft,
                              Instant departureTime, Instant arrivalTime, BigDecimal price, long availableSeats) {
        this.id = id;
        this.flightNumber = flightNumber;
        this.sourceCode = sourceCode;
        this.sourceCity = sourceCity;
        this.destCode = destCode;
        this.destCity = destCity;
        this.aircraft = aircraft;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.price = price;
        this.availableSeats = availableSeats;
    }

    public String getId() { return id; }
    public String getFlightNumber() { return flightNumber; }
    public String getSourceCode() { return sourceCode; }
    public String getSourceCity() { return sourceCity; }
    public String getDestCode() { return destCode; }
    public String getDestCity() { return destCity; }
    public String getAircraft() { return aircraft; }
    public Instant getDepartureTime() { return departureTime; }
    public Instant getArrivalTime() { return arrivalTime; }
    public BigDecimal getPrice() { return price; }
    public long getAvailableSeats() { return availableSeats; }
}
