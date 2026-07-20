package com.example.pricing_calculation.web;

import com.example.pricing_calculation.dto.ManagerReportResponse;
import com.example.pricing_calculation.service.UnifiedReportService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/manager/reports")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Parking Manager Reports", description = "Luot vao ra, doanh thu, lap day va gio cao diem theo loai xe")
public class ManagerReportController {
    private final UnifiedReportService service;

    public ManagerReportController(UnifiedReportService service) {
        this.service = service;
    }

    @GetMapping
    public ManagerReportResponse report(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.report(from, to);
    }
}
