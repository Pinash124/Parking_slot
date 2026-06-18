package com.smartparking.model.requests;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "Username or email is required")
    @JsonAlias({"identifier", "username", "email"})
    private String usernameOrEmail;

    @NotBlank(message = "Password is required")
    private String password;
}
