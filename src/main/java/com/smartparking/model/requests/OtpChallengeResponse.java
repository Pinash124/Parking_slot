package com.smartparking.model.requests;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class OtpChallengeResponse {

    private String message;

    @Nullable
    private String challengeId;

    @Nullable
    private String email;

    private LocalDateTime expiresAt;

    @Nullable
    private String otp;
}
