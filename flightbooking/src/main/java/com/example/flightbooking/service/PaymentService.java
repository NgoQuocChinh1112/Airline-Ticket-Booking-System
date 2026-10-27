package com.example.flightbooking.service;

import com.example.flightbooking.dto.CreatePaymentRequest;
import com.example.flightbooking.dto.PaymentResponse;
import com.example.flightbooking.entity.Booking;
import com.example.flightbooking.entity.BookingStatus;
import com.example.flightbooking.entity.Payment;
import com.example.flightbooking.entity.PaymentStatus;
import com.example.flightbooking.exception.ApiException;
import com.example.flightbooking.repository.BookingRepository;
import com.example.flightbooking.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final BookingService bookingService;
    private final VnPayService vnPayService;

    public PaymentService(
        PaymentRepository paymentRepository,
        BookingRepository bookingRepository,
        BookingService bookingService,
        VnPayService vnPayService
    ) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.bookingService = bookingService;
        this.vnPayService = vnPayService;
    }

    @Transactional
public PaymentResponse createVnPayPayment(
        String userId,
        CreatePaymentRequest request,
        String clientIp
) {
    UUID bookingId;

    try {
        bookingId = UUID.fromString(request.getBookingId());
    } catch (IllegalArgumentException ex) {
        throw ApiException.badRequest("bookingId không hợp lệ");
    }

    Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> ApiException.notFound("Không tìm thấy booking"));

    if (!booking.getUser().getId().toString().equals(userId)) {
        throw ApiException.unauthorized("Bạn không có quyền thanh toán booking này");
    }

    if (booking.getStatus() != BookingStatus.PENDING) {
        throw ApiException.badRequest("Booking không còn ở trạng thái chờ thanh toán");
    }

    Payment existingPayment = paymentRepository.findByBookingId(bookingId).orElse(null);

    if (existingPayment != null) {
        if (existingPayment.getStatus() == PaymentStatus.PAID) {
            return new PaymentResponse(existingPayment);
        }

        if (existingPayment.getStatus() != PaymentStatus.PENDING) {
            throw ApiException.badRequest("Giao dịch này không thể tiếp tục thanh toán");
        }

        if (existingPayment.getExpiresAt().isBefore(Instant.now())) {
            existingPayment.setStatus(PaymentStatus.EXPIRED);
            paymentRepository.save(existingPayment);
            throw ApiException.badRequest("Giao dịch đã hết hạn");
        }

        String checkoutUrl = vnPayService.createPaymentUrl(existingPayment, clientIp);
        return new PaymentResponse(existingPayment, checkoutUrl);
    }

    Instant expiresAt = booking.getSeats().stream()
            .map(seat -> seat.getFlightSeat().getHeldUntil())
            .filter(Objects::nonNull)
            .min(Instant::compareTo)
            .orElseThrow(() -> ApiException.badRequest("Booking không có ghế đang được giữ"));

    if (expiresAt.isBefore(Instant.now())) {
        throw ApiException.badRequest("Đã hết thời gian giữ ghế, vui lòng chọn ghế lại");
    }

    Payment payment = new Payment();
    payment.setBooking(booking);
    payment.setProvider("VNPAY");
    payment.setAmount(booking.getTotalPrice());
    payment.setPaymentCode(generatePaymentCode());
    payment.setExpiresAt(expiresAt);

    paymentRepository.save(payment);

    String checkoutUrl = vnPayService.createPaymentUrl(payment, clientIp);

    return new PaymentResponse(payment, checkoutUrl);
    }
    @Transactional
    public PaymentResponse completeSandboxPayment(String userId, String paymentCode) {
        Payment payment = paymentRepository.findByPaymentCode(paymentCode)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy giao dịch thanh toán"));

        if (!payment.getBooking().getUser().getId().toString().equals(userId)) {
            throw ApiException.unauthorized("Bạn không có quyền thanh toán giao dịch này");
        }

        if (payment.getStatus() == PaymentStatus.PAID) {
            return new PaymentResponse(payment);
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw ApiException.badRequest("Giao dịch không thể thanh toán");
        }

        if (payment.getExpiresAt().isBefore(Instant.now())) {
            payment.setStatus(PaymentStatus.EXPIRED);
            paymentRepository.save(payment);
            throw ApiException.badRequest("Giao dịch đã hết hạn");
        }

        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(Instant.now());
        payment.setGatewayTransactionId("SANDBOX-" + UUID.randomUUID());

        bookingService.confirmBooking(payment.getBooking().getId());

        return new PaymentResponse(paymentRepository.save(payment));
    }

    @Transactional
public PaymentResponse completeVnPayPayment(
        String paymentCode,
        String vnPayTransactionNumber,
        String vnPayAmount
) {
    Payment payment = paymentRepository
            .findByPaymentCodeForUpdate(paymentCode)
            .orElseThrow(() -> ApiException.notFound("Không tìm thấy giao dịch thanh toán"));

    if (payment.getStatus() == PaymentStatus.PAID) {
        return new PaymentResponse(payment);
    }

    if (payment.getStatus() != PaymentStatus.PENDING) {
        throw ApiException.badRequest("Giao dịch không thể xác nhận");
    }

    if (payment.getExpiresAt().isBefore(Instant.now())) {
        payment.setStatus(PaymentStatus.EXPIRED);
        paymentRepository.save(payment);

        throw ApiException.badRequest("Giao dịch đã hết hạn");
    }

    String expectedAmount = payment.getAmount()
            .movePointRight(2)
            .toBigIntegerExact()
            .toString();

    if (!expectedAmount.equals(vnPayAmount)) {
        throw ApiException.badRequest("Số tiền VNPay trả về không khớp");
    }

    payment.setGatewayTransactionId(vnPayTransactionNumber);
    payment.setStatus(PaymentStatus.PAID);
    payment.setPaidAt(Instant.now());

    bookingService.confirmBooking(payment.getBooking().getId());

    return new PaymentResponse(paymentRepository.save(payment));
}

    private String generatePaymentCode() {
        return "PAY" + UUID.randomUUID()
            .toString()
            .replace("-", "")
            .substring(0, 16)
            .toUpperCase();
    }

    @Transactional(readOnly = true)
public PaymentResponse getVnPayPaymentStatus(String paymentCode) {
    Payment payment = paymentRepository.findByPaymentCode(paymentCode)
            .orElseThrow(() -> ApiException.notFound("Không tìm thấy giao dịch"));

    return new PaymentResponse(payment);
}
}