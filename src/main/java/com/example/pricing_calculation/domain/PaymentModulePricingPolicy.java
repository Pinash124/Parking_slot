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

    @Column(name = "hourly_billing_mode", length = 20)
    private String hourlyBillingMode;

    @Column(name = "hourly_billing_block_hours")
    private Integer hourlyBillingBlockHours;

    @Column(name = "daily_rate", precision = 18, scale = 2)
    private BigDecimal dailyRate;

    @Column(name = "daily_billing_mode", length = 20)
    private String dailyBillingMode;

    @Column(name = "daily_billing_block_hours")
    private Integer dailyBillingBlockHours;

    @Column(name = "monthly_rate", precision = 18, scale = 2)
    private BigDecimal monthlyRate;

    @Column(name = "fixed_surcharge", precision = 18, scale = 2)
    private BigDecimal fixedSurcharge;

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

    public String getHourlyBillingMode() {
        return hourlyBillingMode;
    }

    public Integer getHourlyBillingBlockHours() {
        return hourlyBillingBlockHours;
    }

    public BigDecimal getDailyRate() {
        return dailyRate;
    }

    public String getDailyBillingMode() {
        return dailyBillingMode;
    }

    public Integer getDailyBillingBlockHours() {
        return dailyBillingBlockHours;
    }

    public BigDecimal getMonthlyRate() { return monthlyRate; }
    public BigDecimal getFixedSurcharge() { return fixedSurcharge; }

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

    public void setVehicleType(VehicleTypeEntity vehicleType) { this.vehicleType = vehicleType; }
    public void setPolicyName(String policyName) { this.policyName = policyName; }
    public void setHourlyRate(BigDecimal hourlyRate) { this.hourlyRate = hourlyRate; }
    public void setHourlyBillingMode(String hourlyBillingMode) { this.hourlyBillingMode = hourlyBillingMode; }
    public void setHourlyBillingBlockHours(Integer hourlyBillingBlockHours) { this.hourlyBillingBlockHours = hourlyBillingBlockHours; }
    public void setDailyRate(BigDecimal dailyRate) { this.dailyRate = dailyRate; }
    public void setDailyBillingMode(String dailyBillingMode) { this.dailyBillingMode = dailyBillingMode; }
    public void setDailyBillingBlockHours(Integer dailyBillingBlockHours) { this.dailyBillingBlockHours = dailyBillingBlockHours; }
    public void setMonthlyRate(BigDecimal monthlyRate) { this.monthlyRate = monthlyRate; }
    public void setFixedSurcharge(BigDecimal fixedSurcharge) { this.fixedSurcharge = fixedSurcharge; }
    public void setLostTicketFee(BigDecimal lostTicketFee) { this.lostTicketFee = lostTicketFee; }
    public void setOvertimeFee(BigDecimal overtimeFee) { this.overtimeFee = overtimeFee; }
    public void setEffectiveFrom(LocalDateTime effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    public void setEffectiveTo(LocalDateTime effectiveTo) { this.effectiveTo = effectiveTo; }
    public void setStatus(String status) { this.status = status; }
}
