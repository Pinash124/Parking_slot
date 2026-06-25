package com.example.pricing_calculation.dto;

import java.time.LocalDateTime;

public record PaymentGatewayResponse(
        String gateway,
        Long paymentId,
        String referenceCode,
        String status,
        String paymentUrl,
        String qrContent,
        String message,
        PaymentResponse payment,
        LocalDateTime exitDeadline
) {
}
