package com.example.pricing_calculation.repository;

import com.example.pricing_calculation.domain.AuditLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findTop200ByOrderByCreatedAtDesc();
}
