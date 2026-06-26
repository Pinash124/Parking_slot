package com.smartparking.model.schemas;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "buildings")
@Getter
@Setter
public class ParkingBuilding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "building_id")
    private Long buildingId;

    private String name;

    private String address;

    private String description;

    private String status;
}
