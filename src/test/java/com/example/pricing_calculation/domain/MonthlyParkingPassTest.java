package com.example.pricing_calculation.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class MonthlyParkingPassTest {

    @Test
    void paidPassCanBeActiveWithoutReservedSlot() {
        MonthlyParkingPass pass = new MonthlyParkingPass();
        pass.setStartDate(LocalDate.of(2026, 7, 1));
        pass.setEndDate(LocalDate.of(2026, 7, 31));
        pass.setStatus("ACTIVE");
        pass.setPaymentStatus("PAID");

        assertEquals(true, pass.isActiveAt(LocalDate.of(2026, 7, 20)));
    }
}
