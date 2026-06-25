package com.example.pricing_calculation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentCheckoutResponse(
        Long sessionId,
        String licensePlate,
        LocalDateTime entryTime,
        LocalDateTime exitTime,
        BigDecimal parkingFee,
        BigDecimal penaltyFee,
        BigDecimal totalFee,
        String sessionStatus,
        Long paymentId,
        String paymentMethod,
        String paymentStatus,
        boolean paid,
        LocalDateTime paidAt,
        LocalDateTime exitDeadline,
        int exitWindowMinutes
) {
}
