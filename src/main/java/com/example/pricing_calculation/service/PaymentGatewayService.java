package com.example.pricing_calculation.service;

import com.example.pricing_calculation.domain.Payment;
import com.example.pricing_calculation.domain.TransactionHistory;
import com.example.pricing_calculation.dto.PaymentCreateRequest;
import com.example.pricing_calculation.dto.PaymentGatewayConfirmRequest;
import com.example.pricing_calculation.dto.PaymentGatewayRequest;
import com.example.pricing_calculation.dto.PaymentGatewayResponse;
import com.example.pricing_calculation.dto.PaymentResponse;
import com.example.pricing_calculation.repository.PaymentRepository;
import com.example.pricing_calculation.repository.TransactionHistoryRepository;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentGatewayService {

    public static final int EXIT_WINDOW_MINUTES = 15;

    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;
    private final TransactionHistoryRepository transactionHistoryRepository;
    private final RealtimeEventService realtimeEventService;

    public PaymentGatewayService(
            PaymentService paymentService,
            PaymentRepository paymentRepository,
            TransactionHistoryRepository transactionHistoryRepository,
            RealtimeEventService realtimeEventService) {
        this.paymentService = paymentService;
        this.paymentRepository = paymentRepository;
        this.transactionHistoryRepository = transactionHistoryRepository;
        this.realtimeEventService = realtimeEventService;
    }

    @Transactional
    public PaymentGatewayResponse createMomoPayment(PaymentGatewayRequest request) {
        return createOnlineGatewayPayment("MOMO", request);
    }

    @Transactional
    public PaymentGatewayResponse createVnpayPayment(PaymentGatewayRequest request) {
        return createOnlineGatewayPayment("VNPAY", request);
    }

    @Transactional
    public PaymentGatewayResponse createCashPayment(PaymentGatewayRequest request) {
        validateRequest(request);
        String referenceCode = buildReferenceCode("CASH");
        PaymentResponse payment = paymentService.create(new PaymentCreateRequest(
                request.sessionId(),
                request.amount(),
                "CASH",
                LocalDateTime.now(),
                "COMPLETED",
                "CASH",
                referenceCode
        ));
        PaymentGatewayResponse response = new PaymentGatewayResponse(
                "CASH",
                payment.id(),
                referenceCode,
                payment.status(),
                null,
                "CASH:" + referenceCode,
                "Cash payment confirmed. Vehicle must exit within 15 minutes",
                payment,
                payment.paymentTime().plusMinutes(EXIT_WINDOW_MINUTES)
        );
        realtimeEventService.publish("/topic/payments", "CASH_PAYMENT_COMPLETED", "Cash payment completed", response);
        return response;
    }

    @Transactional
    public PaymentGatewayResponse confirmOnlinePayment(String gateway, PaymentGatewayConfirmRequest request) {
        if (request == null || request.referenceCode() == null || request.referenceCode().isBlank()) {
            throw new BadRequestException("referenceCode is required");
        }
        String normalizedGateway = normalizeGateway(gateway);
        String normalizedStatus = normalizeStatus(request.status());
        TransactionHistory transaction = transactionHistoryRepository
                .findByReferenceCodeIgnoreCase(request.referenceCode().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + request.referenceCode()));
        if (transaction.getGateway() != null
                && !transaction.getGateway().equalsIgnoreCase(normalizedGateway)) {
            throw new BadRequestException("Reference code does not belong to gateway " + normalizedGateway);
        }
        Payment payment = transaction.getPayment();
        payment.setStatus(normalizedStatus);
        if ("COMPLETED".equals(normalizedStatus)) {
            payment.setPaymentTime(LocalDateTime.now());
        }
        transaction.setStatus(normalizedStatus);
        Payment savedPayment = paymentRepository.save(payment);
        transactionHistoryRepository.save(transaction);
        PaymentResponse paymentResponse = PaymentResponse.from(savedPayment);
        PaymentGatewayResponse response = new PaymentGatewayResponse(
                normalizedGateway,
                savedPayment.getId(),
                transaction.getReferenceCode(),
                normalizedStatus,
                null,
                normalizedGateway + ":" + transaction.getReferenceCode(),
                callbackMessage(request.message(), normalizedStatus),
                paymentResponse,
                "COMPLETED".equals(normalizedStatus)
                        ? paymentResponse.paymentTime().plusMinutes(EXIT_WINDOW_MINUTES)
                        : null
        );
        realtimeEventService.publish(
                "/topic/payments",
                normalizedGateway + "_PAYMENT_" + normalizedStatus,
                normalizedGateway + " payment callback processed",
                response
        );
        return response;
    }

    private PaymentGatewayResponse createOnlineGatewayPayment(String gateway, PaymentGatewayRequest request) {
        validateRequest(request);
        String referenceCode = buildReferenceCode(gateway);
        PaymentResponse payment = paymentService.create(new PaymentCreateRequest(
                request.sessionId(),
                request.amount(),
                gateway,
                LocalDateTime.now(),
                "PENDING",
                gateway,
                referenceCode
        ));
        String paymentUrl = buildPaymentUrl(gateway, referenceCode, request.returnUrl());
        PaymentGatewayResponse response = new PaymentGatewayResponse(
                gateway,
                payment.id(),
                referenceCode,
                payment.status(),
                paymentUrl,
                gateway + ":" + referenceCode + ":" + payment.amount(),
                request.orderInfo() == null ? gateway + " payment created" : request.orderInfo(),
                payment,
                null
        );
        realtimeEventService.publish("/topic/payments", gateway + "_PAYMENT_CREATED", gateway + " payment created", response);
        return response;
    }

    private void validateRequest(PaymentGatewayRequest request) {
        if (request == null || request.sessionId() == null) {
            throw new BadRequestException("sessionId is required");
        }
    }

    private String buildReferenceCode(String gateway) {
        return gateway + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private String buildPaymentUrl(String gateway, String referenceCode, String returnUrl) {
        String baseUrl = "MOMO".equals(gateway)
                ? "https://sandbox.momo.vn/mock/pay"
                : "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
        String encodedReferenceCode = URLEncoder.encode(referenceCode, StandardCharsets.UTF_8);
        String encodedReturnUrl = returnUrl == null || returnUrl.isBlank()
                ? ""
                : "&returnUrl=" + URLEncoder.encode(returnUrl.trim(), StandardCharsets.UTF_8);
        return baseUrl + "?referenceCode=" + encodedReferenceCode + encodedReturnUrl;
    }

    private String normalizeGateway(String gateway) {
        if (gateway == null || gateway.isBlank()) {
            throw new BadRequestException("gateway is required");
        }
        String normalized = gateway.trim().toUpperCase(Locale.ROOT);
        if (!normalized.equals("MOMO") && !normalized.equals("VNPAY")) {
            throw new BadRequestException("Only MOMO and VNPAY callbacks are supported here");
        }
        return normalized;
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "COMPLETED";
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "SUCCESS" -> "COMPLETED";
            case "FAILED", "CANCELLED", "COMPLETED", "PENDING" -> normalized;
            default -> throw new BadRequestException("Unsupported payment status: " + status);
        };
    }

    private String callbackMessage(String requestedMessage, String status) {
        if (requestedMessage != null && !requestedMessage.isBlank()) {
            return requestedMessage.trim();
        }
        if ("COMPLETED".equals(status)) {
            return "Payment completed. Vehicle must exit within 15 minutes";
        }
        return "Gateway callback processed";
    }
}
