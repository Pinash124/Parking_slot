package com.example.pricing_calculation.service;

public final class CarZoneAllocation {
    private CarZoneAllocation() { }

    public static int monthlySlots(int totalCarSlots) {
        if (totalCarSlots < 0) throw new IllegalArgumentException("totalCarSlots cannot be negative");
        return totalCarSlots / 3;
    }

    public static int normalSlots(int totalCarSlots) {
        return totalCarSlots - monthlySlots(totalCarSlots);
    }
}
