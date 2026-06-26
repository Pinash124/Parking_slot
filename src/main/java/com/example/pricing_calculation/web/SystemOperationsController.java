package com.example.pricing_calculation.web;

import com.example.pricing_calculation.domain.UserAccount;
import com.example.pricing_calculation.domain.UserRole;
import com.example.pricing_calculation.dto.BackupResponse;
import com.example.pricing_calculation.dto.RecoveryStatusResponse;
import com.example.pricing_calculation.dto.SystemOperationalStatusResponse;
import com.example.pricing_calculation.service.PaymentModuleAuthService;
import com.example.pricing_calculation.service.BackupService;
import com.example.pricing_calculation.service.SystemOperationsService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
@Tag(name = "System Operations", description = "SLA 3 giay, multi-lane, backup va recovery")
public class SystemOperationsController {

    private final SystemOperationsService systemOperationsService;
    private final BackupService backupService;
    private final PaymentModuleAuthService authService;

    public SystemOperationsController(
            SystemOperationsService systemOperationsService,
            BackupService backupService,
            PaymentModuleAuthService authService) {
        this.systemOperationsService = systemOperationsService;
        this.backupService = backupService;
        this.authService = authService;
    }

    @GetMapping("/operations")
    public SystemOperationalStatusResponse operations() {
        return systemOperationsService.status();
    }

    @GetMapping("/recovery")
    public RecoveryStatusResponse recovery() {
        return systemOperationsService.recoveryStatus();
    }

    @GetMapping("/backups/latest")
    public BackupResponse latestBackup() {
        return backupService.lastBackup();
    }

    @PostMapping("/backups")
    @SecurityRequirement(name = "bearerAuth")
    public BackupResponse createBackup(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        UserAccount user = authService.requireAnyRole(
                authorizationHeader,
                UserRole.ADMINISTRATOR,
                UserRole.PARKING_MANAGER
        );
        return backupService.createManualBackup(user);
    }
}
