package com.example.pricing_calculation.web;

import com.example.pricing_calculation.domain.PaymentMethod;
import com.example.pricing_calculation.domain.TransactionStatus;
import com.example.pricing_calculation.domain.TransactionType;
import com.example.pricing_calculation.dto.PageResponse;
import com.example.pricing_calculation.dto.TransactionHistoryCreateRequest;
import com.example.pricing_calculation.dto.TransactionHistoryResponse;
import com.example.pricing_calculation.dto.TransactionHistorySummaryResponse;
import com.example.pricing_calculation.dto.TransactionHistoryUpdateRequest;
import com.example.pricing_calculation.service.TransactionHistoryService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transaction-history")
public class TransactionHistoryController {

    private final TransactionHistoryService service;

    public TransactionHistoryController(TransactionHistoryService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<TransactionHistoryResponse> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @RequestParam(required = false) String licensePlate,
            @RequestParam(required = false) String reservationCode,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "occurredAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        return service.search(
                keyword,
                type,
                status,
                paymentMethod,
                licensePlate,
                reservationCode,
                from,
                to,
                minAmount,
                maxAmount,
                page,
                size,
                sortBy,
                sortDirection
        );
    }

    @GetMapping("/summary")
    public TransactionHistorySummaryResponse summary(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return service.summary(from, to);
    }

    @GetMapping("/recent")
    public List<TransactionHistoryResponse> recent(@RequestParam(defaultValue = "10") int limit) {
        return service.recent(limit);
    }

    @GetMapping("/code/{transactionCode}")
    public TransactionHistoryResponse getByCode(@PathVariable String transactionCode) {
        return service.getByCode(transactionCode);
    }

    @GetMapping("/{id}")
    public TransactionHistoryResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    public ResponseEntity<TransactionHistoryResponse> create(@RequestBody TransactionHistoryCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    public TransactionHistoryResponse update(
            @PathVariable Long id,
            @RequestBody TransactionHistoryUpdateRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public TransactionHistoryResponse changeStatus(
            @PathVariable Long id,
            @RequestParam TransactionStatus status) {
        return service.changeStatus(id, status);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
