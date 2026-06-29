package com.example.pricing_calculation.service;

import com.example.pricing_calculation.domain.Payment;
import com.example.pricing_calculation.domain.TransactionHistory;
import com.example.pricing_calculation.dto.PaymentCreateRequest;
import com.example.pricing_calculation.dto.PaymentGatewayRequest;
import com.example.pricing_calculation.dto.PaymentGatewayResponse;
import com.example.pricing_calculation.dto.PaymentResponse;
import com.example.pricing_calculation.repository.PaymentRepository;
import com.example.pricing_calculation.repository.TransactionHistoryRepository;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentGatewayService {

    public static final int EXIT_WINDOW_MINUTES = 15;
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter VNPAY_DATE = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;
    private final TransactionHistoryRepository transactionHistoryRepository;
    private final RealtimeEventService realtimeEventService;
    private final String vnpayTmnCode;
    private final String vnpayHashSecret;
    private final String vnpayPaymentUrl;
    private final String vnpayReturnUrl;
    private final String personalQrImageUrl;

    public PaymentGatewayService(
            PaymentService paymentService,
            PaymentRepository paymentRepository,
            TransactionHistoryRepository transactionHistoryRepository,
            RealtimeEventService realtimeEventService,
            @Value("${vnpay.tmn-code:}") String vnpayTmnCode,
            @Value("${vnpay.hash-secret:}") String vnpayHashSecret,
            @Value("${vnpay.payment-url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}") String vnpayPaymentUrl,
            @Value("${vnpay.return-url:http://localhost:8080/api/payment-gateways/vnpay/return}") String vnpayReturnUrl,
            @Value("${personal-qr.image-url:/payment/vnpay-personal-qr.png}") String personalQrImageUrl) {
        this.paymentService = paymentService;
        this.paymentRepository = paymentRepository;
        this.transactionHistoryRepository = transactionHistoryRepository;
        this.realtimeEventService = realtimeEventService;
        this.vnpayTmnCode = vnpayTmnCode;
        this.vnpayHashSecret = vnpayHashSecret;
        this.vnpayPaymentUrl = vnpayPaymentUrl;
        this.vnpayReturnUrl = vnpayReturnUrl;
        this.personalQrImageUrl = personalQrImageUrl;
    }

    @Transactional
    public PaymentGatewayResponse createVnpayPayment(PaymentGatewayRequest request, String clientIp) {
        validateRequest(request);
        requireVnpayConfiguration();
        String referenceCode = buildReferenceCode("VNPAY");
        PaymentResponse payment = paymentService.create(new PaymentCreateRequest(
                request.sessionId(), request.amount(), "VNPAY", LocalDateTime.now(),
                "PENDING", "VNPAY", referenceCode));
        String paymentUrl = buildVnpayPaymentUrl(
                referenceCode, payment.amount(), request.orderInfo(), clientIp);
        PaymentGatewayResponse response = new PaymentGatewayResponse(
                "VNPAY", payment.id(), referenceCode, payment.status(), paymentUrl,
                paymentUrl, "VNPay sandbox payment created", payment, null,
                null, null, payment.amount());
        realtimeEventService.publish(
                "/topic/payments", "VNPAY_PAYMENT_CREATED", "VNPay payment created", response);
        return response;
    }

    @Transactional
    public PaymentGatewayResponse createPersonalQrPayment(PaymentGatewayRequest request) {
        validateRequest(request);
        String referenceCode = buildReferenceCode("PERSONALQR");
        PaymentResponse payment = paymentService.create(new PaymentCreateRequest(
                request.sessionId(), request.amount(), "PERSONAL_QR", LocalDateTime.now(),
                "PENDING", "PERSONAL_QR", referenceCode));
        String transferContent = "PARKING-" + payment.id();
        PaymentGatewayResponse response = new PaymentGatewayResponse(
                "PERSONAL_QR", payment.id(), referenceCode, payment.status(), null,
                transferContent,
                "Personal QR payment created. Staff must verify the wallet transfer manually.",
                payment, null, personalQrImageUrl, transferContent, payment.amount());
        realtimeEventService.publish(
                "/topic/payments", "PERSONAL_QR_PAYMENT_CREATED",
                "Personal QR payment created", response);
        return response;
    }

    @Transactional
    public PaymentGatewayResponse createCashPayment(PaymentGatewayRequest request) {
        validateRequest(request);
        String referenceCode = buildReferenceCode("CASH");
        PaymentResponse payment = paymentService.create(new PaymentCreateRequest(
                request.sessionId(), request.amount(), "CASH", LocalDateTime.now(),
                "COMPLETED", "CASH", referenceCode));
        PaymentGatewayResponse response = new PaymentGatewayResponse(
                "CASH", payment.id(), referenceCode, payment.status(), null,
                "CASH:" + referenceCode,
                "Cash payment confirmed. Vehicle must exit within 15 minutes",
                payment, payment.paymentTime().plusMinutes(EXIT_WINDOW_MINUTES),
                null, null, payment.amount());
        realtimeEventService.publish(
                "/topic/payments", "CASH_PAYMENT_COMPLETED", "Cash payment completed", response);
        return response;
    }

    @Transactional
    public PaymentGatewayResponse processVnpayCallback(Map<String, String> callbackParameters) {
        requireVnpayConfiguration();
        if (callbackParameters == null || callbackParameters.isEmpty()) {
            throw new BadRequestException("VNPay callback parameters are required");
        }
        verifyVnpaySignature(callbackParameters);

        String referenceCode = requiredParameter(callbackParameters, "vnp_TxnRef");
        TransactionHistory transaction = transactionHistoryRepository
                .findByReferenceCodeIgnoreCase(referenceCode)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + referenceCode));
        if (transaction.getGateway() == null || !"VNPAY".equalsIgnoreCase(transaction.getGateway())) {
            throw new BadRequestException("Transaction does not belong to VNPay");
        }

        Payment payment = transaction.getPayment();
        verifyCallbackAmount(payment.getAmount(), requiredParameter(callbackParameters, "vnp_Amount"));
        boolean successful = "00".equals(callbackParameters.get("vnp_ResponseCode"))
                && "00".equals(callbackParameters.get("vnp_TransactionStatus"));

        if (!"COMPLETED".equalsIgnoreCase(payment.getStatus())) {
            String status = successful ? "COMPLETED" : "FAILED";
            payment.setStatus(status);
            transaction.setStatus(status);
            if (successful) {
                payment.setPaymentTime(LocalDateTime.now());
            }
            paymentRepository.save(payment);
            transactionHistoryRepository.save(transaction);
        }

        PaymentResponse paymentResponse = PaymentResponse.from(payment);
        PaymentGatewayResponse response = new PaymentGatewayResponse(
                "VNPAY", payment.getId(), referenceCode, payment.getStatus(), null, null,
                successful ? "VNPay payment completed" : "VNPay payment failed",
                paymentResponse,
                "COMPLETED".equalsIgnoreCase(payment.getStatus())
                        ? paymentResponse.paymentTime().plusMinutes(EXIT_WINDOW_MINUTES)
                        : null,
                null, null, paymentResponse.amount());
        realtimeEventService.publish(
                "/topic/payments", "VNPAY_PAYMENT_" + payment.getStatus(),
                "VNPay callback processed", response);
        return response;
    }

    private String buildVnpayPaymentUrl(
            String referenceCode, BigDecimal amount, String requestedOrderInfo, String clientIp) {
        ZonedDateTime now = ZonedDateTime.now(VIETNAM_ZONE);
        Map<String, String> parameters = new TreeMap<>();
        parameters.put("vnp_Version", "2.1.0");
        parameters.put("vnp_Command", "pay");
        parameters.put("vnp_TmnCode", vnpayTmnCode.trim());
        parameters.put("vnp_Amount", toVnpayAmount(amount));
        parameters.put("vnp_CurrCode", "VND");
        parameters.put("vnp_TxnRef", referenceCode);
        parameters.put("vnp_OrderInfo", normalizeOrderInfo(requestedOrderInfo, referenceCode));
        parameters.put("vnp_OrderType", "other");
        parameters.put("vnp_Locale", "vn");
        parameters.put("vnp_ReturnUrl", vnpayReturnUrl.trim());
        parameters.put("vnp_IpAddr", normalizeIp(clientIp));
        parameters.put("vnp_CreateDate", VNPAY_DATE.format(now));
        parameters.put("vnp_ExpireDate", VNPAY_DATE.format(now.plusMinutes(15)));

        String query = encodeParameters(parameters);
        return vnpayPaymentUrl.trim() + "?" + query
                + "&vnp_SecureHash=" + hmacSha512(vnpayHashSecret.trim(), query);
    }

    private void verifyVnpaySignature(Map<String, String> callbackParameters) {
        String receivedHash = requiredParameter(callbackParameters, "vnp_SecureHash");
        Map<String, String> signedFields = new TreeMap<>();
        callbackParameters.forEach((key, value) -> {
            if (key != null && key.startsWith("vnp_")
                    && !"vnp_SecureHash".equals(key)
                    && !"vnp_SecureHashType".equals(key)
                    && value != null && !value.isBlank()) {
                signedFields.put(key, value);
            }
        });
        String expectedHash = hmacSha512(vnpayHashSecret.trim(), encodeParameters(signedFields));
        if (!MessageDigest.isEqual(
                expectedHash.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII),
                receivedHash.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII))) {
            throw new BadRequestException("Invalid VNPay signature");
        }
    }

    private String encodeParameters(Map<String, String> parameters) {
        Map<String, String> encoded = new LinkedHashMap<>();
        parameters.forEach((key, value) -> encoded.put(urlEncode(key), urlEncode(value)));
        return encoded.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .reduce((left, right) -> left + "&" + right)
                .orElse("");
    }

    private String hmacSha512(String secret, String data) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA512");
            hmac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] bytes = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                result.append(String.format("%02x", value & 0xff));
            }
            return result.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to sign VNPay request", exception);
        }
    }

    private void verifyCallbackAmount(BigDecimal expectedAmount, String callbackAmount) {
        try {
            if (!toVnpayAmount(expectedAmount).equals(new BigDecimal(callbackAmount).toPlainString())) {
                throw new BadRequestException("VNPay callback amount does not match payment amount");
            }
        } catch (NumberFormatException exception) {
            throw new BadRequestException("Invalid VNPay callback amount");
        }
    }

    private String toVnpayAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("VNPay amount must be greater than 0");
        }
        try {
            return amount.movePointRight(2).toBigIntegerExact().toString();
        } catch (ArithmeticException exception) {
            throw new BadRequestException("VNPay amount supports at most two decimal places");
        }
    }

    private String normalizeOrderInfo(String orderInfo, String referenceCode) {
        if (orderInfo == null || orderInfo.isBlank()) {
            return "Parking payment " + referenceCode;
        }
        String normalized = orderInfo.trim().replaceAll("[^\\p{L}\\p{N} ._-]", "");
        return normalized.isBlank() ? "Parking payment " + referenceCode : normalized;
    }

    private String normalizeIp(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            return "127.0.0.1";
        }
        String firstAddress = clientIp.split(",")[0].trim();
        return firstAddress.isBlank() ? "127.0.0.1" : firstAddress;
    }

    private String requiredParameter(Map<String, String> parameters, String name) {
        String value = parameters.get(name);
        if (value == null || value.isBlank()) {
            throw new BadRequestException(name + " is required");
        }
        return value.trim();
    }

    private void validateRequest(PaymentGatewayRequest request) {
        if (request == null || request.sessionId() == null) {
            throw new BadRequestException("sessionId is required");
        }
    }

    private void requireVnpayConfiguration() {
        if (vnpayTmnCode == null || vnpayTmnCode.isBlank()
                || vnpayHashSecret == null || vnpayHashSecret.isBlank()) {
            throw new BadRequestException("VNPay is not configured. Set VNPAY_TMN_CODE and VNPAY_HASH_SECRET");
        }
    }

    private String buildReferenceCode(String gateway) {
        return gateway + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}

