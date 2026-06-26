package com.smartparking.model.requests;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Getter
@AllArgsConstructor
public class ManagerOverviewReport {

    private LocalDateTime from;
    private LocalDateTime to;
    private long totalCheckIns;
    private long totalCheckOuts;
    private BigDecimal revenue;
    private long totalSlots;
    private long availableSlots;
    private long occupiedSlots;
    private long reservedSlots;
    private long maintenanceSlots;
    private long lockedSlots;
    private double occupancyRate;
    private Map<Integer, Long> peakHours;
}
