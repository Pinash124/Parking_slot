package com.example.pricing_calculation.dto;

import com.example.pricing_calculation.domain.Violation;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class ViolationDtos {

    private ViolationDtos() {
    }

    public record ViolationRequest(Long sessionId, String violationType, String description, BigDecimal penaltyAmount, String status) {
    }

    public record ViolationResponse(
            Long id,
            Long sessionId,
            Long userId,
            String userEmail,
            String ticketCode,
            String violationType,
            String description,
            BigDecimal penaltyAmount,
            String status,
            LocalDateTime createdAt) {
        public static ViolationResponse from(Violation violation) {
            return new ViolationResponse(
                    violation.getId(),
                    violation.getSession().getId(),
                    violation.getSession().getVehicle().getUser().getId(),
                    violation.getSession().getVehicle().getUser().getEmail(),
                    violation.getSession().getTicketCode(),
                    violation.getViolationType(),
                    violation.getDescription(),
                    violation.getPenaltyAmount(),
                    violation.getStatus(),
                    violation.getCreatedAt()
            );
        }
    }
}
