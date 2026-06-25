package com.example.pricing_calculation.dto;

public record AuthRegistrationResponse(
        Long userId,
        String fullName,
        String email,
        String phone,
        String status,
        String role,
        String message
) {
}
