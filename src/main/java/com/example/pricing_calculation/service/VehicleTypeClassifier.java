package com.example.pricing_calculation.service;

import com.example.pricing_calculation.domain.VehicleTypeEntity;
import java.util.Locale;

public final class VehicleTypeClassifier {

    private VehicleTypeClassifier() { }

    public static int wheelCount(VehicleTypeEntity vehicleType) {
        if (vehicleType != null && vehicleType.getWheelCount() != null
                && (vehicleType.getWheelCount() == 2 || vehicleType.getWheelCount() == 4)) {
            return vehicleType.getWheelCount();
        }
        String name = vehicleType == null || vehicleType.getName() == null
                ? "" : vehicleType.getName().toLowerCase(Locale.ROOT);
        if (name.contains("motor") || name.contains("moto") || name.contains("bike")
                || name.contains("xe máy") || name.contains("xe may")
                || name.contains("2 bánh") || name.contains("2 banh")) {
            return 2;
        }
        return 4;
    }

    public static boolean isTwoWheel(VehicleTypeEntity vehicleType) {
        return wheelCount(vehicleType) == 2;
    }

    public static boolean isCar(VehicleTypeEntity vehicleType) {
        return wheelCount(vehicleType) == 4;
    }
}
