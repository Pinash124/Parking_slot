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
@Table(name = "Zones")
public class Zone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "zone_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "floor_id")
    private Floor floor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_type_id")
    private VehicleTypeEntity vehicleType;

    @Column(name = "zone_name", length = 50)
    private String zoneName;

    @Column(name = "zone_type", length = 30)
    private String zoneType;

    public Long getId() {
        return id;
    }

    public Floor getFloor() {
        return floor;
    }

    public VehicleTypeEntity getVehicleType() {
        return vehicleType;
    }

    public String getZoneName() {
        return zoneName;
    }

    public String getZoneType() { return zoneType; }

    public void setFloor(Floor floor) { this.floor = floor; }
    public void setVehicleType(VehicleTypeEntity vehicleType) { this.vehicleType = vehicleType; }
    public void setZoneName(String zoneName) { this.zoneName = zoneName; }
    public void setZoneType(String zoneType) { this.zoneType = zoneType == null ? null : zoneType.trim().toUpperCase(); }
}
