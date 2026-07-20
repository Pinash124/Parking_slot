package com.example.pricing_calculation.repository;

import com.example.pricing_calculation.domain.SessionServiceUsage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionServiceUsageRepository extends JpaRepository<SessionServiceUsage, Long> {
    List<SessionServiceUsage> findBySessionId(Long sessionId);
}
