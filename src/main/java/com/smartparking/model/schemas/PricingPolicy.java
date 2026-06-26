package com.smartparking.model.schemas;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pricing_policies")
@Getter
@Setter
public class PricingPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "policy_id")
    private Long policyId;

    @Column(name = "vehicle_type_id")
    private Long vehicleTypeId;

    @Column(name = "policy_name")
    private String policyName;

    @Column(name = "hourly_rate")
    private BigDecimal hourlyRate;

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

    private String status;
}
