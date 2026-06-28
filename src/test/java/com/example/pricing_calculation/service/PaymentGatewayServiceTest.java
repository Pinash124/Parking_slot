package com.example.pricing_calculation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.pricing_calculation.domain.Payment;
import com.example.pricing_calculation.domain.TransactionHistory;
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
                mock(RealtimeEventService.class),
                "TESTCODE",
                secret,
                "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html",
                "https://merchant.example/api/payment-gateways/vnpay/return");

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
        assertTrue(signedData.contains("vnp_ReturnUrl=https%3A%2F%2Fmerchant.example%2Fapi%2Fpayment-gateways%2Fvnpay%2Freturn"));
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
        when(paymentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        String secret = "test-hash-secret";
        PaymentGatewayService service = new PaymentGatewayService(
                mock(PaymentService.class), paymentRepository, transactionRepository,
                mock(RealtimeEventService.class), "TESTCODE", secret,
                "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html",
                "https://merchant.example/api/payment-gateways/vnpay/return");

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


