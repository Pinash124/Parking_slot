package com.example.pricing_calculation.repository;

import com.example.pricing_calculation.domain.AdditionalService;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdditionalServiceRepository extends JpaRepository<AdditionalService, Long> {
    List<AdditionalService> findByStatusIgnoreCaseOrderByNameAsc(String status);
}
