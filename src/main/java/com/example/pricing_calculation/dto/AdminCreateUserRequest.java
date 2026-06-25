package com.example.pricing_calculation.dto;

public record AdminCreateUserRequest(
        String fullName,
        String email,
        String phone,
        String password,
        String role
) {
}
