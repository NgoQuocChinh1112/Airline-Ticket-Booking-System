package com.example.flightbooking.service;

import com.example.flightbooking.entity.Payment;
import com.example.flightbooking.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.security.MessageDigest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;

@Service
public class VnPayService {

    private static final DateTimeFormatter VNP_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                    .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    private final String tmnCode;
    private final String hashSecret;
    private final String paymentUrl;
    private final String returnUrl;

    public VnPayService(
            @Value("${app.vnpay.tmn-code}") String tmnCode,
            @Value("${app.vnpay.hash-secret}") String hashSecret,
            @Value("${app.vnpay.payment-url}") String paymentUrl,
            @Value("${app.vnpay.return-url}") String returnUrl
    ) {
        this.tmnCode = tmnCode;
        this.hashSecret = hashSecret;
        this.paymentUrl = paymentUrl;
        this.returnUrl = returnUrl;
    }

    public String createPaymentUrl(Payment payment, String clientIp) {
        requireConfiguration();

        Map<String, String> parameters = new TreeMap<>();

        parameters.put("vnp_Version", "2.1.0");
        parameters.put("vnp_Command", "pay");
        parameters.put("vnp_TmnCode", tmnCode);
        parameters.put("vnp_Amount", vnpAmount(payment.getAmount()));
        parameters.put("vnp_CurrCode", "VND");
        parameters.put("vnp_TxnRef", payment.getPaymentCode());
        parameters.put("vnp_OrderInfo",
                "Thanh toan ve may bay " + payment.getPaymentCode());
        parameters.put("vnp_OrderType", "other");
        parameters.put("vnp_Locale", "vn");
        parameters.put("vnp_ReturnUrl", returnUrl);
        parameters.put("vnp_IpAddr", clientIp);
        parameters.put("vnp_CreateDate", VNP_DATE_FORMAT.format(payment.getCreatedAt()));
        parameters.put("vnp_ExpireDate", VNP_DATE_FORMAT.format(payment.getExpiresAt()));

        String query = buildQuery(parameters);

        String secureHash = hmacSha512(hashSecret, query);

        return paymentUrl + "?" + query + "&vnp_SecureHash=" + secureHash;
    }

    public boolean isValidSignature(Map<String, String> parameters) {
    String receivedHash = parameters.get("vnp_SecureHash");

    if (receivedHash == null || receivedHash.isBlank()) {
        return false;
    }

    Map<String, String> signedParameters = new TreeMap<>();

    parameters.forEach((key, value) -> {
        boolean isVnPayParameter = key.startsWith("vnp_");
        boolean isSignature = key.equals("vnp_SecureHash")
                || key.equals("vnp_SecureHashType");

        if (isVnPayParameter && !isSignature && value != null) {
            signedParameters.put(key, value);
        }
    });

    String dataToVerify = buildQuery(signedParameters);
    String expectedHash = hmacSha512(hashSecret, dataToVerify);

    return MessageDigest.isEqual(
            expectedHash.toLowerCase().getBytes(StandardCharsets.US_ASCII),
            receivedHash.toLowerCase().getBytes(StandardCharsets.US_ASCII)
    );
}

private String buildQuery(Map<String, String> parameters) {
    return new TreeMap<>(parameters).entrySet().stream()
            .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
            .reduce((left, right) -> left + "&" + right)
            .orElseThrow();
}

    private String vnpAmount(BigDecimal amount) {
        return amount.movePointRight(2).toBigIntegerExact().toString();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.US_ASCII);
    }

    private String hmacSha512(String key, String data) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA512");
            hmac.init(new SecretKeySpec(
                    key.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA512"
            ));

            return HexFormat.of().formatHex(
                    hmac.doFinal(data.getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Không thể tạo chữ ký VNPay", ex);
        }
    }

    private void requireConfiguration() {
        if (tmnCode.isBlank() || hashSecret.isBlank() || returnUrl.isBlank()) {
            throw ApiException.badRequest("Thiếu cấu hình VNPay");
        }
    }
}