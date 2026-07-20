package com.example.pricing_calculation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PricingQuoteResponse(
        Long vehicleTypeId,
        String vehicleType,
        Long policyId,
        String policyName,
        LocalDateTime entryTime,
        LocalDateTime exitTime,
        long durationMinutes,
        long billableHours,
        BigDecimal hourlyRate,
        BigDecimal dailyRate,
        BigDecimal parkingFee,
        BigDecimal lostTicketFee,
        BigDecimal overtimeFee,
        BigDecimal fixedSurcharge,
        BigDecimal penaltyFee,
        BigDecimal totalFee,
        String currency
) {
}
