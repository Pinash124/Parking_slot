package com.example.pricing_calculation.web;

import com.example.pricing_calculation.dto.PaymentGatewayConfirmRequest;
import com.example.pricing_calculation.dto.PaymentGatewayRequest;
import com.example.pricing_calculation.dto.PaymentGatewayResponse;
import com.example.pricing_calculation.service.PaymentGatewayService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;
import com.example.pricing_calculation.domain.UserAccount;
import com.example.pricing_calculation.domain.UserRole;
import com.example.pricing_calculation.domain.PaymentModuleParkingSession;
import com.example.pricing_calculation.repository.PaymentModuleParkingSessionRepository;
import com.example.pricing_calculation.service.PaymentModuleAuthService;
import com.example.pricing_calculation.service.ForbiddenException;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/payment-gateways")
public class PaymentGatewayController {

    private final PaymentGatewayService paymentGatewayService;
    private final PaymentModuleAuthService authService;
    private final PaymentModuleParkingSessionRepository sessions;

    public PaymentGatewayController(PaymentGatewayService paymentGatewayService, PaymentModuleAuthService authService,
            PaymentModuleParkingSessionRepository sessions) {
        this.paymentGatewayService = paymentGatewayService;
        this.authService = authService;
        this.sessions = sessions;
    }

    private void authorize(String header, Long sessionId) {
        UserAccount user = authService.authenticate(header);
        if (UserRole.fromCode(user.getRole()) == UserRole.PARKING_USER) {
            PaymentModuleParkingSession session = sessions.findById(sessionId).orElseThrow();
            if (!session.getVehicle().getUser().getId().equals(user.getId())) throw new ForbiddenException("Session does not belong to current user");
        }
    }

    @PostMapping("/momo")
    @SecurityRequirement(name="bearerAuth")
    public ResponseEntity<PaymentGatewayResponse> createMomo(@RequestHeader("Authorization") String header, @RequestBody PaymentGatewayRequest request) {
        authorize(header, request.sessionId());
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentGatewayService.createMomoPayment(request));
    }

    @PostMapping("/vnpay")
    @SecurityRequirement(name="bearerAuth")
    public ResponseEntity<PaymentGatewayResponse> createVnpay(@RequestHeader("Authorization") String header, @RequestBody PaymentGatewayRequest request) {
        authorize(header, request.sessionId());
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentGatewayService.createVnpayPayment(request));
    }

    @PostMapping("/cash")
    @SecurityRequirement(name="bearerAuth")
    public ResponseEntity<PaymentGatewayResponse> createCash(@RequestHeader("Authorization") String header, @RequestBody PaymentGatewayRequest request) {
        authorize(header, request.sessionId());
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentGatewayService.createCashPayment(request));
    }

    @PostMapping("/{gateway}/confirm")
    public PaymentGatewayResponse confirm(
            @PathVariable String gateway,
            @RequestBody PaymentGatewayConfirmRequest request) {
        return paymentGatewayService.confirmOnlinePayment(gateway, request);
    }
}
