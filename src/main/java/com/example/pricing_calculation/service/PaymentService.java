package com.example.pricing_calculation.service;

import com.example.pricing_calculation.domain.PaymentModuleParkingSession;
import com.example.pricing_calculation.domain.Payment;
import com.example.pricing_calculation.domain.TransactionHistory;
import com.example.pricing_calculation.dto.PaymentCreateRequest;
import com.example.pricing_calculation.dto.PaymentResponse;
import com.example.pricing_calculation.dto.PaymentStatusUpdateRequest;
import com.example.pricing_calculation.repository.PaymentModuleParkingSessionRepository;
import com.example.pricing_calculation.repository.PaymentRepository;
import com.example.pricing_calculation.repository.TransactionHistoryRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentModuleParkingSessionRepository parkingSessionRepository;
    private final TransactionHistoryRepository transactionHistoryRepository;
    private final RealtimeEventService realtimeEventService;

    public PaymentService(
            PaymentRepository paymentRepository,
            PaymentModuleParkingSessionRepository parkingSessionRepository,
            TransactionHistoryRepository transactionHistoryRepository,
            RealtimeEventService realtimeEventService) {
        this.paymentRepository = paymentRepository;
        this.parkingSessionRepository = parkingSessionRepository;
        this.transactionHistoryRepository = transactionHistoryRepository;
        this.realtimeEventService = realtimeEventService;
    }

    @Transactional
    public PaymentResponse create(PaymentCreateRequest request) {
        if (request == null || request.sessionId() == null) {
            throw new BadRequestException("sessionId is required");
        }
        PaymentModuleParkingSession session = parkingSessionRepository.findById(request.sessionId())
                .orElseThrow(() -> new ResourceNotFoundException("Parking session not found: " + request.sessionId()));
        BigDecimal amount = request.amount() == null ? session.getTotalFee() : request.amount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Payment amount must be greater than or equal to 0");
        }
        Payment payment = new Payment();
        payment.setSession(session);
        payment.setAmount(amount);
        payment.setPaymentMethod(defaultText(request.paymentMethod(), "CASH"));
        payment.setPaymentTime(request.paymentTime() == null ? LocalDateTime.now() : request.paymentTime());
        payment.setStatus(defaultText(request.status(), "PENDING").toUpperCase());
        Payment saved = paymentRepository.save(payment);
        createGatewayTransactionIfNeeded(saved, request.gateway(), request.referenceCode(), saved.getStatus());
        PaymentResponse response = PaymentResponse.from(saved);
        realtimeEventService.publish("/topic/payments", "PAYMENT_CREATED", "Payment created", response);
        return response;
    }

    @Transactional(readOnly = true)
    public PaymentResponse getById(Long id) {
        return PaymentResponse.from(findPayment(id));
    }

    @Transactional
    public PaymentResponse updateStatus(Long id, PaymentStatusUpdateRequest request) {
        Payment payment = findPayment(id);
        if (request == null || request.status() == null || request.status().isBlank()) {
            throw new BadRequestException("status is required");
        }
        payment.setStatus(request.status().toUpperCase());
        Payment saved = paymentRepository.save(payment);
        createGatewayTransactionIfNeeded(saved, request.gateway(), request.referenceCode(), saved.getStatus());
        PaymentResponse response = PaymentResponse.from(saved);
        realtimeEventService.publish("/topic/payments", "PAYMENT_STATUS_CHANGED", "Payment status changed", response);
        return response;
    }

    private Payment findPayment(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + id));
    }

    private void createGatewayTransactionIfNeeded(
            Payment payment,
            String gateway,
            String referenceCode,
            String status) {
        if ((gateway == null || gateway.isBlank()) && (referenceCode == null || referenceCode.isBlank())) {
            return;
        }
        if (referenceCode != null
                && !referenceCode.isBlank()
                && transactionHistoryRepository.existsByReferenceCodeIgnoreCase(referenceCode.trim())) {
            return;
        }
        TransactionHistory transaction = new TransactionHistory();
        transaction.setPayment(payment);
        transaction.setGateway(defaultText(gateway, payment.getPaymentMethod()));
        transaction.setReferenceCode(referenceCode);
        transaction.setStatus(defaultText(status, "PENDING").toUpperCase());
        transactionHistoryRepository.save(transaction);
    }

    private String defaultText(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}
