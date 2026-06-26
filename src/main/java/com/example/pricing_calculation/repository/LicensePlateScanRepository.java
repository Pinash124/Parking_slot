package com.example.pricing_calculation.repository;

import com.example.pricing_calculation.domain.LicensePlateScan;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LicensePlateScanRepository extends JpaRepository<LicensePlateScan, Long> {

    List<LicensePlateScan> findBySessionIdOrderByScanTimeDesc(Long sessionId);
}
