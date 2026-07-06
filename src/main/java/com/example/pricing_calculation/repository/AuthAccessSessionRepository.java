package com.example.pricing_calculation.repository;

import com.example.pricing_calculation.domain.AuthAccessSession;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthAccessSessionRepository extends JpaRepository<AuthAccessSession, String> {
    long deleteByUserId(Long userId);
    long deleteByExpiresAtBefore(LocalDateTime cutoff);
}
