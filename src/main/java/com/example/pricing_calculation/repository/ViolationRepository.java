package com.example.pricing_calculation.repository;

import com.example.pricing_calculation.domain.Violation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ViolationRepository extends JpaRepository<Violation, Long> {

    List<Violation> findBySessionIdOrderByCreatedAtDesc(Long sessionId);
}
