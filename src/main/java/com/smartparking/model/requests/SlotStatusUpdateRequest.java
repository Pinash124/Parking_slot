package com.smartparking.model.requests;

import com.smartparking.model.enums.ParkingSlotStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SlotStatusUpdateRequest {

    @NotNull(message = "Slot status is required")
    private ParkingSlotStatus status;

    private String note;
}
