package com.example.flightbooking.controller;

import com.example.flightbooking.dto.SeatDto;
import com.example.flightbooking.service.SeatService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/flights/{flightId}/seats")
public class SeatController {

    private final SeatService seatService;

    public SeatController(SeatService seatService) {
        this.seatService = seatService;
    }

    @GetMapping
    public List<SeatDto> getSeatMap(@PathVariable String flightId) {
        return seatService.getSeatMap(UUID.fromString(flightId));
    }
}
