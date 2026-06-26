package com.smartparking.model.schemas;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "vehicle_types")
@Getter
@Setter
public class VehicleType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vehicle_type_id")
    private Long vehicleTypeId;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "default_hourly_fee")
    private BigDecimal defaultHourlyFee;
}
