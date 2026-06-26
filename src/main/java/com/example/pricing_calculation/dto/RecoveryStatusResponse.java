package com.example.pricing_calculation.dto;

import java.time.LocalDateTime;

public record RecoveryStatusResponse(
        String databaseStatus,
        boolean connectionAvailable,
        boolean autoReconnectEnabled,
        String lastBackupFile,
        LocalDateTime checkedAt,
        String message
) {
}
