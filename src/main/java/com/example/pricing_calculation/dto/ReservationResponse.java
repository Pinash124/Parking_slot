package com.example.pricing_calculation.dto;

import com.example.pricing_calculation.domain.Reservation;
import com.example.pricing_calculation.domain.Vehicle;
import com.example.pricing_calculation.domain.Zone;
import java.time.LocalDateTime;

public record ReservationResponse(
        Long id,
        Long userId,
        String customerName,
        Long vehicleId,
        String licensePlate,
        Long zoneId,
        String zoneName,
        Long reservedSlotId,
        String reservedSlotCode,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String status
) {

    public static ReservationResponse from(Reservation reservation) {
        Vehicle vehicle = reservation.getVehicle();
        Zone zone = reservation.getZone();
        return new ReservationResponse(
                reservation.getId(),
                reservation.getUser() == null ? null : reservation.getUser().getId(),
                reservation.getUser() == null ? null : reservation.getUser().getFullName(),
                vehicle == null ? null : vehicle.getId(),
                vehicle == null ? null : vehicle.getPlateNumber(),
                zone == null ? null : zone.getId(),
                zone == null ? null : zone.getZoneName(),
                reservation.getReservedSlot() == null ? null : reservation.getReservedSlot().getId(),
                reservation.getReservedSlot() == null ? null : reservation.getReservedSlot().getSlotCode(),
                reservation.getStartTime(),
                reservation.getEndTime(),
                reservation.getStatus()
        );
    }
}
