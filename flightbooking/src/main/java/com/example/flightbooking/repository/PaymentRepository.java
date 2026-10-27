package com.example.flightbooking.repository;

import com.example.flightbooking.entity.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByBookingId(UUID bookingId);

    Optional<Payment> findByPaymentCode(String paymentCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p from Payment p
            join fetch p.booking b
            join fetch b.user
            where p.paymentCode = :paymentCode
            """)
    Optional<Payment> findByPaymentCodeForUpdate(
            @Param("paymentCode") String paymentCode
    );
}