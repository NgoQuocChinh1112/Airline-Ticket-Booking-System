package com.example.flightbooking.service;

import java.util.LinkedHashMap;
import java.util.Map;

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
            existingPayment.setPaymentCode(generatePaymentCode());
            existingPayment.setGatewayTransactionId(null);
            existingPayment.setStatus(PaymentStatus.PENDING);
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

    /**
 * Xử lý IPN từ VNPay. Thứ tự kiểm tra theo đúng bộ test của VNPay:
 * chữ ký (97) -> không thấy đơn (01) -> sai số tiền (04) -> đơn đã xử lý (02) -> cập nhật (00).
 *
 * Không ném exception cho các trường hợp nghiệp vụ đã biết, chỉ ném khi lỗi bất ngờ
 * (vd confirmBooking thất bại) để transaction rollback; controller sẽ đổi thành RspCode 99.
 */
@Transactional
public Map<String, String> processVnPayIpn(Map<String, String> parameters) {
    // 97 - Chữ ký không hợp lệ
    if (!vnPayService.isValidSignature(parameters)) {
        return ipnResponse("97", "Invalid Checksum");
    }

    String paymentCode = parameters.get("vnp_TxnRef");
    String amount = parameters.get("vnp_Amount");
    String transactionNumber = parameters.get("vnp_TransactionNo");
    String responseCode = parameters.get("vnp_ResponseCode");
    String transactionStatus = parameters.get("vnp_TransactionStatus");

    if (paymentCode == null || paymentCode.isBlank() || amount == null || amount.isBlank()) {
        return ipnResponse("99", "Missing payment data");
    }

    // 01 - Không tìm thấy giao dịch
    Payment payment = paymentRepository.findByPaymentCodeForUpdate(paymentCode).orElse(null);
    if (payment == null) {
        return ipnResponse("01", "Order Not Found");
    }

    // 04 - Số tiền không khớp (phải kiểm tra TRƯỚC trạng thái, VNPay test lại trên đơn đã PAID)
    String expectedAmount = payment.getAmount()
            .movePointRight(2)
            .toBigIntegerExact()
            .toString();

    if (!expectedAmount.equals(amount)) {
        return ipnResponse("04", "Invalid amount");
    }

    // 02 - Giao dịch đã được xử lý trước đó (PAID/FAILED/EXPIRED)
    if (payment.getStatus() != PaymentStatus.PENDING) {
        return ipnResponse("02", "Order already confirmed");
    }

    boolean paymentSucceeded = "00".equals(responseCode) && "00".equals(transactionStatus);

    // Thanh toán không thành công -> lưu FAILED, vẫn trả 00 để VNPay biết đã nhận kết quả
    if (!paymentSucceeded) {
        if (transactionNumber != null && !transactionNumber.isBlank()) {
            payment.setGatewayTransactionId(transactionNumber);
        }
        payment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);

        return ipnResponse("00", "Confirm Success");
    }

    if (transactionNumber == null || transactionNumber.isBlank()) {
        return ipnResponse("99", "Missing payment data");
    }

    // Thanh toán thành công -> PAID và xác nhận booking.
    // Không chặn theo expiresAt ở đây: VNPay đã thu tiền, confirmBooking sẽ tự kiểm tra ghế còn được giữ.
    payment.setGatewayTransactionId(transactionNumber);
    payment.setStatus(PaymentStatus.PAID);
    payment.setPaidAt(Instant.now());

    bookingService.confirmBooking(payment.getBooking().getId());
    paymentRepository.save(payment);

    return ipnResponse("00", "Confirm Success");
}

public static Map<String, String> ipnResponse(String responseCode, String message) {
    // LinkedHashMap giữ thứ tự Message -> RspCode giống mẫu của VNPay
    Map<String, String> response = new LinkedHashMap<>();
    response.put("Message", message);
    response.put("RspCode", responseCode);
    return response;
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