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
        if (flightRepository.count() > 0) {
            log.info("Dữ liệu chuyến bay đã tồn tại, bỏ qua seed.");
            return;
        }

        log.info("Đang tạo dữ liệu sân bay, máy bay và chuyến bay mẫu...");

        Airport han = airportRepository.save(new Airport("HAN", "Nội Bài", "Hà Nội"));
        Airport sgn = airportRepository.save(new Airport("SGN", "Tân Sơn Nhất", "TP.HCM"));
        Airport dad = airportRepository.save(new Airport("DAD", "Đà Nẵng", "Đà Nẵng"));
        Airport hph = airportRepository.save(new Airport("HPH", "Cát Bi", "Hải Phòng"));
        Airport cxr = airportRepository.save(new Airport("CXR", "Cam Ranh", "Nha Trang"));

        Aircraft a321 = aircraftRepository.save(new Aircraft("Airbus A321", 60));
        Aircraft a320 = aircraftRepository.save(new Aircraft("Airbus A320", 60));

        Instant now = Instant.now();

        // Sinh chuyến bay dải đều cho các ngày: Hôm nay, +1 ngày, +2 ngày, +3 ngày, +5 ngày
        int[] dayOffsets = {0, 1, 2, 3, 5};

        for (int offset : dayOffsets) {
            Instant departureDay = now.plus(offset, ChronoUnit.DAYS);

            createFlightWithSeats("VN101-" + offset, "Vietnam Airlines", a321, han, sgn, now.plus(24, ChronoUnit.HOURS), 2);
            createFlightWithSeats("VJ102-" + offset, "VietJet Air", a320, sgn, han, departureDay.plus(10, ChronoUnit.HOURS), 2);
            createFlightWithSeats("QH205-" + offset, "Bamboo Airways", a321, han, dad, departureDay.plus(14, ChronoUnit.HOURS), 1);
            createFlightWithSeats("VN310-" + offset, "Vietnam Airlines", a320, sgn, cxr, departureDay.plus(16, ChronoUnit.HOURS), 1);
            createFlightWithSeats("VJ450-" + offset, "VietJet Air", a321, hph, sgn, departureDay.plus(18, ChronoUnit.HOURS), 2);
        }

        log.info("Hoàn tất seed dữ liệu mẫu cho nhiều ngày.");
    }

    private void createFlightWithSeats(String flightNumber, String airlineName, Aircraft aircraft,
                                       Airport source, Airport dest, Instant departureTime,
                                       long durationHours) {
        Flight flight = new Flight();
        flight.setFlightNumber(flightNumber);
        flight.setAirlineName(airlineName);
        flight.setAircraft(aircraft);
        flight.setSourceAirport(source);
        flight.setDestAirport(dest);
        flight.setDepartureTime(departureTime);
        flight.setArrivalTime(departureTime.plus(durationHours, ChronoUnit.HOURS));
        flight.setTotalSeats(60);
        flight.setAvailableSeats(60);

        flight = flightRepository.save(flight);

        List<FlightSeat> seats = new ArrayList<>();
        String[] cols = {"A", "B", "C", "D", "E", "F"};

        for (int row = 1; row <= 10; row++) {
            for (String col : cols) {
                FlightSeat seat = new FlightSeat();
                seat.setFlight(flight);
                seat.setSeatNumber(row + col);

                if (row <= 2) {
                    seat.setSeatClass("BUSINESS");
                    seat.setPrice(new BigDecimal("3000000"));
                } else {
                    seat.setSeatClass("ECONOMY");
                    seat.setPrice(new BigDecimal("1200000"));
                }
                seats.add(seat);
            }
        }
        flightSeatRepository.saveAll(seats);
    }
}