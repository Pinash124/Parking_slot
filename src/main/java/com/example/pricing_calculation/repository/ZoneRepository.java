package com.example.pricing_calculation.repository;

import com.example.pricing_calculation.domain.Zone;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ZoneRepository extends JpaRepository<Zone, Long> {
    List<Zone> findByFloorId(Long floorId);
    List<Zone> findByVehicleTypeId(Long vehicleTypeId);
}
