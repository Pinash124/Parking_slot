package com.example.pricing_calculation.dto;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
public record ManagerReportResponse(LocalDateTime from,LocalDateTime to,long entries,long exits,long currentlyParked,
        BigDecimal revenue,long totalSlots,long availableSlots,long occupiedSlots,long reservedSlots,long maintenanceSlots,
        long lockedSlots,double occupancyRate,Map<Integer,Long> peakHours,List<VehicleTypeSummary> byVehicleType){
    public record VehicleTypeSummary(Long vehicleTypeId,String vehicleTypeName,long entries,long exits,long parked,
            long totalSlots,long availableSlots,BigDecimal revenue){}
}
