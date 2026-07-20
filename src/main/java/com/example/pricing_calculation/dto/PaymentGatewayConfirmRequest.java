package com.example.pricing_calculation.dto;

public record PaymentGatewayConfirmRequest(
        String referenceCode,
        String status,
        String transactionNo,
        String message
) {
}
