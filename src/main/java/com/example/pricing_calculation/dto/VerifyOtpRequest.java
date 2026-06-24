package com.example.pricing_calculation.dto;

public record VerifyOtpRequest(
        String email,
        String otp
) {
}
