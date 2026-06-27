package com.example.pricing_calculation.repository;

import com.example.pricing_calculation.domain.Floor;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FloorRepository extends JpaRepository<Floor, Long> {
    List<Floor> findByBuildingIdOrderByFloorNumberAsc(Long buildingId);
}
