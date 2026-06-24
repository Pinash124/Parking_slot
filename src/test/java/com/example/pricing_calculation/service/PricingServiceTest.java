package com.example.pricing_calculation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.pricing_calculation.domain.PricingPolicy;
import com.example.pricing_calculation.domain.VehicleTypeEntity;
import com.example.pricing_calculation.dto.PricingQuoteResponse;
import com.example.pricing_calculation.repository.PricingPolicyRepository;
import com.example.pricing_calculation.repository.VehicleTypeRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PricingServiceTest {

    private final PricingPolicyRepository policyRepository = mock(PricingPolicyRepository.class);
    private final VehicleTypeRepository vehicleTypeRepository = mock(VehicleTypeRepository.class);
    private final PricingService service = new PricingService(policyRepository, vehicleTypeRepository);

    @BeforeEach
    void configurePolicy() {
        VehicleTypeEntity vehicleType = mock(VehicleTypeEntity.class);
        when(vehicleType.getId()).thenReturn(1L);
        when(vehicleType.getName()).thenReturn("CAR");
        when(vehicleType.getDefaultHourlyFee()).thenReturn(new BigDecimal("12000.00"));
        PricingPolicy policy = mock(PricingPolicy.class);
        when(policy.getId()).thenReturn(10L);
        when(policy.getPolicyName()).thenReturn("Standard car pricing");
        when(policy.getHourlyRate()).thenReturn(new BigDecimal("15000.00"));
        when(policy.getDailyRate()).thenReturn(new BigDecimal("200000.00"));
        when(policy.getLostTicketFee()).thenReturn(new BigDecimal("50000.00"));
        when(policy.getOvertimeFee()).thenReturn(new BigDecimal("7000.00"));
        when(vehicleTypeRepository.findById(1L)).thenReturn(Optional.of(vehicleType));
        when(policyRepository.findActivePolicies(eq(1L), any(LocalDateTime.class)))
                .thenReturn(List.of(policy));
    }

    @Test
    void calculatesHourlyLostTicketAndOvertimeFees() {
        LocalDateTime entry = LocalDateTime.of(2026, 6, 17, 9, 0);

        PricingQuoteResponse quote = service.estimate(
                1L,
                entry,
                entry.plusHours(3),
                true,
                30
        );

        assertEquals(new BigDecimal("45000.00"), quote.parkingFee());
        assertEquals(new BigDecimal("57000.00"), quote.penaltyFee());
        assertEquals(new BigDecimal("102000.00"), quote.totalFee());
    }

    @Test
    void appliesDailyRateThenRemainingHourlyRate() {
        LocalDateTime entry = LocalDateTime.of(2026, 6, 17, 9, 0);

        PricingQuoteResponse quote = service.estimate(
                1L,
                entry,
                entry.plusHours(25),
                false,
                0
        );

        assertEquals(new BigDecimal("215000.00"), quote.parkingFee());
        assertEquals(new BigDecimal("215000.00"), quote.totalFee());
    }
}
