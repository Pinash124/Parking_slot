package com.example.pricing_calculation.web;

import com.example.pricing_calculation.domain.UserAccount;
import com.example.pricing_calculation.domain.UserRole;
import com.example.pricing_calculation.dto.GateDtos.GateRequest;
import com.example.pricing_calculation.dto.GateDtos.GateResponse;
import com.example.pricing_calculation.service.GateService;
import com.example.pricing_calculation.service.PaymentModuleAuthService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/manager/gates")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Gates", description = "Quan ly cong vao/ra trong bai xe")
public class GateController {

    private final GateService gateService;
    private final PaymentModuleAuthService authService;

    public GateController(GateService gateService, PaymentModuleAuthService authService) {
        this.gateService = gateService;
        this.authService = authService;
    }

    private UserAccount manager(String header) {
        return authService.requireAnyRole(header, UserRole.PARKING_MANAGER, UserRole.ADMINISTRATOR);
    }

    @GetMapping
    public List<GateResponse> list(@RequestHeader("Authorization") String header,
            @RequestParam(required = false) Long buildingId) {
        manager(header);
        return gateService.list(buildingId);
    }

    @GetMapping("/{id}")
    public GateResponse getById(@RequestHeader("Authorization") String header, @PathVariable Long id) {
        manager(header);
        return gateService.getById(id);
    }

    @PostMapping
    public ResponseEntity<GateResponse> create(@RequestHeader("Authorization") String header, @RequestBody GateRequest request) {
        manager(header);
        return ResponseEntity.status(HttpStatus.CREATED).body(gateService.save(null, request));
    }

    @PutMapping("/{id}")
    public GateResponse update(@RequestHeader("Authorization") String header,
            @PathVariable Long id,
            @RequestBody GateRequest request) {
        manager(header);
        return gateService.save(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@RequestHeader("Authorization") String header, @PathVariable Long id) {
        manager(header);
        gateService.delete(id);
    }
}
