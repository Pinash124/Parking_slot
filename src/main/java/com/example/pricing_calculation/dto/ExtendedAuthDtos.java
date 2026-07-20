package com.example.pricing_calculation.dto;

import java.time.LocalDateTime;

public final class ExtendedAuthDtos {
    private ExtendedAuthDtos() { }
    public record OtpChallengeResponse(String email, String purpose, LocalDateTime expiresAt, String message, String developmentOtp) { }
    public record VerifyOtpRequest(String email, String otp) { }
    public record ForgotPasswordRequest(String email) { }
    public record ResetPasswordRequest(String email, String otp, String newPassword) { }
    public record ChangePasswordRequest(String currentPassword, String newPassword) { }
    public record UpdateProfileRequest(String fullName, String phone) { }
    public record UserProfileResponse(Long id, String fullName, String email, String phone, String status, String role) { }
}
