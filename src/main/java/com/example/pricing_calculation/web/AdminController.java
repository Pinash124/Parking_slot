package com.example.pricing_calculation.web;

import com.example.pricing_calculation.dto.AuditLogResponse;
import com.example.pricing_calculation.dto.AdminCreateUserRequest;
import com.example.pricing_calculation.dto.AdminResetPasswordRequest;
import com.example.pricing_calculation.dto.AdminRoleUpdateRequest;
import com.example.pricing_calculation.dto.AdminStatusUpdateRequest;
import com.example.pricing_calculation.dto.AdminUserResponse;
import com.example.pricing_calculation.dto.SystemSettingRequest;
import com.example.pricing_calculation.dto.SystemSettingResponse;
import com.example.pricing_calculation.service.AdminService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public List<AdminUserResponse> getUsers(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        return adminService.getUsers(authorizationHeader);
    }

    @PostMapping("/users")
    public ResponseEntity<AdminUserResponse> createUser(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestBody AdminCreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminService.createUser(authorizationHeader, request));
    }

    @PatchMapping("/users/{userId}/role")
    public AdminUserResponse updateRole(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @PathVariable Long userId,
            @RequestBody AdminRoleUpdateRequest request) {
        return adminService.updateRole(authorizationHeader, userId, request);
    }

    @PatchMapping("/users/{userId}/status")
    public AdminUserResponse updateStatus(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @PathVariable Long userId,
            @RequestBody AdminStatusUpdateRequest request) {
        return adminService.updateStatus(authorizationHeader, userId, request);
    }

    @PostMapping("/users/{userId}/reset-password")
    public AdminUserResponse resetPassword(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @PathVariable Long userId,
            @RequestBody AdminResetPasswordRequest request) {
        return adminService.resetPassword(authorizationHeader, userId, request);
    }

    @GetMapping("/system-configs")
    public List<SystemSettingResponse> getSystemSettings(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        return adminService.getSettings(authorizationHeader);
    }

    @PutMapping("/system-configs/{key}")
    public SystemSettingResponse saveSystemSetting(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @PathVariable String key,
            @RequestBody SystemSettingRequest request) {
        return adminService.saveSetting(authorizationHeader, key, request);
    }

    @GetMapping("/audit-logs")
    public List<AuditLogResponse> getAuditLogs(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        return adminService.getAuditLogs(authorizationHeader);
    }
}
