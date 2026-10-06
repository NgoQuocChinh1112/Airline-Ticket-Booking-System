package com.example.flightbooking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // bật cơ chế chạy job định kỳ, dùng để tự động nhả ghế hết hạn giữ chỗ
public class FlightbookingApplication {
    public static void main(String[] args) {
        SpringApplication.run(FlightbookingApplication.class, args);
    }
}
