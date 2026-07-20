package com.example.pricing_calculation.dto;

import com.example.pricing_calculation.domain.UserRole;
import java.util.List;

public record UserRoleResponse(
        String code,
        String displayName,
        String description,
        List<String> capabilities
) {

    public static UserRoleResponse from(UserRole role) {
        return new UserRoleResponse(
                role.code(),
                role.displayName(),
                role.description(),
                List.copyOf(role.capabilities())
        );
    }
}
