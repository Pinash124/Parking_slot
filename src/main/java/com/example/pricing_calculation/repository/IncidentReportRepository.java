package com.example.pricing_calculation.repository;

import com.example.pricing_calculation.domain.IncidentReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentReportRepository extends JpaRepository<IncidentReport, Long> {
}
