package com.smartparking.model.requests;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyOtpRequest {

    @JsonAlias({"challengeId", "token"})
    private String challengeId;

    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "OTP is required")
    @JsonAlias({"code", "token"})
    private String otp;
}
