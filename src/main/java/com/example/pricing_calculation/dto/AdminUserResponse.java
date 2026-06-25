package com.example.pricing_calculation.dto;

import com.example.pricing_calculation.domain.UserAccount;

public record AdminUserResponse(
        Long userId,
        String fullName,
        String email,
        String phone,
        String status,
        String role
) {
    public static AdminUserResponse from(UserAccount user) {
        return new AdminUserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getStatus(),
                user.getRole()
        );
    }
}
