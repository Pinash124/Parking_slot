package com.smartparking.model.requests;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;

@Getter
@Setter
@AllArgsConstructor
public class PasswordResetResponse {

    private String message;

    @Nullable
    private String resetLink;
}
