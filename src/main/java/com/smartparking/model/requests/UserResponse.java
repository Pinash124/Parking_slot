package com.smartparking.model.requests;

import com.smartparking.model.schemas.Role;
import com.smartparking.model.schemas.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class UserResponse {

    private Long userId;
    private String fullName;
    private String username;
    private String email;
    private String phone;
    private String status;
    private String role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UserResponse from(User user) {
        Role role = Role.from(user.getRole());
        return new UserResponse(
                user.getUserId(),
                user.getFullName(),
                user.getUsername(),
                user.getEmail(),
                user.getPhone(),
                user.getStatus(),
                role.name(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
