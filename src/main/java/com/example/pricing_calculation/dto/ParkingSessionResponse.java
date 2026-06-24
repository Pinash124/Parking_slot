package com.example.pricing_calculation.dto;

import com.example.pricing_calculation.domain.ParkingSession;
import com.example.pricing_calculation.domain.ParkingSlot;
import com.example.pricing_calculation.domain.Vehicle;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ParkingSessionResponse(
        Long id,
        Long reservationId,
        Long vehicleId,
        String licensePlate,
        Long slotId,
        String slotCode,
        String ticketCode,
        LocalDateTime entryTime,
        LocalDateTime exitTime,
        BigDecimal parkingFee,
        BigDecimal penaltyFee,
        BigDecimal totalFee,
        String status
) {

    public static ParkingSessionResponse from(ParkingSession session) {
        Vehicle vehicle = session.getVehicle();
        ParkingSlot slot = session.getSlot();
        return new ParkingSessionResponse(
                session.getId(),
                session.getReservation() == null ? null : session.getReservation().getId(),
                vehicle == null ? null : vehicle.getId(),
                vehicle == null ? null : vehicle.getPlateNumber(),
                slot == null ? null : slot.getId(),
                slot == null ? null : slot.getSlotCode(),
                session.getTicketCode(),
                session.getEntryTime(),
                session.getExitTime(),
                session.getParkingFee(),
                session.getPenaltyFee(),
                session.getTotalFee(),
                session.getStatus()
        );
    }
}
