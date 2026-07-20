package com.example.pricing_calculation.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.pricing_calculation.domain.VehicleTypeEntity;
import org.junit.jupiter.api.Test;

class VehicleTypeClassifierTest {

    @Test
    void distinguishesCarsFromMotorbikes() {
        VehicleTypeEntity car = new VehicleTypeEntity();
        car.setName("Car");
        car.setWheelCount(4);
        VehicleTypeEntity motorbike = new VehicleTypeEntity();
        motorbike.setName("Motor");
        motorbike.setWheelCount(2);

        assertTrue(VehicleTypeClassifier.isCar(car));
        assertFalse(VehicleTypeClassifier.isCar(motorbike));
    }
}
