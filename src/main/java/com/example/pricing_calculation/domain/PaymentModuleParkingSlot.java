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
@Table(name = "ParkingSlots")
public class PaymentModuleParkingSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "slot_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "zone_id")
    private Zone zone;

    @Column(name = "slot_code", nullable = false, length = 20)
    private String slotCode;

    @Column(name = "status", length = 20)
    private String status;

    public Long getId() {
        return id;
    }

    public Zone getZone() {
        return zone;
    }

    public String getSlotCode() {
        return slotCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = normalize(status);
    }

    public void setZone(Zone zone) { this.zone = zone; }
    public void setSlotCode(String slotCode) { this.slotCode = normalize(slotCode); }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
