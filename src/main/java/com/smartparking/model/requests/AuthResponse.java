package com.smartparking.model.requests;

import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;

@Getter
@Setter
public class AuthResponse {

    private String message;
    @Nullable
    private String accessToken;
    @Nullable
    private String userId;
    @Nullable
    private String fullName;
    @Nullable
    private String email;
    private String role;

    public AuthResponse(String message,
                        @Nullable String accessToken,
                        @Nullable String userId,
                        @Nullable String fullName,
                        @Nullable String email,
                        String role) {
        this.message = message;
        this.accessToken = accessToken;
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
    }
}
