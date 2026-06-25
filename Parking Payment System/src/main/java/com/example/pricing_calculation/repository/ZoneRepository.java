package com.example.pricing_calculation.repository;

import com.example.pricing_calculation.domain.Zone;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ZoneRepository extends JpaRepository<Zone, Long> {
}
