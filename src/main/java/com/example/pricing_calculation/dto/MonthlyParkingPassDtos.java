package com.example.pricing_calculation.dto;

import com.example.pricing_calculation.domain.MonthlyParkingPass;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class MonthlyParkingPassDtos {
    private MonthlyParkingPassDtos() {
    }

    public record MonthlyParkingPassCreateRequest(Long vehicleId, Long slotId, LocalDate startDate, Integer months, String note) { }

    public record MonthlyParkingPassPaymentRequest(String paymentMethod, String referenceCode, Boolean autoRenew) { }

    public record MonthlyParkingPassPaymentPrepareRequest(Boolean autoRenew) { }

    public record MonthlyParkingPassQrConfirmRequest(String qrContent, String paymentMethod, String referenceCode) { }

    public record MonthlyParkingPassPaymentInstructionResponse(
            MonthlyParkingPassResponse pass,
            String paymentMethod,
            String paymentReference,
            BigDecimal amount,
            Boolean autoRenew,
            String qrContent,
            String billContent,
            LocalDateTime createdAt
    ) { }

    public record MonthlyParkingPassResponse(
            Long id,
            Long userId,
            Long vehicleId,
            String licensePlate,
            Long vehicleTypeId,
            String vehicleTypeName,
            Long slotId,
            String slotCode,
            String slotStatus,
            Integer months,
            BigDecimal monthlyRate,
            BigDecimal totalAmount,
            LocalDate startDate,
            LocalDate endDate,
            String status,
            String paymentStatus,
            String paymentMethod,
            String paymentReference,
            Boolean autoRenew,
            LocalDateTime paidAt,
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
                    pass.getReservedSlot() == null ? null : pass.getReservedSlot().getId(),
                    pass.getReservedSlot() == null ? null : pass.getReservedSlot().getSlotCode(),
                    pass.getReservedSlot() == null ? null : pass.getReservedSlot().getStatus(),
                    pass.getMonths(),
                    pass.getMonthlyRate(),
                    pass.getTotalAmount(),
                    pass.getStartDate(),
                    pass.getEndDate(),
                    pass.getStatus(),
                    pass.getPaymentStatus(),
                    pass.getPaymentMethod(),
                    pass.getPaymentReference(),
                    pass.getAutoRenew(),
                    pass.getPaidAt(),
                    pass.getNote(),
                    pass.getCreatedAt(),
                    pass.getUpdatedAt()
            );
        }
    }
}
