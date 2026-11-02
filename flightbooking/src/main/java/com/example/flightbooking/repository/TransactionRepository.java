package com.example.flightbooking.repository;

import com.example.flightbooking.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    List<Transaction> findByBookingId(UUID bookingId);
}