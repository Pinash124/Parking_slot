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

@RestController
@RequestMapping("/api/payment-gateways")
public class PaymentGatewayController {

    private final PaymentGatewayService paymentGatewayService;

    public PaymentGatewayController(PaymentGatewayService paymentGatewayService) {
        this.paymentGatewayService = paymentGatewayService;
    }

    @PostMapping("/momo")
    public ResponseEntity<PaymentGatewayResponse> createMomo(@RequestBody PaymentGatewayRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentGatewayService.createMomoPayment(request));
    }

    @PostMapping("/vnpay")
    public ResponseEntity<PaymentGatewayResponse> createVnpay(@RequestBody PaymentGatewayRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentGatewayService.createVnpayPayment(request));
    }

    @PostMapping("/cash")
    public ResponseEntity<PaymentGatewayResponse> createCash(@RequestBody PaymentGatewayRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentGatewayService.createCashPayment(request));
    }

    @PostMapping("/{gateway}/confirm")
    public PaymentGatewayResponse confirm(
            @PathVariable String gateway,
            @RequestBody PaymentGatewayConfirmRequest request) {
        return paymentGatewayService.confirmOnlinePayment(gateway, request);
    }
}
