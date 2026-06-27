package com.example.pricing_calculation.dto;

import java.math.BigDecimal;
import java.util.List;

public record CurrentParkingSessionResponse(
        ParkingSessionResponse session,
        PricingQuoteResponse temporaryQuote,
        List<ServiceUsageView> additionalServices,
        BigDecimal additionalServiceTotal,
        BigDecimal estimatedGrandTotal
) {
    public record ServiceUsageView(Long usageId, Long serviceId, String serviceName, Integer quantity,
                                   BigDecimal unitPrice, BigDecimal lineTotal) { }
}
