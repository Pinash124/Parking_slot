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
import java.time.LocalTime;

@Entity
@Table(name = "pricing_policies")
@Getter
@Setter
public class PricingPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "policy_id")
    private Long pricingPolicyId;

    @Column(name = "vehicle_type_id")
    private Long vehicleTypeId;

    @Column(name = "policy_name")
    private String name;

    @Column(name = "hourly_rate")
    private BigDecimal priceAmount;

    @Column(name = "daily_rate")
    private BigDecimal dailyRate;

    @Column(name = "lost_ticket_fee")
    private BigDecimal lostTicketFee;

    @Column(name = "overtime_fee")
    private BigDecimal overtimeFee;

    @Column(name = "effective_from")
    private LocalDateTime effectiveFrom;

    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;

    @Transient
    private String pricingUnit;

    @Transient
    private LocalTime peakStartTime;

    @Transient
    private LocalTime peakEndTime;

    @Transient
    private BigDecimal peakMultiplier;

    @Transient
    private String rules;

    private String status;

    @Transient
    private LocalDateTime createdAt;

    @Transient
    private LocalDateTime updatedAt;
}
