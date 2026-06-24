package com.example.pricing_calculation.dto;

import java.time.LocalDateTime;

public record PaymentExitValidationResponse(
        Long sessionId,
        String licensePlate,
        Long paymentId,
        String paymentStatus,
        LocalDateTime paidAt,
        LocalDateTime exitDeadline,
        long remainingSeconds,
        boolean openBarrier,
        String decision
) {
}
