package com.example.pricing_calculation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.pricing_calculation.config.ParkingRuleProperties;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class TimeBandParkingFeeCalculatorTest {

    private final TimeBandParkingFeeCalculator calculator =
            new TimeBandParkingFeeCalculator(new ParkingRuleProperties());

    @Test
    void chargesOneDayTurnForDaytimeParking() {
        var result = calculator.calculate(2,
                LocalDateTime.of(2026, 7, 5, 8, 0),
                LocalDateTime.of(2026, 7, 5, 18, 0));

        assertEquals(new BigDecimal("20000"), result.total());
        assertEquals(1, result.dayTurns());
        assertEquals(0, result.nightHours());
    }

    @Test
    void addsStartedNightHoursAfterTenMinuteGrace() {
        var result = calculator.calculate(4,
                LocalDateTime.of(2026, 7, 5, 20, 0),
                LocalDateTime.of(2026, 7, 6, 0, 20));

        assertEquals(new BigDecimal("65000"), result.total());
        assertEquals(1, result.dayTurns());
        assertEquals(3, result.nightHours());
    }

    @Test
    void ignoresTenMinutesAcrossTariffBoundary() {
        var result = calculator.calculate(2,
                LocalDateTime.of(2026, 7, 5, 21, 0),
                LocalDateTime.of(2026, 7, 5, 22, 10));

        assertEquals(new BigDecimal("20000"), result.total());
        assertEquals(0, result.nightHours());
    }

    @Test
    void stillChargesAShortVisitInsideOneTariffBand() {
        var result = calculator.calculate(2,
                LocalDateTime.of(2026, 7, 5, 12, 0),
                LocalDateTime.of(2026, 7, 5, 12, 5));

        assertEquals(new BigDecimal("20000"), result.total());
        assertEquals(1, result.dayTurns());
    }
}
