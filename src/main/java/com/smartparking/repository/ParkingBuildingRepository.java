package com.smartparking.repository;

import com.smartparking.model.schemas.ParkingBuilding;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParkingBuildingRepository extends JpaRepository<ParkingBuilding, Long> {
}
