package com.example.pricing_calculation.service;

import com.example.pricing_calculation.domain.AuditLog;
import com.example.pricing_calculation.domain.SystemSetting;
import com.example.pricing_calculation.domain.UserAccount;
import com.example.pricing_calculation.dto.AuditLogResponse;
import com.example.pricing_calculation.dto.AdminCreateUserRequest;
import com.example.pricing_calculation.dto.AdminResetPasswordRequest;
import com.example.pricing_calculation.dto.AdminRoleUpdateRequest;
import com.example.pricing_calculation.dto.AdminStatusUpdateRequest;
import com.example.pricing_calculation.dto.AdminUserResponse;
import com.example.pricing_calculation.dto.SystemSettingRequest;
import com.example.pricing_calculation.dto.SystemSettingResponse;
import com.example.pricing_calculation.repository.AuditLogRepository;
import com.example.pricing_calculation.repository.SystemSettingRepository;
import com.example.pricing_calculation.repository.UserAccountRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {

    private static final Set<String> ROLES = Set.of("ADMIN", "MANAGER", "STAFF", "CUSTOMER");
    private static final Set<String> STATUSES = Set.of("ACTIVE", "LOCKED");

    private final UserAccountRepository userAccountRepository;
    private final PasswordHashService passwordHashService;
    private final AuthService authService;
    private final SystemSettingRepository systemSettingRepository;
    private final AuditLogRepository auditLogRepository;

    public AdminService(UserAccountRepository userAccountRepository,
                        PasswordHashService passwordHashService,
                        AuthService authService,
                        SystemSettingRepository systemSettingRepository,
                        AuditLogRepository auditLogRepository) {
        this.userAccountRepository = userAccountRepository;
        this.passwordHashService = passwordHashService;
        this.authService = authService;
        this.systemSettingRepository = systemSettingRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> getUsers(String authorizationHeader) {
        UserAccount actor = authService.requireRole(authorizationHeader, "ADMIN");
        return userAccountRepository.findAllByOrderByIdAsc().stream()
                .map(AdminUserResponse::from)
                .toList();
    }

    @Transactional
    public AdminUserResponse createUser(String authorizationHeader, AdminCreateUserRequest request) {
        UserAccount actor = authService.requireRole(authorizationHeader, "ADMIN");
        if (request == null) {
            throw new BadRequestException("User information is required");
        }

        String fullName = requireText(request.fullName(), "fullName is required");
        if (fullName.length() < 2 || fullName.length() > 100) {
            throw new BadRequestException("fullName must contain between 2 and 100 characters");
        }
        String email = normalizeEmail(request.email());
        if (email.isBlank() || email.length() > 100 || !email.contains("@")) {
            throw new BadRequestException("A valid email is required");
        }
        if (userAccountRepository.existsByEmailIgnoreCase(email)) {
            throw new BadRequestException("Email is already registered");
        }
        authService.validatePasswordForAdministration(request.password());

        UserAccount user = new UserAccount();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhone(blankToNull(request.phone()));
        user.setPasswordHash(passwordHashService.hash(request.password()));
        user.setStatus("ACTIVE");
        user.setRole(normalizeRole(request.role()));
        UserAccount saved = userAccountRepository.save(user);
        audit(actor, "CREATE_USER role=" + saved.getRole(), "UserAccount", saved.getId());
        return AdminUserResponse.from(saved);
    }

    @Transactional
    public AdminUserResponse updateRole(String authorizationHeader, Long userId, AdminRoleUpdateRequest request) {
        UserAccount actor = authService.requireRole(authorizationHeader, "ADMIN");
        UserAccount target = findUser(userId);
        String newRole = normalizeRole(request == null ? null : request.role());

        if (actor.getId().equals(target.getId()) && !"ADMIN".equals(newRole)) {
            throw new BadRequestException("Admin cannot remove their own ADMIN role");
        }
        ensureNotRemovingLastActiveAdmin(target, newRole, target.getStatus());

        target.setRole(newRole);
        UserAccount saved = userAccountRepository.save(target);
        authService.revokeSessions(target.getId());
        audit(actor, "UPDATE_ROLE role=" + newRole, "UserAccount", target.getId());
        return AdminUserResponse.from(saved);
    }

    @Transactional
    public AdminUserResponse updateStatus(
            String authorizationHeader,
            Long userId,
            AdminStatusUpdateRequest request) {
        UserAccount actor = authService.requireRole(authorizationHeader, "ADMIN");
        UserAccount target = findUser(userId);
        String newStatus = normalizeStatus(request == null ? null : request.status());

        if (actor.getId().equals(target.getId()) && !"ACTIVE".equals(newStatus)) {
            throw new BadRequestException("Admin cannot lock their own account");
        }
        ensureNotRemovingLastActiveAdmin(target, target.getRole(), newStatus);

        target.setStatus(newStatus);
        UserAccount saved = userAccountRepository.save(target);
        if (!"ACTIVE".equals(newStatus)) {
            authService.revokeSessions(target.getId());
        }
        audit(actor, "UPDATE_STATUS status=" + newStatus, "UserAccount", target.getId());
        return AdminUserResponse.from(saved);
    }

    @Transactional
    public AdminUserResponse resetPassword(
            String authorizationHeader,
            Long userId,
            AdminResetPasswordRequest request) {
        UserAccount actor = authService.requireRole(authorizationHeader, "ADMIN");
        if (request == null) {
            throw new BadRequestException("New password is required");
        }
        authService.validatePasswordForAdministration(request.newPassword());

        UserAccount target = findUser(userId);
        target.setPasswordHash(passwordHashService.hash(request.newPassword()));
        UserAccount saved = userAccountRepository.save(target);
        authService.revokeSessions(target.getId());
        audit(actor, "RESET_PASSWORD", "UserAccount", target.getId());
        return AdminUserResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<SystemSettingResponse> getSettings(String authorizationHeader) {
        authService.requireRole(authorizationHeader, "ADMIN");
        return systemSettingRepository.findAllByOrderByKeyAsc().stream()
                .map(SystemSettingResponse::from)
                .toList();
    }

    @Transactional
    public SystemSettingResponse saveSetting(
            String authorizationHeader,
            String key,
            SystemSettingRequest request) {
        UserAccount actor = authService.requireRole(authorizationHeader, "ADMIN");
        String normalizedKey = requireText(key, "config key is required").toUpperCase(Locale.ROOT);
        if (normalizedKey.length() > 150) {
            throw new BadRequestException("config key must not exceed 150 characters");
        }
        if (request == null) {
            throw new BadRequestException("Configuration information is required");
        }
        if (request.value() != null && request.value().length() > 2000) {
            throw new BadRequestException("config value must not exceed 2000 characters");
        }

        SystemSetting setting = systemSettingRepository.findByKey(normalizedKey)
                .orElseGet(SystemSetting::new);
        setting.setKey(normalizedKey);
        setting.setValue(request.value());
        setting.setDescription(request.description());
        setting.setUpdatedAt(LocalDateTime.now());
        SystemSetting saved = systemSettingRepository.save(setting);
        audit(actor, "SAVE_SYSTEM_CONFIG key=" + normalizedKey, "SystemSetting", saved.getId());
        return SystemSettingResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAuditLogs(String authorizationHeader) {
        authService.requireRole(authorizationHeader, "ADMIN");
        return auditLogRepository.findTop200ByOrderByCreatedAtDesc().stream()
                .map(AuditLogResponse::from)
                .toList();
    }

    private UserAccount findUser(Long userId) {
        return userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    private void audit(UserAccount actor, String action, String entityName, Long entityId) {
        AuditLog log = new AuditLog();
        log.setUserId(actor.getId());
        log.setAction(action);
        log.setEntityName(entityName);
        log.setEntityId(entityId);
        log.setCreatedAt(LocalDateTime.now());
        auditLogRepository.save(log);
    }

    private void ensureNotRemovingLastActiveAdmin(UserAccount target, String newRole, String newStatus) {
        boolean targetIsActiveAdmin = "ADMIN".equalsIgnoreCase(target.getRole())
                && "ACTIVE".equalsIgnoreCase(target.getStatus());
        boolean remainsActiveAdmin = "ADMIN".equalsIgnoreCase(newRole)
                && "ACTIVE".equalsIgnoreCase(newStatus);
        if (targetIsActiveAdmin && !remainsActiveAdmin
                && userAccountRepository.countByRoleIgnoreCaseAndStatusIgnoreCase("ADMIN", "ACTIVE") <= 1) {
            throw new BadRequestException("The last active ADMIN account cannot be removed or locked");
        }
    }

    private String normalizeRole(String role) {
        String normalized = requireText(role, "role is required")
                .toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
        if ("PARKING_MANAGER".equals(normalized)) {
            normalized = "MANAGER";
        }
        if (!ROLES.contains(normalized)) {
            throw new BadRequestException("role must be ADMIN, MANAGER, STAFF, or CUSTOMER");
        }
        return normalized;
    }

    private String normalizeStatus(String status) {
        String normalized = requireText(status, "status is required").toUpperCase(Locale.ROOT);
        if (!STATUSES.contains(normalized)) {
            throw new BadRequestException("status must be ACTIVE or LOCKED");
        }
        return normalized;
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }
        return value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
