package com.example.pricing_calculation.dto;

import com.example.pricing_calculation.domain.PaymentModuleParkingSession;
import com.example.pricing_calculation.domain.PaymentModuleParkingSlot;
import com.example.pricing_calculation.domain.Vehicle;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ParkingSessionResponse(
        Long id,
        Long reservationId,
        Long vehicleId,
        String licensePlate,
        Long vehicleTypeId,
        String vehicleTypeName,
        Long slotId,
        String slotCode,
        Long zoneId,
        String zoneName,
        String floorName,
        String buildingName,
        String ticketCode,
        String entryGateCode,
        String exitGateCode,
        Long entryStaffId,
        Long exitStaffId,
        LocalDateTime entryTime,
        LocalDateTime exitTime,
        BigDecimal parkingFee,
        BigDecimal penaltyFee,
        BigDecimal totalFee,
        String status
) {

    public static ParkingSessionResponse from(PaymentModuleParkingSession session) {
        Vehicle vehicle = session.getVehicle();
        PaymentModuleParkingSlot slot = session.getSlot();
        return new ParkingSessionResponse(
                session.getId(),
                session.getReservation() == null ? null : session.getReservation().getId(),
                vehicle == null ? null : vehicle.getId(),
                vehicle == null ? null : vehicle.getPlateNumber(),
                vehicle == null || vehicle.getVehicleType() == null ? null : vehicle.getVehicleType().getId(),
                vehicle == null || vehicle.getVehicleType() == null ? null : vehicle.getVehicleType().getName(),
                slot == null ? null : slot.getId(),
                slot == null ? null : slot.getSlotCode(),
                slot == null || slot.getZone() == null ? null : slot.getZone().getId(),
                slot == null || slot.getZone() == null ? null : slot.getZone().getZoneName(),
                slot == null || slot.getZone() == null || slot.getZone().getFloor() == null ? null : slot.getZone().getFloor().getFloorName(),
                slot == null || slot.getZone() == null || slot.getZone().getFloor() == null || slot.getZone().getFloor().getBuilding() == null ? null : slot.getZone().getFloor().getBuilding().getName(),
                session.getTicketCode(),
                session.getEntryGateCode(),
                session.getExitGateCode(),
                session.getEntryStaffId(),
                session.getExitStaffId(),
                session.getEntryTime(),
                session.getExitTime(),
                session.getParkingFee(),
                session.getPenaltyFee(),
                session.getTotalFee(),
                session.getStatus()
        );
    }
}
