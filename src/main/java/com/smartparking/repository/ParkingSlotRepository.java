package com.smartparking.repository;

import com.smartparking.model.schemas.ParkingSlot;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ParkingSlotRepository extends JpaRepository<ParkingSlot, Long> {

    Optional<ParkingSlot> findBySlotCode(String slotCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select slot from ParkingSlot slot where slot.slotId = :id")
    Optional<ParkingSlot> findByIdForUpdate(@Param("id") Long id);

    List<ParkingSlot> findByStatus(String status);

    List<ParkingSlot> findByZoneId(Long zoneId);

    @Query(value = """
            select ps.*
            from parking_slots ps
            join zones z on z.zone_id = ps.zone_id
            where z.vehicle_type_id = :vehicleTypeId
            """, nativeQuery = true)
    List<ParkingSlot> findByVehicleTypeId(@Param("vehicleTypeId") Long vehicleTypeId);

    long countByStatus(String status);
}
