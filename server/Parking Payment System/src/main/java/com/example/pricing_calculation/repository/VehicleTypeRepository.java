package com.example.pricing_calculation.repository;

import com.example.pricing_calculation.domain.VehicleTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleTypeRepository extends JpaRepository<VehicleTypeEntity, Long> {
}
