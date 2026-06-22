package com.smartparking.model.schemas;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
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

    @Column(name = "incident_type")
    private String incidentType;

    private String description;

    @Transient
    private BigDecimal penaltyAmount;

    private String status;

    @Column(name = "reported_by")
    private Long createdBy;

    @Transient
    private Long resolvedBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Transient
    private LocalDateTime resolvedAt;
}
