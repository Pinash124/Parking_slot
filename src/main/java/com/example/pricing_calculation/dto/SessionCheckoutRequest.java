package com.example.pricing_calculation.dto;

import java.time.LocalDateTime;

public record SessionCheckoutRequest(
        LocalDateTime exitTime,
        boolean lostTicket,
        Integer overtimeMinutes
) {
}
