package com.example.pricing_calculation.web;

import com.example.pricing_calculation.dto.DashboardOverviewResponse;
import com.example.pricing_calculation.dto.RevenueTrendResponse;
import com.example.pricing_calculation.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/overview")
    public DashboardOverviewResponse overview() {
        return dashboardService.overview();
    }

    @GetMapping("/revenue/daily")
    public RevenueTrendResponse dailyRevenue(@RequestParam(defaultValue = "14") int days) {
        return dashboardService.dailyRevenue(days);
    }

    @GetMapping("/revenue/monthly")
    public RevenueTrendResponse monthlyRevenue(@RequestParam(defaultValue = "12") int months) {
        return dashboardService.monthlyRevenue(months);
    }
}
