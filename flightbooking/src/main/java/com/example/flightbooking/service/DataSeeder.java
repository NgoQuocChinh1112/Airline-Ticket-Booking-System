package com.example.flightbooking.service;

import com.example.flightbooking.entity.Aircraft;
import com.example.flightbooking.entity.Airport;
import com.example.flightbooking.entity.Flight;
import com.example.flightbooking.entity.FlightSeat;
import com.example.flightbooking.repository.AircraftRepository;
import com.example.flightbooking.repository.AirportRepository;
import com.example.flightbooking.repository.FlightRepository;
import com.example.flightbooking.repository.FlightSeatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Tương đương prisma/seed.ts ở bản Node.js: tự động tạo sân bay, máy bay, chuyến bay,
 * và sơ đồ ghế mẫu để có dữ liệu test ngay, không cần chạy lệnh riêng.
 * Chỉ chạy nếu bảng airports đang trống (tránh tạo trùng mỗi lần khởi động lại app).
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final AirportRepository airportRepository;
    private final AircraftRepository aircraftRepository;
    private final FlightRepository flightRepository;
    private final FlightSeatRepository flightSeatRepository;

    public DataSeeder(AirportRepository airportRepository, AircraftRepository aircraftRepository,
                       FlightRepository flightRepository, FlightSeatRepository flightSeatRepository) {
        this.airportRepository = airportRepository;
        this.aircraftRepository = aircraftRepository;
        this.flightRepository = flightRepository;
        this.flightSeatRepository = flightSeatRepository;
    }

    @Override
    public void run(String... args) {
        if (airportRepository.count() > 0) {
            log.info("Dữ liệu mẫu đã tồn tại, bỏ qua seed.");
            return;
        }

        log.info("Đang tạo dữ liệu mẫu...");

        Airport han = airportRepository.save(new Airport("HAN", "Nội Bài", "Hà Nội"));
        Airport sgn = airportRepository.save(new Airport("SGN", "Tân Sơn Nhất", "TP.HCM"));
        Airport dad = airportRepository.save(new Airport("DAD", "Đà Nẵng", "Đà Nẵng"));
        Airport hph = airportRepository.save(new Airport("HPH", "Cát Bi", "Hải Phòng"));
        Airport cxr = airportRepository.save(new Airport("CXR", "Cam Ranh", "Nha Trang"));

        Aircraft a321 = aircraftRepository.save(new Aircraft("Airbus A321", 60));
        Aircraft a320 = aircraftRepository.save(new Aircraft("Airbus A320", 60));

        Instant now = Instant.now();

        createFlightWithSeats("VN101", a321, han, sgn, now.plus(24, ChronoUnit.HOURS), 2, new BigDecimal("1500000"));
        createFlightWithSeats("VN102", a320, sgn, han, now.plus(30, ChronoUnit.HOURS), 2, new BigDecimal("1550000"));
        createFlightWithSeats("VN205", a321, han, dad, now.plus(26, ChronoUnit.HOURS), 1, new BigDecimal("1100000"));
        createFlightWithSeats("VN310", a320, sgn, cxr, now.plus(28, ChronoUnit.HOURS), 1, new BigDecimal("950000"));
        createFlightWithSeats("VN450", a321, hph, sgn, now.plus(40, ChronoUnit.HOURS), 2, new BigDecimal("1700000"));

        log.info("Hoàn tất seed dữ liệu mẫu.");
    }

    private void createFlightWithSeats(String flightNumber, Aircraft aircraft, Airport source, Airport dest,
                                        Instant departureTime, long durationHours, BigDecimal price) {
        Flight flight = new Flight();
        flight.setFlightNumber(flightNumber);
        flight.setAircraft(aircraft);
        flight.setSourceAirport(source);
        flight.setDestAirport(dest);
        flight.setDepartureTime(departureTime);
        flight.setArrivalTime(departureTime.plus(durationHours, ChronoUnit.HOURS));
        flight.setPrice(price);
        flight = flightRepository.save(flight);

        // 10 hàng (1-10) x 6 cột (A-F) = 60 ghế
        List<FlightSeat> seats = new ArrayList<>();
        String[] cols = {"A", "B", "C", "D", "E", "F"};
        for (int row = 1; row <= 10; row++) {
            for (String col : cols) {
                FlightSeat seat = new FlightSeat();
                seat.setFlight(flight);
                seat.setSeatNumber(row + col);
                seats.add(seat);
            }
        }
        flightSeatRepository.saveAll(seats);

        log.info("  - {}: tạo {} ghế", flightNumber, seats.size());
    }
}
