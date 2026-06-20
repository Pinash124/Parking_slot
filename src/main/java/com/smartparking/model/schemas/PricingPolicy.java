package com.smartparking.model.schemas;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
    @Column(name = "pricing_policy_id")
    private Long pricingPolicyId;

    @Column(name = "vehicle_type_id")
    private Long vehicleTypeId;

    @Column(nullable = false)
    private String name;

    @Column(name = "price_amount", nullable = false)
    private BigDecimal priceAmount;

    @Column(name = "pricing_unit")
    private String pricingUnit;

    @Column(name = "peak_start_time")
    private LocalTime peakStartTime;

    @Column(name = "peak_end_time")
    private LocalTime peakEndTime;

    @Column(name = "peak_multiplier")
    private BigDecimal peakMultiplier;

    private String rules;

    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
