package com.smartparking.repository;

import com.smartparking.model.schemas.ParkingZone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParkingZoneRepository extends JpaRepository<ParkingZone, Long> {

    List<ParkingZone> findByFloorId(Long floorId);

    List<ParkingZone> findByVehicleTypeId(Long vehicleTypeId);
}