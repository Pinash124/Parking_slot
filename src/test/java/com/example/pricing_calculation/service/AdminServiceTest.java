package com.example.pricing_calculation.service;

import com.example.pricing_calculation.domain.AuditLog;
import com.example.pricing_calculation.domain.UserAccount;
import com.example.pricing_calculation.dto.AdminCreateUserRequest;
import com.example.pricing_calculation.dto.AdminRoleUpdateRequest;
import com.example.pricing_calculation.dto.AdminStatusUpdateRequest;
import com.example.pricing_calculation.dto.AdminUserResponse;
import com.example.pricing_calculation.repository.AuditLogRepository;
import com.example.pricing_calculation.repository.SystemSettingRepository;
import com.example.pricing_calculation.repository.UserAccountRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminServiceTest {

    private UserAccountRepository userRepository;
    private PasswordHashService passwordHashService;
    private AuthService authService;
    private AuditLogRepository auditLogRepository;
    private AdminService adminService;
    private UserAccount admin;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserAccountRepository.class);
        passwordHashService = new PasswordHashService();
        authService = mock(AuthService.class);
        auditLogRepository = mock(AuditLogRepository.class);
        adminService = new AdminService(
                userRepository,
                passwordHashService,
                authService,
                mock(SystemSettingRepository.class),
                auditLogRepository
        );

        admin = user(1L, "admin@example.com", "ADMIN", "ACTIVE");
        when(authService.requireRole("Bearer admin-token", "ADMIN")).thenReturn(admin);
    }

    @Test
    void adminCreatesStaffWithHashedPasswordAndAuditLog() {
        when(userRepository.existsByEmailIgnoreCase("staff@example.com")).thenReturn(false);
        when(userRepository.save(any(UserAccount.class))).thenAnswer(invocation -> {
            UserAccount user = invocation.getArgument(0);
            user.setId(2L);
            return user;
        });

        AdminUserResponse response = adminService.createUser(
                "Bearer admin-token",
                new AdminCreateUserRequest(
                        "Parking Staff",
                        "STAFF@example.com",
                        "0901234567",
                        "Safe-password-123!",
                        "STAFF"
                )
        );

        assertEquals("staff@example.com", response.email());
        assertEquals("STAFF", response.role());
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void adminCannotRemoveOwnAdminRole() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        BadRequestException error = assertThrows(
                BadRequestException.class,
                () -> adminService.updateRole(
                        "Bearer admin-token",
                        1L,
                        new AdminRoleUpdateRequest("MANAGER")
                )
        );

        assertEquals("Admin cannot remove their own ADMIN role", error.getMessage());
        verify(userRepository, never()).save(any(UserAccount.class));
    }

    @Test
    void lastActiveAdminCannotBeLocked() {
        UserAccount otherAdmin = user(2L, "second-admin@example.com", "ADMIN", "ACTIVE");
        when(userRepository.findById(2L)).thenReturn(Optional.of(otherAdmin));
        when(userRepository.countByRoleIgnoreCaseAndStatusIgnoreCase("ADMIN", "ACTIVE")).thenReturn(1L);

        BadRequestException error = assertThrows(
                BadRequestException.class,
                () -> adminService.updateStatus(
                        "Bearer admin-token",
                        2L,
                        new AdminStatusUpdateRequest("LOCKED")
                )
        );

        assertEquals("The last active ADMIN account cannot be removed or locked", error.getMessage());
        verify(userRepository, never()).save(any(UserAccount.class));
    }

    @Test
    void lockingUserRevokesTheirSessions() {
        UserAccount staff = user(3L, "staff@example.com", "STAFF", "ACTIVE");
        when(userRepository.findById(3L)).thenReturn(Optional.of(staff));
        when(userRepository.save(staff)).thenReturn(staff);

        AdminUserResponse response = adminService.updateStatus(
                "Bearer admin-token",
                3L,
                new AdminStatusUpdateRequest("LOCKED")
        );

        assertEquals("LOCKED", response.status());
        verify(authService).revokeSessions(3L);
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    private UserAccount user(Long id, String email, String role, String status) {
        UserAccount user = new UserAccount();
        user.setId(id);
        user.setFullName("Test User");
        user.setEmail(email);
        user.setPasswordHash(passwordHashService.hash("Safe-password-123!"));
        user.setRole(role);
        user.setStatus(status);
        return user;
    }
}
