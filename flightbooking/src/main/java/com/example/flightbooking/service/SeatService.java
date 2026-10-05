package com.example.flightbooking.service;

import com.example.flightbooking.dto.SeatDto;
import com.example.flightbooking.entity.FlightSeat;
import com.example.flightbooking.entity.SeatStatus;
import com.example.flightbooking.repository.FlightSeatRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SeatService {

    private final FlightSeatRepository flightSeatRepository;

    public SeatService(FlightSeatRepository flightSeatRepository) {
        this.flightSeatRepository = flightSeatRepository;
    }

    public List<SeatDto> getSeatMap(UUID flightId) {
        List<FlightSeat> seats = flightSeatRepository.findByFlightIdOrderBySeatNumberAsc(flightId);
        Instant now = Instant.now();

        return seats.stream().map(s -> {
            // Lazy check: ghế "held" nhưng đã hết hạn thì hiển thị luôn là "available"
            // cho client, không cần chờ job định kỳ chạy.
            boolean expiredHold = s.getStatus() == SeatStatus.HELD
                    && s.getHeldUntil() != null && s.getHeldUntil().isBefore(now);
            String displayStatus = expiredHold ? "AVAILABLE" : s.getStatus().name();
            return new SeatDto(s.getId().toString(), s.getSeatNumber(), displayStatus);
        }).collect(Collectors.toList());
    }
}
