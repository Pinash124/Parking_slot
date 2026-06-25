package com.example.pricing_calculation.dto;

import com.example.pricing_calculation.domain.AuditLog;
import java.time.LocalDateTime;

public record AuditLogResponse(
        Long id,
        Long userId,
        String userEmail,
        String action,
        String entityName,
        Long entityId,
        LocalDateTime createdAt
) {

    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getUser() == null ? null : log.getUser().getId(),
                log.getUser() == null ? null : log.getUser().getEmail(),
                log.getAction(),
                log.getEntityName(),
                log.getEntityId(),
                log.getCreatedAt()
        );
    }
}
