package com.example.pricing_calculation.dto;

import java.math.BigDecimal;

public class TransactionHistorySummaryResponse {

    private final long totalTransactions;
    private final long completedTransactions;
    private final long pendingTransactions;
    private final long cancelledTransactions;
    private final long failedTransactions;
    private final long refundedTransactions;
    private final long reservationTransactions;
    private final long paymentTransactions;
    private final long sessionTransactions;
    private final long slotChangeTransactions;
    private final BigDecimal totalRevenue;
    private final BigDecimal averageAmount;

    public TransactionHistorySummaryResponse(
            long totalTransactions,
            long completedTransactions,
            long pendingTransactions,
            long cancelledTransactions,
            long failedTransactions,
            long refundedTransactions,
            long reservationTransactions,
            long paymentTransactions,
            long sessionTransactions,
            long slotChangeTransactions,
            BigDecimal totalRevenue,
            BigDecimal averageAmount) {
        this.totalTransactions = totalTransactions;
        this.completedTransactions = completedTransactions;
        this.pendingTransactions = pendingTransactions;
        this.cancelledTransactions = cancelledTransactions;
        this.failedTransactions = failedTransactions;
        this.refundedTransactions = refundedTransactions;
        this.reservationTransactions = reservationTransactions;
        this.paymentTransactions = paymentTransactions;
        this.sessionTransactions = sessionTransactions;
        this.slotChangeTransactions = slotChangeTransactions;
        this.totalRevenue = totalRevenue;
        this.averageAmount = averageAmount;
    }

    public long getTotalTransactions() {
        return totalTransactions;
    }

    public long getCompletedTransactions() {
        return completedTransactions;
    }

    public long getPendingTransactions() {
        return pendingTransactions;
    }

    public long getCancelledTransactions() {
        return cancelledTransactions;
    }

    public long getFailedTransactions() {
        return failedTransactions;
    }

    public long getRefundedTransactions() {
        return refundedTransactions;
    }

    public long getReservationTransactions() {
        return reservationTransactions;
    }

    public long getPaymentTransactions() {
        return paymentTransactions;
    }

    public long getSessionTransactions() {
        return sessionTransactions;
    }

    public long getSlotChangeTransactions() {
        return slotChangeTransactions;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public BigDecimal getAverageAmount() {
        return averageAmount;
    }
}
