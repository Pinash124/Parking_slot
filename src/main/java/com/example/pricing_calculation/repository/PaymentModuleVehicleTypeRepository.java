package com.example.pricing_calculation.repository;

import com.example.pricing_calculation.domain.VehicleTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentModuleVehicleTypeRepository extends JpaRepository<VehicleTypeEntity, Long> {
}
