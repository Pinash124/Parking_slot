package com.smartparking.model.schemas;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "incident_reports")
@Getter
@Setter
public class ParkingIncident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "incident_id")
    private Long incidentId;

    @Column(name = "session_id")
    private Long sessionId;

    @Column(name = "reported_by")
    private Long reportedBy;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "resolved_by")
    private Long resolvedBy;

    @Column(name = "incident_type")
    private String incidentType;

    private String description;

    @Column(name = "penalty_amount")
    private BigDecimal penaltyAmount;

    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
}
