package com.example.pricing_calculation.web;

import com.example.pricing_calculation.dto.ParkingSessionResponse;
import com.example.pricing_calculation.dto.SessionCheckInRequest;
import com.example.pricing_calculation.dto.SessionCheckoutRequest;
import com.example.pricing_calculation.service.PaymentModuleParkingSessionService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.example.pricing_calculation.domain.UserAccount;
import com.example.pricing_calculation.domain.UserRole;
import com.example.pricing_calculation.service.PaymentModuleAuthService;
import com.example.pricing_calculation.service.PaymentCheckoutService;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/staff/parking-sessions")
@SecurityRequirement(name="bearerAuth")
@Tag(name="Parking Staff", description="Check-in, tinh phi, thu phi va xac nhan xe ra")
public class PaymentModuleParkingSessionController {

    private final PaymentModuleParkingSessionService parkingSessionService;
    private final PaymentModuleAuthService authService;
    private final PaymentCheckoutService paymentCheckoutService;

    public PaymentModuleParkingSessionController(PaymentModuleParkingSessionService parkingSessionService,
            PaymentModuleAuthService authService, PaymentCheckoutService paymentCheckoutService) {
        this.parkingSessionService = parkingSessionService;
        this.authService = authService;
        this.paymentCheckoutService = paymentCheckoutService;
    }

    private UserAccount staff(String header) {
        return authService.requireAnyRole(header, UserRole.PARKING_STAFF, UserRole.PARKING_MANAGER, UserRole.ADMINISTRATOR);
    }

    @GetMapping("/{id}")
    public ParkingSessionResponse getById(@RequestHeader("Authorization") String header, @PathVariable Long id) {
        staff(header);
        return parkingSessionService.getById(id);
    }

    @PostMapping("/check-in")
    public ResponseEntity<ParkingSessionResponse> checkIn(@RequestHeader("Authorization") String header,
            @RequestParam String entryGateCode, @RequestBody SessionCheckInRequest request) {
        UserAccount staff = staff(header);
        return ResponseEntity.status(HttpStatus.CREATED).body(parkingSessionService.checkIn(request, staff.getId(), entryGateCode));
    }

    @PostMapping("/{id}/checkout")
    public ParkingSessionResponse prepareCheckout(@RequestHeader("Authorization") String header,
            @PathVariable Long id,
            @RequestBody(required = false) SessionCheckoutRequest request) {
        staff(header);
        return parkingSessionService.checkout(id, request);
    }

    @PostMapping("/{id}/complete-exit")
    public ParkingSessionResponse completeExit(@RequestHeader("Authorization") String header,
            @PathVariable Long id, @RequestParam String exitGateCode) {
        UserAccount staff = staff(header);
        return paymentCheckoutService.completeExit(id, staff.getId(), exitGateCode);
    }
}
