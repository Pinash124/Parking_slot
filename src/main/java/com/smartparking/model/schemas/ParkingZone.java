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

import java.time.LocalDateTime;

@Entity
@Table(name = "zones")
@Getter
@Setter
public class ParkingZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "zone_id")
    private Long zoneId;

    @Transient
    private Long buildingId;

    @Column(name = "floor_id")
    private Long floorId;

    @Column(name = "vehicle_type_id")
    private Long vehicleTypeId;

    @Column(name = "zone_name")
    private String name;

    @Transient
    private Integer floorNumber;

    @Transient
    private String description;

    @Transient
    private String status;

    @Transient
    private LocalDateTime createdAt;

    @Transient
    private LocalDateTime updatedAt;
}
