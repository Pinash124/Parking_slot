package com.example.pricing_calculation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RevenueTrendResponse(
        String period,
        LocalDate startDate,
        LocalDate endDate,
        List<RevenueTrendPoint> points) {

    public record RevenueTrendPoint(
            String key,
            String label,
            BigDecimal revenue,
            long payments) {
    }
}
