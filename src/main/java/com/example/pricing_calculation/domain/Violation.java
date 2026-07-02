package com.example.pricing_calculation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Violations")
public class Violation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "violation_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id")
    private PaymentModuleParkingSession session;

    @Column(name = "violation_type", length = 100)
    private String violationType;

    @Column(name = "description")
    private String description;

    @Column(name = "penalty_amount", precision = 18, scale = 2)
    private BigDecimal penaltyAmount;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public PaymentModuleParkingSession getSession() {
        return session;
    }

    public void setSession(PaymentModuleParkingSession session) {
        this.session = session;
    }

    public String getViolationType() {
        return violationType;
    }

    public void setViolationType(String violationType) {
        this.violationType = normalize(violationType);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = normalize(description);
    }

    public BigDecimal getPenaltyAmount() {
        return penaltyAmount;
    }

    public void setPenaltyAmount(BigDecimal penaltyAmount) {
        this.penaltyAmount = penaltyAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = normalize(status);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
