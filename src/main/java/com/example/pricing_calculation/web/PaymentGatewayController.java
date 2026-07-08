package com.example.pricing_calculation.web;

import com.example.pricing_calculation.dto.PaymentGatewayRequest;
import com.example.pricing_calculation.dto.PaymentGatewayResponse;
import com.example.pricing_calculation.service.BadRequestException;
import com.example.pricing_calculation.service.ResourceNotFoundException;
import com.example.pricing_calculation.service.PaymentGatewayService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import com.example.pricing_calculation.domain.UserAccount;
import com.example.pricing_calculation.domain.UserRole;
import com.example.pricing_calculation.domain.PaymentModuleParkingSession;
import com.example.pricing_calculation.repository.PaymentModuleParkingSessionRepository;
import com.example.pricing_calculation.service.AuditLogService;
import com.example.pricing_calculation.service.PaymentModuleAuthService;
import com.example.pricing_calculation.service.ForbiddenException;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/payment-gateways")
public class PaymentGatewayController {

    private final PaymentGatewayService paymentGatewayService;
    private final PaymentModuleAuthService authService;
    private final PaymentModuleParkingSessionRepository sessions;
    private final AuditLogService auditLogService;

    public PaymentGatewayController(PaymentGatewayService paymentGatewayService, PaymentModuleAuthService authService,
            PaymentModuleParkingSessionRepository sessions,
            AuditLogService auditLogService) {
        this.paymentGatewayService = paymentGatewayService;
        this.authService = authService;
        this.sessions = sessions;
        this.auditLogService = auditLogService;
    }

    private UserAccount authorize(String header, Long sessionId) {
        UserAccount user = authService.authenticate(header);
        if (UserRole.fromCode(user.getRole()) == UserRole.PARKING_USER) {
            PaymentModuleParkingSession session = sessions.findById(sessionId).orElseThrow();
            if (!session.getVehicle().getUser().getId().equals(user.getId())) throw new ForbiddenException("Session does not belong to current user");
        }
        return user;
    }

    @PostMapping("/vnpay")
    @SecurityRequirement(name="bearerAuth")
    public ResponseEntity<PaymentGatewayResponse> createVnpay(
            @RequestHeader("Authorization") String header,
            @RequestBody PaymentGatewayRequest request,
            HttpServletRequest servletRequest) {
        authorize(header, request.sessionId());
        String clientIp = servletRequest.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isBlank()) clientIp = servletRequest.getRemoteAddr();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentGatewayService.createVnpayPayment(request, clientIp));
    }

    @PostMapping("/cash")
    @SecurityRequirement(name="bearerAuth")
    public ResponseEntity<PaymentGatewayResponse> createCash(@RequestHeader("Authorization") String header, @RequestBody PaymentGatewayRequest request) {
        UserAccount actor = authorize(header, request.sessionId());
        PaymentGatewayResponse response = paymentGatewayService.createCashPayment(request);
        auditLogService.record(actor,
                "CASH_PAYMENT_COMPLETED paymentId=" + response.paymentId() + " amount=" + response.amount(),
                "Payment",
                response.paymentId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/personal-qr")
    @SecurityRequirement(name="bearerAuth")
    public ResponseEntity<PaymentGatewayResponse> createPersonalQr(
            @RequestHeader("Authorization") String header,
            @RequestBody PaymentGatewayRequest request) {
        authorize(header, request.sessionId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentGatewayService.createPersonalQrPayment(request));
    }

    @GetMapping("/vnpay/return")
    public PaymentGatewayResponse vnpayReturn(@RequestParam Map<String, String> parameters) {
        return paymentGatewayService.processVnpayCallback(parameters);
    }

    @GetMapping("/vnpay/ipn")
    public Map<String, String> vnpayIpn(@RequestParam Map<String, String> parameters) {
        try {
            paymentGatewayService.processVnpayCallback(parameters);
            return Map.of("RspCode", "00", "Message", "Confirm Success");
        } catch (ResourceNotFoundException exception) {
            return Map.of("RspCode", "01", "Message", "Order not found");
        } catch (BadRequestException exception) {
            String code = exception.getMessage() != null && exception.getMessage().contains("amount") ? "04" : "97";
            return Map.of("RspCode", code, "Message", exception.getMessage());
        }
    }
}

