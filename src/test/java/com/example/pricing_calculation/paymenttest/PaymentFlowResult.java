package com.example.pricing_calculation.paymenttest;

import java.math.BigDecimal;

record PaymentFlowResult(
        String botCode,
        String gateway,
        boolean successful,
        long durationMillis,
        BigDecimal totalFee,
        Long paymentId,
        String referenceCode,
        String error
) {
}
