package com.smartparking.model.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserStatusUpdateRequest {

    @NotBlank(message = "Status is required")
    private String status;
}
