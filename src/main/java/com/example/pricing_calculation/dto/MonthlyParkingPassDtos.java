package com.example.pricing_calculation.dto;

import com.example.pricing_calculation.domain.MonthlyParkingPass;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class MonthlyParkingPassDtos {
    private MonthlyParkingPassDtos() {
    }

    public record MonthlyParkingPassCreateRequest(Long vehicleId, LocalDate startDate, Integer months, String note) { }

    public record MonthlyParkingPassResponse(
            Long id,
            Long userId,
            Long vehicleId,
            String licensePlate,
            Long vehicleTypeId,
            String vehicleTypeName,
            Integer months,
            BigDecimal monthlyRate,
            BigDecimal totalAmount,
            LocalDate startDate,
            LocalDate endDate,
            String status,
            String note,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        public static MonthlyParkingPassResponse from(MonthlyParkingPass pass) {
            return new MonthlyParkingPassResponse(
                    pass.getId(),
                    pass.getUser() == null ? null : pass.getUser().getId(),
                    pass.getVehicle() == null ? null : pass.getVehicle().getId(),
                    pass.getVehicle() == null ? null : pass.getVehicle().getPlateNumber(),
                    pass.getVehicleType() == null ? null : pass.getVehicleType().getId(),
                    pass.getVehicleType() == null ? null : pass.getVehicleType().getName(),
                    pass.getMonths(),
                    pass.getMonthlyRate(),
                    pass.getTotalAmount(),
                    pass.getStartDate(),
                    pass.getEndDate(),
                    pass.getStatus(),
                    pass.getNote(),
                    pass.getCreatedAt(),
                    pass.getUpdatedAt()
            );
        }
    }
}
