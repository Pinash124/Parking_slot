package com.smartparking.repository;

import com.smartparking.model.schemas.ParkingZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ParkingZoneRepository extends JpaRepository<ParkingZone, Long> {

    @Query(value = """
            select z.*
            from zones z
            join floors f on f.floor_id = z.floor_id
            where f.building_id = :buildingId
            """, nativeQuery = true)
    List<ParkingZone> findByBuildingId(@Param("buildingId") Long buildingId);

    List<ParkingZone> findByVehicleTypeId(Long vehicleTypeId);
}
