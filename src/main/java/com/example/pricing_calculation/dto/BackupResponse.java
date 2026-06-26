package com.example.pricing_calculation.dto;

import java.time.LocalDateTime;

public record BackupResponse(
        String status,
        String backupFile,
        LocalDateTime createdAt,
        String message
) {
}
