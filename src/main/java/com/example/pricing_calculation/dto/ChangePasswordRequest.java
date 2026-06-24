package com.example.pricing_calculation.dto;

public record ChangePasswordRequest(
        String oldPassword,
        String newPassword
) {
}
