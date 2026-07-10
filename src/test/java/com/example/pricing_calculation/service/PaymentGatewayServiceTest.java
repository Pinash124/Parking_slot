package com.example.pricing_calculation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.pricing_calculation.domain.Payment;
import com.example.pricing_calculation.domain.TransactionHistory;
import com.example.pricing_calculation.domain.UserAccount;
import com.example.pricing_calculation.dto.MonthlyParkingPassDtos.MonthlyParkingPassResponse;
import com.example.pricing_calculation.dto.PaymentGatewayRequest;
import com.example.pricing_calculation.dto.PaymentGatewayResponse;
import com.example.pricing_calculation.dto.PaymentResponse;
import com.example.pricing_calculation.repository.PaymentRepository;
import com.example.pricing_calculation.repository.TransactionHistoryRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class PaymentGatewayServiceTest {

        @Test
        void createsSignedVnpayUrlAndUsesItAsQrContent() throws Exception {
                PaymentService paymentService = mock(PaymentService.class);
                when(paymentService.create(any())).thenReturn(new PaymentResponse(
                                1L, 9L, new BigDecimal("70000"), "VNPAY",
                                LocalDateTime.now(), "PENDING"));
                String secret = "test-hash-secret";
                PaymentGatewayService service = new PaymentGatewayService(
                                paymentService,
                                mock(PaymentRepository.class),
                                mock(TransactionHistoryRepository.class),
                                mock(MonthlyParkingPassService.class),
                                mock(RealtimeEventService.class),
                                mock(AuditLogService.class),
                                "TESTCODE",
                                secret,
                                "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html",
                                "https://merchant.example/api/payment-gateways/vnpay/return",
                                "/payment/vnpay-personal-qr.png");

                PaymentGatewayResponse response = service.createVnpayPayment(
                                new PaymentGatewayRequest(9L, new BigDecimal("70000"), null, "Parking payment"),
                                "127.0.0.1");

                assertEquals(response.paymentUrl(), response.qrContent());
                String query = response.paymentUrl().substring(response.paymentUrl().indexOf('?') + 1);
                int hashSeparator = query.lastIndexOf("&vnp_SecureHash=");
                String signedData = query.substring(0, hashSeparator);
                String actualHash = query.substring(hashSeparator + "&vnp_SecureHash=".length());
                assertEquals(hmacSha512(secret, signedData), actualHash);
                assertTrue(signedData.contains("vnp_Amount=7000000"));
                assertTrue(signedData.contains("vnp_TmnCode=TESTCODE"));
                assertTrue(signedData.contains("vnp_Version=2.1.0"));
                assertTrue(signedData.contains(
                                "vnp_ReturnUrl=https%3A%2F%2Fmerchant.example%2Fapi%2Fpayment-gateways%2Fvnpay%2Freturn"));
        }

        @Test
        void createsPendingPersonalQrPaymentForManualConfirmation() {
                PaymentService paymentService = mock(PaymentService.class);
                when(paymentService.create(any())).thenReturn(new PaymentResponse(
                                128L, 9L, new BigDecimal("70000"), "PERSONAL_QR",
                                LocalDateTime.now(), "PENDING"));
                PaymentGatewayService service = new PaymentGatewayService(
                                paymentService,
                                mock(PaymentRepository.class),
                                mock(TransactionHistoryRepository.class),
                                mock(MonthlyParkingPassService.class),
                                mock(RealtimeEventService.class),
                                mock(AuditLogService.class),
                                "",
                                "",
                                "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html",
                                "https://merchant.example/api/payment-gateways/vnpay/return",
                                "/payment/vnpay-personal-qr.png");

                PaymentGatewayResponse response = service.createPersonalQrPayment(
                                new PaymentGatewayRequest(9L, new BigDecimal("70000"), null, "Parking payment"));

                assertEquals("PERSONAL_QR", response.gateway());
                assertEquals("PENDING", response.status());
                assertEquals("/payment/vnpay-personal-qr.png", response.qrImageUrl());
                assertEquals("PARKING-128", response.transferContent());
                assertEquals(new BigDecimal("70000"), response.amount());
        }

        @Test
        void usesConfiguredReturnUrlWhenCreatingVnpayPayment() {
                PaymentService paymentService = mock(PaymentService.class);
                when(paymentService.create(any())).thenReturn(new PaymentResponse(
                                1L, 9L, new BigDecimal("70000"), "VNPAY",
                                LocalDateTime.now(), "PENDING"));
                PaymentGatewayService service = new PaymentGatewayService(
                                paymentService,
                                mock(PaymentRepository.class),
                                mock(TransactionHistoryRepository.class),
                                mock(MonthlyParkingPassService.class),
                                mock(RealtimeEventService.class),
                                mock(AuditLogService.class),
                                "TESTCODE",
                                "test-hash-secret",
                                "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html",
                                "https://merchant.example/api/payment-gateways/vnpay/return",
                                "/payment/vnpay-personal-qr.png");

                PaymentGatewayResponse response = service.createVnpayPayment(
                                new PaymentGatewayRequest(
                                                9L,
                                                new BigDecimal("70000"),
                                                "http://localhost:5173/payment-return",
                                                "Parking payment"),
                                "127.0.0.1");

                assertTrue(response.paymentUrl().contains(
                                "vnp_ReturnUrl=https%3A%2F%2Fmerchant.example%2Fapi%2Fpayment-gateways%2Fvnpay%2Freturn"));
        }

        @Test
        void personalQrEndpointAutomaticallyUsesVnpayWhenConfigured() {
                PaymentService paymentService = mock(PaymentService.class);
                when(paymentService.create(any())).thenReturn(new PaymentResponse(
                                128L, 9L, new BigDecimal("70000"), "VNPAY",
                                LocalDateTime.now(), "PENDING"));
                PaymentGatewayService service = new PaymentGatewayService(
                                paymentService,
                                mock(PaymentRepository.class),
                                mock(TransactionHistoryRepository.class),
                                mock(MonthlyParkingPassService.class),
                                mock(RealtimeEventService.class),
                                mock(AuditLogService.class),
                                "TESTCODE",
                                "test-hash-secret",
                                "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html",
                                "https://merchant.example/api/payment-gateways/vnpay/return",
                                "/payment/vnpay-personal-qr.png");

                PaymentGatewayResponse response = service.createPersonalQrPayment(
                                new PaymentGatewayRequest(9L, new BigDecimal("70000"), null, "Parking payment"));

                assertEquals("VNPAY", response.gateway());
                assertEquals(response.paymentUrl(), response.qrContent());
                assertTrue(response.qrImageUrl().contains("api.qrserver.com"));
                assertTrue(response.referenceCode().startsWith("VNPAY"));
        }

        @Test
        void createsMonthlyPassVnpayPaymentWithoutParkingSessionPayment() {
                MonthlyParkingPassService monthlyPassService = mock(MonthlyParkingPassService.class);
                when(monthlyPassService.prepareVnpayPayment(any(), any(), anyString())).thenReturn(
                                new MonthlyParkingPassResponse(
                                                6L, 2L, 9L, "59A-12345", 1L, "CAR",
                                                10L, "F1-CAR-001", "MONTHLY_HELD", 1,
                                                new BigDecimal("500000"), new BigDecimal("500000"),
                                                java.time.LocalDate.now(),
                                                java.time.LocalDate.now().plusMonths(1).minusDays(1),
                                                "PENDING_PAYMENT", "PENDING", "VNPAY", "MTHVNPAY-TEST",
                                                null, 30L, false, null, null,
                                                LocalDateTime.now(), LocalDateTime.now()));
                String secret = "test-hash-secret";
                PaymentGatewayService service = new PaymentGatewayService(
                                mock(PaymentService.class),
                                mock(PaymentRepository.class),
                                mock(TransactionHistoryRepository.class),
                                monthlyPassService,
                                mock(RealtimeEventService.class),
                                mock(AuditLogService.class),
                                "TESTCODE",
                                secret,
                                "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html",
                                "https://merchant.example/api/payment-gateways/vnpay/return",
                                "/payment/vnpay-personal-qr.png");

                PaymentGatewayResponse response = service.createMonthlyPassVnpayPayment(
                                mock(UserAccount.class), 6L, "127.0.0.1");

                assertEquals("VNPAY", response.gateway());
                assertEquals("PENDING", response.status());
                assertEquals(response.paymentUrl(), response.qrContent());
                assertEquals(new BigDecimal("500000"), response.amount());
                assertTrue(response.referenceCode().startsWith("MTHVNPAY"));
        }

        @Test
        void acceptsValidCallbackAndCompletesPayment() throws Exception {
                PaymentRepository paymentRepository = mock(PaymentRepository.class);
                TransactionHistoryRepository transactionRepository = mock(TransactionHistoryRepository.class);
                Payment payment = new Payment();
                payment.setAmount(new BigDecimal("70000"));
                payment.setPaymentMethod("VNPAY");
                payment.setPaymentTime(LocalDateTime.now());
                payment.setStatus("PENDING");
                TransactionHistory transaction = new TransactionHistory();
                transaction.setPayment(payment);
                transaction.setGateway("VNPAY");
                transaction.setReferenceCode("VNPAY202606290001ABC12345");
                transaction.setStatus("PENDING");
                when(transactionRepository.findByReferenceCodeIgnoreCase(transaction.getReferenceCode()))
                                .thenReturn(java.util.Optional.of(transaction));
                when(paymentRepository.save(org.mockito.ArgumentMatchers.<Payment>any()))
                                .thenAnswer(invocation -> (Payment) invocation.getArgument(0));
                String secret = "test-hash-secret";
                PaymentGatewayService service = new PaymentGatewayService(
                                mock(PaymentService.class),
                                paymentRepository,
                                transactionRepository,
                                mock(MonthlyParkingPassService.class),
                                mock(RealtimeEventService.class),
                                mock(AuditLogService.class),
                                "TESTCODE",
                                secret,
                                "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html",
                                "https://merchant.example/api/payment-gateways/vnpay/return",
                                "/payment/vnpay-personal-qr.png");

                Map<String, String> callback = new TreeMap<>();
                callback.put("vnp_Amount", "7000000");
                callback.put("vnp_ResponseCode", "00");
                callback.put("vnp_TmnCode", "TESTCODE");
                callback.put("vnp_TransactionStatus", "00");
                callback.put("vnp_TxnRef", transaction.getReferenceCode());
                callback.put("vnp_SecureHash", hmacSha512(secret, encode(callback)));

                PaymentGatewayResponse response = service.processVnpayCallback(callback);

                assertEquals("COMPLETED", response.status());
                assertEquals("COMPLETED", transaction.getStatus());
                verify(paymentRepository).save(payment);
                verify(transactionRepository).save(transaction);
        }

        @Test
        void acceptsMonthlyPassVnpayCallbackAndConfirmsPass() throws Exception {
                MonthlyParkingPassService monthlyPassService = mock(MonthlyParkingPassService.class);

                when(monthlyPassService.amountByPaymentReference("MTHVNPAY202607080001ABC12345"))
                                .thenReturn(new BigDecimal("500000"));

                MonthlyParkingPassResponse confirmedPass = new MonthlyParkingPassResponse(
                                6L, 2L, 9L, "59A-12345", 1L, "CAR",
                                10L, "F1-CAR-001", "MONTHLY_HELD", 1,
                                new BigDecimal("500000"), new BigDecimal("500000"),
                                java.time.LocalDate.now(),
                                java.time.LocalDate.now().plusMonths(1).minusDays(1),
                                "ACTIVE", "PAID", "VNPAY", "MTHVNPAY202607080001ABC12345",
                                null, 30L, false, null, null,
                                LocalDateTime.now(), LocalDateTime.now());

                when(monthlyPassService.confirmVnpayPayment("MTHVNPAY202607080001ABC12345"))
                                .thenReturn(confirmedPass);

                String secret = "test-hash-secret";

                PaymentGatewayService service = new PaymentGatewayService(
                                mock(PaymentService.class),
                                mock(PaymentRepository.class),
                                mock(TransactionHistoryRepository.class),
                                monthlyPassService,
                                mock(RealtimeEventService.class),
                                mock(AuditLogService.class),
                                "TESTCODE",
                                secret,
                                "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html",
                                "https://merchant.example/api/payment-gateways/vnpay/return",
                                "/payment/vnpay-personal-qr.png");

                Map<String, String> callback = new TreeMap<>();
                callback.put("vnp_Amount", "50000000");
                callback.put("vnp_ResponseCode", "00");
                callback.put("vnp_TmnCode", "TESTCODE");
                callback.put("vnp_TransactionStatus", "00");
                callback.put("vnp_TxnRef", "MTHVNPAY202607080001ABC12345");
                callback.put("vnp_SecureHash", hmacSha512(secret, encode(callback)));

                PaymentGatewayResponse response = service.processVnpayCallback(callback);

                assertEquals("COMPLETED", response.status());
                assertEquals("MTHVNPAY202607080001ABC12345", response.referenceCode());
                verify(monthlyPassService).confirmVnpayPayment("MTHVNPAY202607080001ABC12345");
        }

        private String hmacSha512(String secret, String data) throws Exception {
                Mac hmac = Mac.getInstance("HmacSHA512");
                hmac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
                StringBuilder result = new StringBuilder();
                for (byte value : hmac.doFinal(data.getBytes(StandardCharsets.UTF_8))) {
                        result.append(String.format("%02x", value & 0xff));
                }
                return result.toString();
        }

        private String encode(Map<String, String> parameters) {
                return parameters.entrySet().stream()
                                .filter(entry -> !"vnp_SecureHash".equals(entry.getKey()))
                                .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8)
                                                + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                                .reduce((left, right) -> left + "&" + right)
                                .orElse("");
        }
}
