package com.example.pricing_calculation.web;

import com.example.pricing_calculation.domain.UserRole;
import com.example.pricing_calculation.dto.AuditLogResponse;
import com.example.pricing_calculation.service.AuditLogService;
import com.example.pricing_calculation.service.PaymentModuleAuthService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit-logs")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Audit Logs", description = "Ghi va tra cuu audit log thao tac he thong")
public class AuditLogController {

    private final AuditLogService auditLogService;
    private final PaymentModuleAuthService authService;

    public AuditLogController(AuditLogService auditLogService, PaymentModuleAuthService authService) {
        this.auditLogService = auditLogService;
        this.authService = authService;
    }

    @GetMapping("/recent")
    public List<AuditLogResponse> recent(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestParam(defaultValue = "50") int limit) {
        authService.requireAnyRole(
                authorizationHeader,
                UserRole.ADMINISTRATOR,
                UserRole.PARKING_MANAGER
        );
        return auditLogService.recent(limit);
    }
}
