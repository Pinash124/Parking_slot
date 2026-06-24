package com.example.pricing_calculation.paymenttest;

record PaymentBotSeed(
        String code,
        String email,
        Long userId,
        Long vehicleId,
        Long vehicleTypeId,
        Long zoneId,
        Long slotId
) {
}
