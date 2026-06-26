package com.example.pricing_calculation.web;

import com.example.pricing_calculation.domain.UserAccount;
import com.example.pricing_calculation.domain.UserRole;
import com.example.pricing_calculation.dto.LicensePlateScanCreateRequest;
import com.example.pricing_calculation.dto.LicensePlateScanResponse;
import com.example.pricing_calculation.service.PaymentModuleAuthService;
import com.example.pricing_calculation.service.LicensePlateScanService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/license-plate-scans")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "License Plate Scans", description = "Ghi nhan scan bien so theo lan xe/lane")
public class LicensePlateScanController {

    private final LicensePlateScanService licensePlateScanService;
    private final PaymentModuleAuthService authService;

    public LicensePlateScanController(
            LicensePlateScanService licensePlateScanService,
            PaymentModuleAuthService authService) {
        this.licensePlateScanService = licensePlateScanService;
        this.authService = authService;
    }

    @PostMapping
    public ResponseEntity<LicensePlateScanResponse> create(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestBody LicensePlateScanCreateRequest request) {
        UserAccount staff = authService.requireAnyRole(
                authorizationHeader,
                UserRole.PARKING_STAFF,
                UserRole.PARKING_MANAGER,
                UserRole.ADMINISTRATOR
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(licensePlateScanService.create(staff, request));
    }

    @GetMapping("/sessions/{sessionId}")
    public List<LicensePlateScanResponse> bySession(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @PathVariable Long sessionId) {
        authService.requireAnyRole(
                authorizationHeader,
                UserRole.PARKING_STAFF,
                UserRole.PARKING_MANAGER,
                UserRole.ADMINISTRATOR
        );
        return licensePlateScanService.bySession(sessionId);
    }
}
