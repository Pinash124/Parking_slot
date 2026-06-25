package com.example.pricing_calculation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.pricing_calculation.domain.ParkingSession;
import com.example.pricing_calculation.domain.Payment;
import com.example.pricing_calculation.domain.TransactionHistory;
import com.example.pricing_calculation.dto.PaymentGatewayConfirmRequest;
import com.example.pricing_calculation.dto.PaymentGatewayRequest;
import com.example.pricing_calculation.dto.PaymentGatewayResponse;
import com.example.pricing_calculation.dto.PaymentResponse;
import com.example.pricing_calculation.repository.PaymentRepository;
import com.example.pricing_calculation.repository.TransactionHistoryRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PaymentGatewayServiceTest {

    private final PaymentService paymentService = mock(PaymentService.class);
    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final TransactionHistoryRepository transactionRepository = mock(TransactionHistoryRepository.class);
    private final RealtimeEventService realtimeEventService = mock(RealtimeEventService.class);
    private final PaymentGatewayService service = new PaymentGatewayService(
            paymentService,
            paymentRepository,
            transactionRepository,
            realtimeEventService
    );

    @Test
    void cashCompletesImmediatelyAndReturnsExitDeadline() {
        LocalDateTime paidAt = LocalDateTime.of(2026, 6, 17, 10, 0);
        PaymentResponse payment = payment(1L, 11L, "CASH", "COMPLETED", paidAt);
        when(paymentService.create(any())).thenReturn(payment);

        PaymentGatewayResponse response = service.createCashPayment(
                new PaymentGatewayRequest(11L, null, null, "cash")
        );

        assertEquals("CASH", response.gateway());
        assertEquals("COMPLETED", response.status());
        assertNull(response.paymentUrl());
        assertTrue(response.referenceCode().startsWith("CASH-"));
        assertEquals(paidAt.plusMinutes(15), response.exitDeadline());
    }

    @Test
    void momoCreatesPendingPaymentUrlAndQrContent() {
        PaymentResponse payment = payment(2L, 12L, "MOMO", "PENDING", LocalDateTime.now());
        when(paymentService.create(any())).thenReturn(payment);

        PaymentGatewayResponse response = service.createMomoPayment(
                new PaymentGatewayRequest(
                        12L,
                        BigDecimal.valueOf(45000),
                        "https://client.test/return?order=1&source=web",
                        "momo order"
                )
        );

        assertEquals("MOMO", response.gateway());
        assertEquals("PENDING", response.status());
        assertTrue(response.paymentUrl().startsWith("https://sandbox.momo.vn/"));
        assertTrue(response.paymentUrl().contains("returnUrl="));
        assertTrue(response.qrContent().startsWith("MOMO:"));
        assertNull(response.exitDeadline());
    }

    @Test
    void vnpayCreatesPendingPaymentUrlAndQrContent() {
        PaymentResponse payment = payment(3L, 13L, "VNPAY", "PENDING", LocalDateTime.now());
        when(paymentService.create(any())).thenReturn(payment);

        PaymentGatewayResponse response = service.createVnpayPayment(
                new PaymentGatewayRequest(13L, null, "https://client.test/return", "vnpay order")
        );

        assertEquals("VNPAY", response.gateway());
        assertEquals("PENDING", response.status());
        assertTrue(response.paymentUrl().startsWith("https://sandbox.vnpayment.vn/"));
        assertTrue(response.qrContent().startsWith("VNPAY:"));
    }

    @Test
    void successfulCallbackCompletesPaymentAndStartsFifteenMinuteWindow() {
        ParkingSession session = mock(ParkingSession.class);
        when(session.getId()).thenReturn(21L);
        Payment payment = new Payment();
        payment.setSession(session);
        payment.setAmount(BigDecimal.valueOf(30000));
        payment.setPaymentMethod("MOMO");
        payment.setPaymentTime(LocalDateTime.now().minusMinutes(2));
        payment.setStatus("PENDING");
        TransactionHistory transaction = new TransactionHistory();
        transaction.setPayment(payment);
        transaction.setGateway("MOMO");
        transaction.setReferenceCode("MOMO-CALLBACK-1");
        transaction.setStatus("PENDING");
        when(transactionRepository.findByReferenceCodeIgnoreCase("MOMO-CALLBACK-1"))
                .thenReturn(Optional.of(transaction));
        when(paymentRepository.save(payment)).thenReturn(payment);

        LocalDateTime beforeCallback = LocalDateTime.now();
        PaymentGatewayResponse response = service.confirmOnlinePayment(
                "momo",
                new PaymentGatewayConfirmRequest("MOMO-CALLBACK-1", "SUCCESS", "GW-1", null)
        );

        assertEquals("COMPLETED", response.status());
        assertEquals("COMPLETED", payment.getStatus());
        assertEquals("COMPLETED", transaction.getStatus());
        assertTrue(!payment.getPaymentTime().isBefore(beforeCallback));
        assertEquals(payment.getPaymentTime().plusMinutes(15), response.exitDeadline());
        assertTrue(response.message().contains("15 minutes"));
        verify(transactionRepository).save(transaction);
    }

    @Test
    void callbackRejectsReferenceFromAnotherGateway() {
        TransactionHistory transaction = new TransactionHistory();
        transaction.setGateway("VNPAY");
        transaction.setReferenceCode("VNPAY-1");
        when(transactionRepository.findByReferenceCodeIgnoreCase("VNPAY-1"))
                .thenReturn(Optional.of(transaction));

        assertThrows(
                BadRequestException.class,
                () -> service.confirmOnlinePayment(
                        "momo",
                        new PaymentGatewayConfirmRequest("VNPAY-1", "SUCCESS", null, null)
                )
        );
    }

    private PaymentResponse payment(
            Long id,
            Long sessionId,
            String method,
            String status,
            LocalDateTime paidAt) {
        PaymentResponse response = new PaymentResponse(
                id,
                sessionId,
                BigDecimal.valueOf(30000),
                method,
                paidAt,
                status
        );
        assertNotNull(response);
        return response;
    }
}
