package com.smartparking.model.requests;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class VehicleTypeReport {

    private Long vehicleTypeId;
    private String vehicleTypeName;
    private long slotCount;
    private long availableSlotCount;
    private BigDecimal revenue;
}
