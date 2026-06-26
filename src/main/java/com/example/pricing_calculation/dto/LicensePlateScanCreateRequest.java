package com.example.pricing_calculation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LicensePlateScanCreateRequest(
        Long sessionId,
        String laneCode,
        String plateNumber,
        String imageUrl,
        BigDecimal confidenceScore,
        LocalDateTime scanTime
) {
}
