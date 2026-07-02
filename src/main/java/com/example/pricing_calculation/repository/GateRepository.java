package com.example.pricing_calculation.repository;

import com.example.pricing_calculation.domain.Gate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GateRepository extends JpaRepository<Gate, Long> {

    List<Gate> findByBuildingIdOrderByGateNameAsc(Long buildingId);

    Optional<Gate> findByGateNameIgnoreCase(String gateName);
}
