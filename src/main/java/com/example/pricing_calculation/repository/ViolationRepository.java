package com.example.pricing_calculation.repository;

import com.example.pricing_calculation.domain.PaymentModuleParkingSession;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface ViolationRepository extends Repository<PaymentModuleParkingSession, Long> {
    @Modifying
    @Query(value = "delete from violations where session_id = :sessionId", nativeQuery = true)
    int deleteBySessionId(@Param("sessionId") Long sessionId);
}
