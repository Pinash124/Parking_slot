package com.example.pricing_calculation.web;

import com.example.pricing_calculation.domain.UserAccount;
import com.example.pricing_calculation.domain.UserRole;
import com.example.pricing_calculation.dto.ViolationDtos.ViolationRequest;
import com.example.pricing_calculation.dto.ViolationDtos.ViolationResponse;
import com.example.pricing_calculation.service.PaymentModuleAuthService;
import com.example.pricing_calculation.service.ViolationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/staff/violations")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Violations", description = "Quan ly vi pham va muc phat")
public class ViolationController {

    private final ViolationService violationService;
    private final PaymentModuleAuthService authService;

    public ViolationController(ViolationService violationService, PaymentModuleAuthService authService) {
        this.violationService = violationService;
        this.authService = authService;
    }

    private UserAccount staff(String header) {
        return authService.requireAnyRole(header, UserRole.PARKING_STAFF, UserRole.PARKING_MANAGER, UserRole.ADMINISTRATOR);
    }

    @GetMapping
    public List<ViolationResponse> list(@RequestHeader("Authorization") String header,
            @RequestParam(required = false) String status) {
        staff(header);
        return violationService.list(status);
    }

    @GetMapping("/{id}")
    public ViolationResponse getById(@RequestHeader("Authorization") String header, @PathVariable Long id) {
        staff(header);
        return violationService.getById(id);
    }

    @PostMapping
    public ResponseEntity<ViolationResponse> create(@RequestHeader("Authorization") String header,
            @RequestBody ViolationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(violationService.create(staff(header), request));
    }

    @PatchMapping("/{id}/resolve")
    public ViolationResponse resolve(@RequestHeader("Authorization") String header, @PathVariable Long id) {
        return violationService.resolve(staff(header), id);
    }

    @DeleteMapping("/{id}")
    public void delete(@RequestHeader("Authorization") String header, @PathVariable Long id) {
        staff(header);
        violationService.delete(id);
    }
}
