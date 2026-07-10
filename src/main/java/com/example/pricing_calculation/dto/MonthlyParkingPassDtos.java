package com.example.pricing_calculation.dto;

import com.example.pricing_calculation.domain.MonthlyParkingPass;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public final class MonthlyParkingPassDtos {
    private MonthlyParkingPassDtos() {
    }

    public record MonthlyParkingPassCreateRequest(Long vehicleId, Long slotId, LocalDate startDate, Integer months, String note) { }

    public record MonthlyParkingPassPaymentRequest(String referenceCode) { }

    public record MonthlyParkingPassQrConfirmRequest(String qrContent, String referenceCode) { }

    public record MonthlyParkingPassPaymentInstructionResponse(
            MonthlyParkingPassResponse pass,
            String paymentMethod,
            String paymentReference,
            BigDecimal amount,
            String qrContent,
            String billContent,
            String qrImageUrl,
            String transferContent,
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
            LocalDateTime paidAt,
            Long daysUntilExpiry,
            Boolean expiryReminderDue,
            String expiryReminderMessage,
            String note,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        public static MonthlyParkingPassResponse from(MonthlyParkingPass pass) {
            LocalDate today = LocalDate.now();
            Long daysUntilExpiry = pass.getEndDate() == null ? null : ChronoUnit.DAYS.between(today, pass.getEndDate());
            boolean paid = "PAID".equalsIgnoreCase(pass.getPaymentStatus());
            boolean activeOrScheduled = pass.getStatus() != null
                    && (pass.getStatus().equalsIgnoreCase("ACTIVE") || pass.getStatus().equalsIgnoreCase("SCHEDULED"));
            boolean reminderDue = paid && activeOrScheduled && daysUntilExpiry != null
                    && daysUntilExpiry >= 0 && daysUntilExpiry <= 3;
            String reminderMessage = reminderDue
                    ? "Ve thang cua xe " + (pass.getVehicle() == null ? "N/A" : pass.getVehicle().getPlateNumber())
                            + " se het han sau " + daysUntilExpiry + " ngay. Vui long thanh toan ky moi neu muon tiep tuc giu cho."
                    : null;
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
                    pass.getPaidAt(),
                    daysUntilExpiry,
                    reminderDue,
                    reminderMessage,
                    pass.getNote(),
                    pass.getCreatedAt(),
                    pass.getUpdatedAt()
            );
        }
    }
}
