package com.example.pricing_calculation.dto;

import java.math.BigDecimal;

public record DashboardOverviewResponse(
        long totalReservations,
        long pendingReservations,
        long approvedReservations,
        long activeSessions,
        long availableSlots,
        long occupiedSlots,
        long reservedSlots,
        long pendingPayments,
        long completedPayments,
        BigDecimal todayRevenue,
        BigDecimal monthRevenue,
        long totalTransactions
) {
}
