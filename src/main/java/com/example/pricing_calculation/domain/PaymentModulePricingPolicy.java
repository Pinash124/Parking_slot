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
@Table(name = "PricingPolicies")
public class PaymentModulePricingPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "policy_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_type_id")
    private VehicleTypeEntity vehicleType;

    @Column(name = "policy_name", length = 100)
    private String policyName;

    @Column(name = "hourly_rate", precision = 18, scale = 2)
    private BigDecimal hourlyRate;

    @Column(name = "daily_rate", precision = 18, scale = 2)
    private BigDecimal dailyRate;

    @Column(name = "lost_ticket_fee", precision = 18, scale = 2)
    private BigDecimal lostTicketFee;

    @Column(name = "overtime_fee", precision = 18, scale = 2)
    private BigDecimal overtimeFee;

    @Column(name = "effective_from")
    private LocalDateTime effectiveFrom;

    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;

    @Column(name = "status", length = 20)
    private String status;

    public Long getId() {
        return id;
    }

    public VehicleTypeEntity getVehicleType() {
        return vehicleType;
    }

    public String getPolicyName() {
        return policyName;
    }

    public BigDecimal getHourlyRate() {
        return hourlyRate;
    }

    public BigDecimal getDailyRate() {
        return dailyRate;
    }

    public BigDecimal getLostTicketFee() {
        return lostTicketFee;
    }

    public BigDecimal getOvertimeFee() {
        return overtimeFee;
    }

    public LocalDateTime getEffectiveFrom() {
        return effectiveFrom;
    }

    public LocalDateTime getEffectiveTo() {
        return effectiveTo;
    }

    public String getStatus() {
        return status;
    }
}
