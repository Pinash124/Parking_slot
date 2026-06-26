package com.example.pricing_calculation.dto;

import com.example.pricing_calculation.domain.Building;
import com.example.pricing_calculation.domain.Floor;
import com.example.pricing_calculation.domain.ParkingSlot;
import com.example.pricing_calculation.domain.VehicleTypeEntity;
import com.example.pricing_calculation.domain.Zone;

public record AvailableSlotResponse(
        Long slotId,
        String slotCode,
        String status,
        Long zoneId,
        String zoneName,
        Long vehicleTypeId,
        String vehicleTypeName,
        Long floorId,
        String floorName,
        Long buildingId,
        String buildingName
) {

    public static AvailableSlotResponse from(ParkingSlot slot) {
        Zone zone = slot.getZone();
        VehicleTypeEntity vehicleType = zone == null ? null : zone.getVehicleType();
        Floor floor = zone == null ? null : zone.getFloor();
        Building building = floor == null ? null : floor.getBuilding();
        return new AvailableSlotResponse(
                slot.getId(),
                slot.getSlotCode(),
                slot.getStatus(),
                zone == null ? null : zone.getId(),
                zone == null ? null : zone.getZoneName(),
                vehicleType == null ? null : vehicleType.getId(),
                vehicleType == null ? null : vehicleType.getName(),
                floor == null ? null : floor.getId(),
                floor == null ? null : floor.getFloorName(),
                building == null ? null : building.getId(),
                building == null ? null : building.getName()
        );
    }
}
