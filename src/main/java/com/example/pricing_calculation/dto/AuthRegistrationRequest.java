package com.example.pricing_calculation.dto;

public record AuthRegistrationRequest(
        String fullName,
        String username,
        String email,
        String phone,
        String password
) {
}
