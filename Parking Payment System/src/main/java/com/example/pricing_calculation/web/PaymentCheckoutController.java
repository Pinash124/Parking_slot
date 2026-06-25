package com.example.pricing_calculation.web;

import com.example.pricing_calculation.dto.PaymentCheckoutPrepareRequest;
import com.example.pricing_calculation.dto.PaymentCheckoutResponse;
import com.example.pricing_calculation.dto.PaymentExitValidationRequest;
import com.example.pricing_calculation.dto.PaymentExitValidationResponse;
import com.example.pricing_calculation.service.PaymentCheckoutService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payment-checkout")
public class PaymentCheckoutController {

    private final PaymentCheckoutService paymentCheckoutService;

    public PaymentCheckoutController(PaymentCheckoutService paymentCheckoutService) {
        this.paymentCheckoutService = paymentCheckoutService;
    }

    @PostMapping("/prepare")
    public PaymentCheckoutResponse prepare(@RequestBody PaymentCheckoutPrepareRequest request) {
        return paymentCheckoutService.prepare(request);
    }

    @GetMapping("/sessions/{sessionId}/status")
    public PaymentCheckoutResponse status(@PathVariable Long sessionId) {
        return paymentCheckoutService.status(sessionId);
    }

    @PostMapping("/validate-exit")
    public PaymentExitValidationResponse validateExit(@RequestBody PaymentExitValidationRequest request) {
        return paymentCheckoutService.validateExit(request);
    }
}
