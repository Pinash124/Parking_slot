package com.example.pricing_calculation.repository;

import com.example.pricing_calculation.domain.Building;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BuildingRepository extends JpaRepository<Building, Long> {
}
