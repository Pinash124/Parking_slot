package com.smartparking.repository;

import com.smartparking.model.schemas.ParkingSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParkingSlotRepository extends JpaRepository<ParkingSlot, Long> {

    Optional<ParkingSlot> findBySlotCode(String slotCode);

    List<ParkingSlot> findByStatus(String status);

    List<ParkingSlot> findByVehicleTypeId(Long vehicleTypeId);

    long countByStatus(String status);
}
