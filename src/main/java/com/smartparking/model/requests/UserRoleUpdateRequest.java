package com.smartparking.model.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRoleUpdateRequest {

    @NotBlank(message = "Role is required")
    private String role;
}
