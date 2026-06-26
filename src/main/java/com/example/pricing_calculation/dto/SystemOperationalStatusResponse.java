package com.example.pricing_calculation.dto;

import java.time.LocalDateTime;
import java.util.List;

public record SystemOperationalStatusResponse(
        int responseSlaMs,
        boolean responseTimeHeaderEnabled,
        boolean multiLaneSupported,
        int supportedConcurrentLanes,
        boolean scheduledBackupEnabled,
        boolean connectionRecoveryCheckEnabled,
        boolean auditLogEnabled,
        boolean roleBasedAccessEnabled,
        List<String> laneCodes,
        LocalDateTime checkedAt
) {
}
