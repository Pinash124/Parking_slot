package com.example.pricing_calculation.dto;

import com.example.pricing_calculation.domain.LicensePlateScan;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LicensePlateScanResponse(
        Long id,
        Long sessionId,
        String laneCode,
        String plateNumber,
        String imageUrl,
        BigDecimal confidenceScore,
        LocalDateTime scanTime
) {

    public static LicensePlateScanResponse from(LicensePlateScan scan) {
        return new LicensePlateScanResponse(
                scan.getId(),
                scan.getSession() == null ? null : scan.getSession().getId(),
                scan.getLaneCode(),
                scan.getPlateNumber(),
                scan.getImageUrl(),
                scan.getConfidenceScore(),
                scan.getScanTime()
        );
    }
}
