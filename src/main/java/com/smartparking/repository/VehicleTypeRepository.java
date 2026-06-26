package com.smartparking.repository;

import com.smartparking.model.schemas.VehicleType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleTypeRepository extends JpaRepository<VehicleType, Long> {

    boolean existsByName(String name);
}