package com.example.flightbooking.repository;

import com.example.flightbooking.entity.BookingSeat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BookingSeatRepository extends JpaRepository<BookingSeat, UUID> {
}
