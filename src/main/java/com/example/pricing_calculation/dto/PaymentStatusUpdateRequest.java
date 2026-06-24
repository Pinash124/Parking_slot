package com.example.pricing_calculation.dto;

public record PaymentStatusUpdateRequest(
        String status,
        String gateway,
        String referenceCode
) {
}
