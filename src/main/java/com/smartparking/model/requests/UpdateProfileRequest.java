package com.smartparking.model.requests;

import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileRequest {

    private String fullName;

    private String username;

    @Email(message = "Email should be valid")
    private String email;

    private String phone;
}
