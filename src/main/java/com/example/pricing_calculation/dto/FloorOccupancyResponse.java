package com.example.pricing_calculation.dto;

public record FloorOccupancyResponse(
        Long floorId,
        String floorName,
        Integer floorNumber,
        long carUsed,
        long carTotal,
        long twoWheelUsed,
        long twoWheelTotal
) {
}
