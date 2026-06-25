package com.example.pricing_calculation.dto;

import java.time.LocalDateTime;

public record PaymentCheckoutPrepareRequest(
        String licensePlate,
        LocalDateTime exitTime,
        boolean lostTicket,
        Integer overtimeMinutes
) {
}
