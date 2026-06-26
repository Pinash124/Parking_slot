package com.example.pricing_calculation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentCreateRequest(
        Long sessionId,
        BigDecimal amount,
        String paymentMethod,
        LocalDateTime paymentTime,
        String status,
        String gateway,
        String referenceCode
) {
}
