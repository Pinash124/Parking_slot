package com.example.pricing_calculation.dto;

import java.math.BigDecimal;
import java.util.List;

public record ParkingFacilityInfoResponse(
        String name,
        String address,
        String operationHours,
        List<String> parkingRules,
        List<VehicleTypeInfo> supportedVehicleTypes,
        List<PricingInfo> pricingPolicies,
        long totalSlots,
        long availableSlots,
        long occupiedSlots,
        long reservedSlots
) {

    public record VehicleTypeInfo(
            Long id,
            String name,
            String description,
            BigDecimal defaultHourlyFee
    ) {
    }

    public record PricingInfo(
            Long id,
            String policyName,
            Long vehicleTypeId,
            String vehicleTypeName,
            BigDecimal hourlyRate,
            BigDecimal dailyRate,
            BigDecimal lostTicketFee,
            BigDecimal overtimeFee,
            String status
    ) {
    }
}
