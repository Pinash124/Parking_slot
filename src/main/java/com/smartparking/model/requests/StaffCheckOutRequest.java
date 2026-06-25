package com.smartparking.model.requests;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class StaffCheckOutRequest {

    @NotNull(message = "Exit staff ID is required")
    private Long exitStaffId;

    @NotNull(message = "Exit gate ID is required")
    private Long exitGateId;

    @DecimalMin(value = "0.0", message = "Parking fee cannot be negative")
    private BigDecimal parkingFee;

    @DecimalMin(value = "0.0", message = "Penalty fee cannot be negative")
    private BigDecimal penaltyFee;

    @DecimalMin(value = "0.0", message = "Total fee cannot be negative")
    private BigDecimal totalFee;
}
