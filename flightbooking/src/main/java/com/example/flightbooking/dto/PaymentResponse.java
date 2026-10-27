package com.example.flightbooking.dto;

import com.example.flightbooking.entity.BookingStatus;
import com.example.flightbooking.entity.Payment;
import com.example.flightbooking.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class PaymentResponse {

    private final UUID id;
    private final UUID bookingId;
    private final String paymentCode;
    private final String provider;
    private final PaymentStatus status;
    private final BookingStatus bookingStatus;
    private final BigDecimal amount;
    private final Instant expiresAt;
    private final Instant paidAt;
    private final String checkoutUrl;

    public PaymentResponse(Payment payment) {
        this(payment, null);
}

    public PaymentResponse(Payment payment, String checkoutUrl) {
        this.id = payment.getId();
        this.bookingId = payment.getBooking().getId();
        this.paymentCode = payment.getPaymentCode();
        this.provider = payment.getProvider();
        this.status = payment.getStatus();
        this.bookingStatus = payment.getBooking().getStatus();
        this.amount = payment.getAmount();
        this.expiresAt = payment.getExpiresAt();
        this.paidAt = payment.getPaidAt();
        this.checkoutUrl = checkoutUrl;
    }

    public UUID getId() { return id; }
    public UUID getBookingId() { return bookingId; }
    public String getPaymentCode() { return paymentCode; }
    public String getProvider() { return provider; }
    public PaymentStatus getStatus() { return status; }
    public BookingStatus getBookingStatus() { return bookingStatus; }
    public BigDecimal getAmount() { return amount; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getPaidAt() { return paidAt; }

    public String getCheckoutUrl() {
        return checkoutUrl;
}
}