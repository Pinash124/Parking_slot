package com.example.pricing_calculation.dto;

import java.time.LocalDateTime;

public record SessionCheckInRequest(
        Long reservationId,
        Long vehicleId,
        Long slotId,
        String ticketCode,
        LocalDateTime entryTime,
        String licensePlate
) {
    public SessionCheckInRequest(Long reservationId, Long vehicleId, Long slotId, String ticketCode, LocalDateTime entryTime) {
        this(reservationId, vehicleId, slotId, ticketCode, entryTime, null);
    }
}
