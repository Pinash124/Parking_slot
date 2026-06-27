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

@Entity
@Table(name = "Floors")
public class Floor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "floor_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "building_id")
    private Building building;

    @Column(name = "floor_name", length = 50)
    private String floorName;

    @Column(name = "floor_number")
    private Integer floorNumber;

    public Long getId() {
        return id;
    }

    public Building getBuilding() {
        return building;
    }

    public String getFloorName() {
        return floorName;
    }

    public Integer getFloorNumber() {
        return floorNumber;
    }

    public void setBuilding(Building building) { this.building = building; }
    public void setFloorName(String floorName) { this.floorName = floorName; }
    public void setFloorNumber(Integer floorNumber) { this.floorNumber = floorNumber; }
}
