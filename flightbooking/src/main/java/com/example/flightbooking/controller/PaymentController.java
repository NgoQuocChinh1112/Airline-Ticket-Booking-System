package com.example.flightbooking.controller;

import com.example.flightbooking.dto.CreatePaymentRequest;
import com.example.flightbooking.dto.PaymentResponse;
import com.example.flightbooking.exception.ApiException;
import com.example.flightbooking.service.PaymentService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
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

    /**
     * IPN do VNPay gọi. Luôn phải trả JSON {"RspCode": "...", "Message": "..."}.
     * Mọi kết quả nghiệp vụ (97/01/04/02/00) do PaymentService quyết định.
     */
    @GetMapping("/vnpay/ipn")
    public Map<String, String> handleVnPayIpn(@RequestParam Map<String, String> parameters) {
        try {
            return paymentService.processVnPayIpn(parameters);
        } catch (ApiException ex) {
            log.warn("IPN VNPay không xử lý được: {}", ex.getMessage());
            return PaymentService.ipnResponse("99", ex.getMessage());
        } catch (RuntimeException ex) {
            log.error("Lỗi không mong đợi khi xử lý IPN VNPay", ex);
            return PaymentService.ipnResponse("99", "Unknown error");
        }
    }

    @GetMapping("/vnpay/status/{paymentCode}")
    public PaymentResponse getVnPayPaymentStatus(@PathVariable String paymentCode) {
        return paymentService.getVnPayPaymentStatus(paymentCode);
    }
}