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
@Table(name = "Gates")
public class Gate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "gate_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "building_id")
    private Building building;

    @Column(name = "gate_name", length = 50)
    private String gateName;

    @Column(name = "gate_type", length = 20)
    private String gateType;

    public Long getId() {
        return id;
    }

    public Building getBuilding() {
        return building;
    }

    public void setBuilding(Building building) {
        this.building = building;
    }

    public String getGateName() {
        return gateName;
    }

    public void setGateName(String gateName) {
        this.gateName = normalize(gateName);
    }

    public String getGateType() {
        return gateType;
    }

    public void setGateType(String gateType) {
        this.gateType = normalize(gateType);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
