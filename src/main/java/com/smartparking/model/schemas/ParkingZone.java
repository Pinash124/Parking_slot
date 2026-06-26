package com.smartparking.model.schemas;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "zones")
@Getter
@Setter
public class ParkingZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "zone_id")
    private Long zoneId;

    @Column(name = "floor_id")
    private Long floorId;

    @Column(name = "vehicle_type_id")
    private Long vehicleTypeId;

    @Column(name = "zone_name")
    private String zoneName;
}
