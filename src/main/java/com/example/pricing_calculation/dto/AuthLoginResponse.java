package com.example.pricing_calculation.dto;

import java.time.LocalDateTime;

public record AuthLoginResponse(
        String accessToken,
        String tokenType,
        LocalDateTime expiresAt,
        Long userId,
        String username,
        String fullName,
        String email,
        String role
) {
}
