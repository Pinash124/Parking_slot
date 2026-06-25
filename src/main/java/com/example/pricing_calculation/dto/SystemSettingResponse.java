package com.example.pricing_calculation.dto;

import com.example.pricing_calculation.domain.SystemSetting;
import java.time.LocalDateTime;

public record SystemSettingResponse(
        Long id,
        String key,
        String value,
        String description,
        LocalDateTime updatedAt
) {
    public static SystemSettingResponse from(SystemSetting setting) {
        return new SystemSettingResponse(
                setting.getId(),
                setting.getKey(),
                setting.getValue(),
                setting.getDescription(),
                setting.getUpdatedAt()
        );
    }
}
