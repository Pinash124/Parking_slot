package com.example.pricing_calculation.dto;
import com.example.pricing_calculation.domain.IncidentReport;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public final class IncidentDtos {private IncidentDtos(){}
 public record IncidentRequest(Long sessionId,String incidentType,String description,BigDecimal penaltyAmount){}
 public record IncidentResponse(Long id,Long sessionId,Long reportedBy,String incidentType,String description,BigDecimal penaltyAmount,String status,LocalDateTime createdAt,Long resolvedBy,LocalDateTime resolvedAt){public static IncidentResponse from(IncidentReport x){return new IncidentResponse(x.getId(),x.getSession().getId(),x.getReportedBy()==null?null:x.getReportedBy().getId(),x.getIncidentType(),x.getDescription(),x.getPenaltyAmount(),x.getStatus(),x.getCreatedAt(),x.getResolvedBy()==null?null:x.getResolvedBy().getId(),x.getResolvedAt());}}
}
