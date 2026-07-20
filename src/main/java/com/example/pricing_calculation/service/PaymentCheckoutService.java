package com.example.pricing_calculation.service;

import com.example.pricing_calculation.domain.PaymentModuleParkingSession;
import com.example.pricing_calculation.domain.Payment;
import com.example.pricing_calculation.dto.ParkingSessionResponse;
import com.example.pricing_calculation.dto.PaymentCheckoutPrepareRequest;
import com.example.pricing_calculation.dto.PaymentCheckoutResponse;
import com.example.pricing_calculation.dto.PaymentExitValidationRequest;
import com.example.pricing_calculation.dto.PaymentExitValidationResponse;
import com.example.pricing_calculation.dto.PaymentGatewayRequest;
import com.example.pricing_calculation.dto.SessionCheckoutRequest;
import com.example.pricing_calculation.repository.PaymentModuleParkingSessionRepository;
import com.example.pricing_calculation.repository.PaymentRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentCheckoutService {

    private static final List<String> PAID_STATUSES = List.of("COMPLETED", "SUCCESS");

    private final PaymentModuleParkingSessionRepository parkingSessionRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentModuleParkingSessionService parkingSessionService;
    private final PaymentGatewayService paymentGatewayService;
    private final RealtimeEventService realtimeEventService;

    public PaymentCheckoutService(
            PaymentModuleParkingSessionRepository parkingSessionRepository,
            PaymentRepository paymentRepository,
            PaymentModuleParkingSessionService parkingSessionService,
            PaymentGatewayService paymentGatewayService,
            RealtimeEventService realtimeEventService) {
        this.parkingSessionRepository = parkingSessionRepository;
        this.paymentRepository = paymentRepository;
        this.parkingSessionService = parkingSessionService;
        this.paymentGatewayService = paymentGatewayService;
        this.realtimeEventService = realtimeEventService;
    }

    @Transactional
    public PaymentCheckoutResponse prepare(PaymentCheckoutPrepareRequest request) {
        if (request == null || request.licensePlate() == null || request.licensePlate().isBlank()) {
            throw new BadRequestException("licensePlate is required");
        }
        PaymentModuleParkingSession session = findLatestSession(request.licensePlate());
        ParkingSessionResponse sessionResponse;
        if ("ACTIVE".equalsIgnoreCase(session.getStatus())) {
            sessionResponse = parkingSessionService.checkout(
                    session.getId(),
                    new SessionCheckoutRequest(
                            request.exitTime(),
                            request.lostTicket(),
                            request.overtimeMinutes()
                    )
            );
            if (sessionResponse.totalFee() != null
                    && sessionResponse.totalFee().compareTo(BigDecimal.ZERO) == 0
                    && findRelevantPayment(session.getId()).isEmpty()) {
                paymentGatewayService.createCashPayment(new PaymentGatewayRequest(
                        session.getId(),
                        BigDecimal.ZERO,
                        null,
                        "Monthly pass / zero-fee checkout"
                ));
            }
        } else if ("PAYMENT_PENDING".equalsIgnoreCase(session.getStatus()) || "COMPLETED".equalsIgnoreCase(session.getStatus())) {
            sessionResponse = ParkingSessionResponse.from(session);
        } else {
            throw new BadRequestException("Parking session cannot enter payment checkout from status " + session.getStatus());
        }
        return buildCheckoutResponse(sessionResponse, findRelevantPayment(session.getId()).orElse(null));
    }

    @Transactional(readOnly = true)
    public PaymentCheckoutResponse status(Long sessionId) {
        PaymentModuleParkingSession session = parkingSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Parking session not found: " + sessionId));
        return buildCheckoutResponse(
                ParkingSessionResponse.from(session),
                findRelevantPayment(sessionId).orElse(null)
        );
    }

    @Transactional
    public PaymentExitValidationResponse validateExit(PaymentExitValidationRequest request) {
        if (request == null || request.licensePlate() == null || request.licensePlate().isBlank()) {
            throw new BadRequestException("licensePlate is required");
        }
        PaymentModuleParkingSession session = findLatestSession(request.licensePlate());
        LocalDateTime detectedAt = request.detectedAt() == null ? LocalDateTime.now() : request.detectedAt();
        Optional<Payment> completedPayment = paymentRepository
                .findFirstBySessionIdAndStatusInOrderByPaymentTimeDesc(session.getId(), PAID_STATUSES);
        PaymentExitValidationResponse response;
        if (completedPayment.isEmpty() || completedPayment.get().getPaymentTime() == null) {
            response = new PaymentExitValidationResponse(
                    session.getId(),
                    session.getVehicle().getPlateNumber(),
                    null,
                    "UNPAID",
                    null,
                    null,
                    0,
                    false,
                    "DENY_PAYMENT_REQUIRED"
            );
        } else {
            Payment payment = completedPayment.get();
            LocalDateTime deadline = payment.getPaymentTime().plusMinutes(PaymentGatewayService.EXIT_WINDOW_MINUTES);
            boolean withinWindow = !detectedAt.isAfter(deadline);
            long remainingSeconds = withinWindow
                    ? Math.max(0, Duration.between(detectedAt, deadline).getSeconds())
                    : 0;
            response = new PaymentExitValidationResponse(
                    session.getId(),
                    session.getVehicle().getPlateNumber(),
                    payment.getId(),
                    payment.getStatus(),
                    payment.getPaymentTime(),
                    deadline,
                    remainingSeconds,
                    withinWindow,
                    withinWindow ? "OPEN_PAYMENT_VERIFIED" : "DENY_EXIT_WINDOW_EXPIRED"
            );
            if (withinWindow && !"COMPLETED".equalsIgnoreCase(session.getStatus())) {
                parkingSessionService.completePaidExit(session.getId(), null, null);
            }
        }
        realtimeEventService.publish(
                "/topic/parking-sessions",
                response.openBarrier() ? "BARRIER_OPEN_APPROVED" : "BARRIER_OPEN_DENIED",
                response.decision(),
                response
        );
        return response;
    }

    @Transactional
    public ParkingSessionResponse completeExit(Long sessionId, Long staffId, String exitGateCode) {
        PaymentModuleParkingSession session = parkingSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Parking session not found: " + sessionId));
        if (paymentRepository.findFirstBySessionIdAndStatusInOrderByPaymentTimeDesc(sessionId, PAID_STATUSES).isEmpty()) {
            throw new BadRequestException("Cannot complete exit before payment");
        }
        return parkingSessionService.completePaidExit(session.getId(), staffId, exitGateCode);
    }

    private PaymentModuleParkingSession findLatestSession(String licensePlate) {
        return parkingSessionRepository
                .findFirstByVehiclePlateNumberIgnoreCaseOrderByEntryTimeDesc(licensePlate.trim())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Parking session not found for license plate: " + licensePlate));
    }

    private Optional<Payment> findRelevantPayment(Long sessionId) {
        Optional<Payment> completed = paymentRepository
                .findFirstBySessionIdAndStatusInOrderByPaymentTimeDesc(sessionId, PAID_STATUSES);
        return completed.isPresent()
                ? completed
                : paymentRepository.findFirstBySessionIdOrderByPaymentTimeDesc(sessionId);
    }

    private PaymentCheckoutResponse buildCheckoutResponse(
            ParkingSessionResponse session,
            Payment payment) {
        boolean paid = payment != null && payment.getStatus() != null
                && PAID_STATUSES.contains(payment.getStatus().toUpperCase());
        LocalDateTime paidAt = paid ? payment.getPaymentTime() : null;
        LocalDateTime deadline = paidAt == null
                ? null
                : paidAt.plusMinutes(PaymentGatewayService.EXIT_WINDOW_MINUTES);
        return new PaymentCheckoutResponse(
                session.id(),
                session.licensePlate(),
                session.entryTime(),
                session.exitTime(),
                session.parkingFee(),
                session.penaltyFee(),
                session.totalFee(),
                session.status(),
                payment == null ? null : payment.getId(),
                payment == null ? null : payment.getPaymentMethod(),
                payment == null ? "UNPAID" : payment.getStatus(),
                paid,
                paidAt,
                deadline,
                PaymentGatewayService.EXIT_WINDOW_MINUTES
        );
    }
}
