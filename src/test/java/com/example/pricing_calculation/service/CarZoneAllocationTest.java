package com.example.pricing_calculation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CarZoneAllocationTest {

    @Test
    void givesOneThirdToMonthlyAndTheIntegerRemainderToNormal() {
        assertEquals(10, CarZoneAllocation.monthlySlots(30));
        assertEquals(20, CarZoneAllocation.normalSlots(30));
        assertEquals(10, CarZoneAllocation.monthlySlots(31));
        assertEquals(21, CarZoneAllocation.normalSlots(31));
        assertEquals(10, CarZoneAllocation.monthlySlots(32));
        assertEquals(22, CarZoneAllocation.normalSlots(32));
    }
}
