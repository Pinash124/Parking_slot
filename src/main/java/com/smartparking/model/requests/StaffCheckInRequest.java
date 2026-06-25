package com.smartparking.model.requests;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StaffCheckInRequest {

    private Long reservationId;

    @NotNull(message = "Vehicle ID is required")
    private Long vehicleId;

    @NotNull(message = "Slot ID is required")
    private Long slotId;

    @NotNull(message = "Entry staff ID is required")
    private Long entryStaffId;

    @NotNull(message = "Entry gate ID is required")
    private Long entryGateId;

    private String ticketCode;
}
