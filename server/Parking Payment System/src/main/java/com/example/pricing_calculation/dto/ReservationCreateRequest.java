package com.example.pricing_calculation.dto;

import java.time.LocalDateTime;

public record ReservationCreateRequest(
        Long userId,
        Long vehicleId,
        Long zoneId,
        LocalDateTime startTime,
        LocalDateTime endTime
) {
}
