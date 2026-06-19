package com.example.pricing_calculation.dto;

import com.example.pricing_calculation.domain.Payment;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        Long sessionId,
        BigDecimal amount,
        String paymentMethod,
        LocalDateTime paymentTime,
        String status
) {

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getSession() == null ? null : payment.getSession().getId(),
                payment.getAmount(),
                payment.getPaymentMethod(),
                payment.getPaymentTime(),
                payment.getStatus()
        );
    }
}
