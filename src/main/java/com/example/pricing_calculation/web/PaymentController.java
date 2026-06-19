package com.example.pricing_calculation.web;

import com.example.pricing_calculation.dto.PaymentCreateRequest;
import com.example.pricing_calculation.dto.PaymentResponse;
import com.example.pricing_calculation.dto.PaymentStatusUpdateRequest;
import com.example.pricing_calculation.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/{id}")
    public PaymentResponse getById(@PathVariable Long id) {
        return paymentService.getById(id);
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> create(@RequestBody PaymentCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.create(request));
    }

    @PatchMapping("/{id}/status")
    public PaymentResponse updateStatus(
            @PathVariable Long id,
            @RequestBody PaymentStatusUpdateRequest request) {
        return paymentService.updateStatus(id, request);
    }
}
