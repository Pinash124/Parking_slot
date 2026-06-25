package com.example.pricing_calculation.dto;

import com.example.pricing_calculation.domain.AuditLog;
import java.time.LocalDateTime;

public record AuditLogResponse(
        Long logId,
        Long userId,
        String action,
        String entityName,
        Long entityId,
        LocalDateTime createdAt
) {
    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getUserId(),
                log.getAction(),
                log.getEntityName(),
                log.getEntityId(),
                log.getCreatedAt()
        );
    }
}
