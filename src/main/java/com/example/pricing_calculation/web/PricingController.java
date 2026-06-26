package com.example.pricing_calculation.web;

import com.example.pricing_calculation.dto.PricingQuoteResponse;
import com.example.pricing_calculation.service.PricingService;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pricing")
public class PricingController {

    private final PricingService pricingService;

    public PricingController(PricingService pricingService) {
        this.pricingService = pricingService;
    }

    @GetMapping("/estimate")
    public PricingQuoteResponse estimate(
            @RequestParam Long vehicleTypeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime entryTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime exitTime,
            @RequestParam(defaultValue = "false") boolean lostTicket,
            @RequestParam(defaultValue = "0") Integer overtimeMinutes) {
        return pricingService.estimate(vehicleTypeId, entryTime, exitTime, lostTicket, overtimeMinutes);
    }
}
