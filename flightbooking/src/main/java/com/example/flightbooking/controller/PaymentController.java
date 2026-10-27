package com.example.flightbooking.controller;

import com.example.flightbooking.dto.CreatePaymentRequest;
import com.example.flightbooking.dto.PaymentResponse;
import com.example.flightbooking.service.PaymentService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.example.flightbooking.exception.ApiException;
import com.example.flightbooking.service.VnPayService;

import java.util.Map;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final VnPayService vnPayService;

    public PaymentController(PaymentService paymentService, VnPayService vnPayService) {
        this.paymentService = paymentService;
        this.vnPayService = vnPayService;
    }
    @PostMapping
public PaymentResponse createPayment(
        Authentication authentication,
        @Valid @RequestBody CreatePaymentRequest request,
        HttpServletRequest httpRequest
    ) {
    String userId = (String) authentication.getPrincipal();

    String clientIp = httpRequest.getHeader("X-Forwarded-For");

    if (clientIp == null || clientIp.isBlank()) {
        clientIp = httpRequest.getRemoteAddr();
    } else {
        clientIp = clientIp.split(",")[0].trim();
    }

    return paymentService.createVnPayPayment(userId, request, clientIp);
    }

    @PostMapping("/{paymentCode}/sandbox/complete")
    public PaymentResponse completeSandboxPayment(
            Authentication authentication,
            @PathVariable String paymentCode
    ) {
        String userId = (String) authentication.getPrincipal();

        return paymentService.completeSandboxPayment(userId, paymentCode);
    }

    @GetMapping("/vnpay/ipn")
public Map<String, String> handleVnPayIpn(
        @RequestParam Map<String, String> parameters
) {
    if (!vnPayService.isValidSignature(parameters)) {
        return ipnResponse("97", "Invalid Checksum");
    }

    String paymentCode = parameters.get("vnp_TxnRef");
    String amount = parameters.get("vnp_Amount");
    String transactionNumber = parameters.get("vnp_TransactionNo");
    String responseCode = parameters.get("vnp_ResponseCode");
    String transactionStatus = parameters.get("vnp_TransactionStatus");

    if (paymentCode == null || amount == null || transactionNumber == null) {
        return ipnResponse("99", "Missing payment data");
    }

    boolean paymentSucceeded =
            "00".equals(responseCode)
            && "00".equals(transactionStatus);

    if (!paymentSucceeded) {
        return ipnResponse("00", "Confirm Success");
    }

    try {
        paymentService.completeVnPayPayment(
                paymentCode,
                transactionNumber,
                amount
        );

        return ipnResponse("00", "Confirm Success");
    } catch (ApiException ex) {
        return ipnResponse("99", ex.getMessage());
    }
}

private Map<String, String> ipnResponse(String responseCode, String message) {
    return Map.of(
            "RspCode", responseCode,
            "Message", message
    );
}

@GetMapping("/vnpay/status/{paymentCode}")
public PaymentResponse getVnPayPaymentStatus(
        @PathVariable String paymentCode
) {
    return paymentService.getVnPayPaymentStatus(paymentCode);
}
}