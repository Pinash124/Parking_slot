package com.example.pricing_calculation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.pricing_calculation.domain.ParkingSession;
import com.example.pricing_calculation.domain.ParkingSlot;
import com.example.pricing_calculation.domain.Payment;
import com.example.pricing_calculation.domain.Vehicle;
import com.example.pricing_calculation.dto.ParkingSessionResponse;
import com.example.pricing_calculation.dto.PaymentCheckoutPrepareRequest;
import com.example.pricing_calculation.dto.PaymentCheckoutResponse;
import com.example.pricing_calculation.dto.PaymentExitValidationRequest;
import com.example.pricing_calculation.dto.PaymentExitValidationResponse;
import com.example.pricing_calculation.dto.SessionCheckoutRequest;
import com.example.pricing_calculation.repository.ParkingSessionRepository;
import com.example.pricing_calculation.repository.PaymentRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PaymentCheckoutServiceTest {

    private final ParkingSessionRepository sessionRepository = mock(ParkingSessionRepository.class);
    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final ParkingSessionService parkingSessionService = mock(ParkingSessionService.class);
    private final RealtimeEventService realtimeEventService = mock(RealtimeEventService.class);
    private final PaymentCheckoutService service = new PaymentCheckoutService(
            sessionRepository,
            paymentRepository,
            parkingSessionService,
            realtimeEventService
    );

    @Test
    void prepareByPlateChecksOutActiveSessionAndReturnsCalculatedFee() {
        ParkingSession session = session(31L, "51A12345", "ACTIVE");
        when(sessionRepository.findFirstByVehiclePlateNumberIgnoreCaseOrderByEntryTimeDesc("51A12345"))
                .thenReturn(Optional.of(session));
        ParkingSessionResponse checkedOut = sessionResponse(31L, "51A12345", "CHECKED_OUT");
        when(parkingSessionService.checkout(eq(31L), org.mockito.ArgumentMatchers.any(SessionCheckoutRequest.class)))
                .thenReturn(checkedOut);
        when(paymentRepository.findFirstBySessionIdAndStatusInOrderByPaymentTimeDesc(eq(31L), anyCollection()))
                .thenReturn(Optional.empty());
        when(paymentRepository.findFirstBySessionIdOrderByPaymentTimeDesc(31L)).thenReturn(Optional.empty());

        PaymentCheckoutResponse response = service.prepare(
                new PaymentCheckoutPrepareRequest(
                        "51A12345",
                        LocalDateTime.of(2026, 6, 17, 12, 0),
                        false,
                        0
                )
        );

        assertEquals("CHECKED_OUT", response.sessionStatus());
        assertEquals(new BigDecimal("30000.00"), response.totalFee());
        assertEquals("UNPAID", response.paymentStatus());
        assertFalse(response.paid());
        verify(parkingSessionService).checkout(eq(31L), org.mockito.ArgumentMatchers.any(SessionCheckoutRequest.class));
    }

    @Test
    void completedPaymentStatusIncludesFifteenMinuteDeadline() {
        ParkingSession session = session(32L, "51A12346", "CHECKED_OUT");
        Payment payment = payment(32L, LocalDateTime.of(2026, 6, 17, 12, 0));
        when(sessionRepository.findById(32L)).thenReturn(Optional.of(session));
        when(paymentRepository.findFirstBySessionIdAndStatusInOrderByPaymentTimeDesc(eq(32L), anyCollection()))
                .thenReturn(Optional.of(payment));

        PaymentCheckoutResponse response = service.status(32L);

        assertTrue(response.paid());
        assertEquals("COMPLETED", response.paymentStatus());
        assertEquals(LocalDateTime.of(2026, 6, 17, 12, 15), response.exitDeadline());
        assertEquals(15, response.exitWindowMinutes());
    }

    @Test
    void exitValidationOpensBarrierInsidePaymentWindow() {
        ParkingSession session = session(33L, "51A12347", "CHECKED_OUT");
        Payment payment = payment(33L, LocalDateTime.of(2026, 6, 17, 12, 0));
        when(sessionRepository.findFirstByVehiclePlateNumberIgnoreCaseOrderByEntryTimeDesc("51A12347"))
                .thenReturn(Optional.of(session));
        when(paymentRepository.findFirstBySessionIdAndStatusInOrderByPaymentTimeDesc(eq(33L), anyCollection()))
                .thenReturn(Optional.of(payment));

        PaymentExitValidationResponse response = service.validateExit(
                new PaymentExitValidationRequest("51A12347", LocalDateTime.of(2026, 6, 17, 12, 10))
        );

        assertTrue(response.openBarrier());
        assertEquals("OPEN_PAYMENT_VERIFIED", response.decision());
        assertEquals(300, response.remainingSeconds());
    }

    @Test
    void exitValidationRejectsExpiredPaymentWindow() {
        ParkingSession session = session(34L, "51A12348", "CHECKED_OUT");
        Payment payment = payment(34L, LocalDateTime.of(2026, 6, 17, 12, 0));
        when(sessionRepository.findFirstByVehiclePlateNumberIgnoreCaseOrderByEntryTimeDesc("51A12348"))
                .thenReturn(Optional.of(session));
        when(paymentRepository.findFirstBySessionIdAndStatusInOrderByPaymentTimeDesc(eq(34L), anyCollection()))
                .thenReturn(Optional.of(payment));

        PaymentExitValidationResponse response = service.validateExit(
                new PaymentExitValidationRequest("51A12348", LocalDateTime.of(2026, 6, 17, 12, 16))
        );

        assertFalse(response.openBarrier());
        assertEquals("DENY_EXIT_WINDOW_EXPIRED", response.decision());
    }

    @Test
    void exitValidationRejectsUnpaidVehicle() {
        ParkingSession session = session(35L, "51A12349", "CHECKED_OUT");
        when(sessionRepository.findFirstByVehiclePlateNumberIgnoreCaseOrderByEntryTimeDesc("51A12349"))
                .thenReturn(Optional.of(session));
        when(paymentRepository.findFirstBySessionIdAndStatusInOrderByPaymentTimeDesc(eq(35L), anyCollection()))
                .thenReturn(Optional.empty());

        PaymentExitValidationResponse response = service.validateExit(
                new PaymentExitValidationRequest("51A12349", LocalDateTime.of(2026, 6, 17, 12, 10))
        );

        assertFalse(response.openBarrier());
        assertEquals("DENY_PAYMENT_REQUIRED", response.decision());
    }

    private ParkingSession session(Long id, String plate, String status) {
        ParkingSession session = mock(ParkingSession.class);
        Vehicle vehicle = mock(Vehicle.class);
        ParkingSlot slot = mock(ParkingSlot.class);
        when(session.getId()).thenReturn(id);
        when(session.getVehicle()).thenReturn(vehicle);
        when(session.getSlot()).thenReturn(slot);
        when(session.getEntryTime()).thenReturn(LocalDateTime.of(2026, 6, 17, 10, 0));
        when(session.getExitTime()).thenReturn(LocalDateTime.of(2026, 6, 17, 12, 0));
        when(session.getParkingFee()).thenReturn(new BigDecimal("30000.00"));
        when(session.getPenaltyFee()).thenReturn(BigDecimal.ZERO.setScale(2));
        when(session.getTotalFee()).thenReturn(new BigDecimal("30000.00"));
        when(session.getStatus()).thenReturn(status);
        when(vehicle.getPlateNumber()).thenReturn(plate);
        when(slot.getSlotCode()).thenReturn("A-01");
        return session;
    }

    private ParkingSessionResponse sessionResponse(Long id, String plate, String status) {
        return new ParkingSessionResponse(
                id,
                1L,
                2L,
                plate,
                3L,
                "A-01",
                "T-01",
                LocalDateTime.of(2026, 6, 17, 10, 0),
                LocalDateTime.of(2026, 6, 17, 12, 0),
                new BigDecimal("30000.00"),
                BigDecimal.ZERO.setScale(2),
                new BigDecimal("30000.00"),
                status
        );
    }

    private Payment payment(Long sessionId, LocalDateTime paidAt) {
        Payment payment = mock(Payment.class);
        when(payment.getId()).thenReturn(sessionId + 100);
        when(payment.getStatus()).thenReturn("COMPLETED");
        when(payment.getPaymentMethod()).thenReturn("MOMO");
        when(payment.getPaymentTime()).thenReturn(paidAt);
        return payment;
    }
}
