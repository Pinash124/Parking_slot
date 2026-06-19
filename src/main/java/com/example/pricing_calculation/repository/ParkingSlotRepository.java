package com.example.pricing_calculation.repository;

import com.example.pricing_calculation.domain.ParkingSlot;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParkingSlotRepository extends JpaRepository<ParkingSlot, Long> {

    long countByStatusIgnoreCase(String status);

    long countByZoneId(Long zoneId);

    long countByZoneIdAndStatusIgnoreCase(Long zoneId, String status);

    List<ParkingSlot> findByZoneIdAndStatusIgnoreCaseOrderBySlotCodeAsc(Long zoneId, String status);
}
