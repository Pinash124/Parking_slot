package com.smartparking.model.requests;

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

    private BigDecimal parkingFee;

    private BigDecimal penaltyFee;

    private BigDecimal totalFee;
}
