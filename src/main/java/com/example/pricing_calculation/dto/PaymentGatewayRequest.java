package com.example.pricing_calculation.dto;

import java.math.BigDecimal;

public record PaymentGatewayRequest(
        Long sessionId,
        BigDecimal amount,
        String returnUrl,
        String orderInfo
) {
}
