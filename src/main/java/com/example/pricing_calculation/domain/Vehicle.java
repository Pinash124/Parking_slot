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
@Table(name = "Vehicles")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vehicle_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_type_id")
    private VehicleTypeEntity vehicleType;

    @Column(name = "plate_number", nullable = false, length = 20, unique = true)
    private String plateNumber;

    @Column(name = "brand", length = 50)
    private String brand;

    @Column(name = "color", length = 30)
    private String color;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "qr_code", length = 255, unique = true)
    private String qrCode;

    public Long getId() {
        return id;
    }

    public UserAccount getUser() {
        return user;
    }

    public VehicleTypeEntity getVehicleType() {
        return vehicleType;
    }

    public String getPlateNumber() {
        return plateNumber;
    }

    public String getBrand() {
        return brand;
    }

    public String getColor() {
        return color;
    }

    public String getStatus() {
        return status;
    }

    public String getQrCode() {
        return qrCode;
    }

    public void setUser(UserAccount user) { this.user = user; }
    public void setVehicleType(VehicleTypeEntity vehicleType) { this.vehicleType = vehicleType; }
    public void setPlateNumber(String plateNumber) { this.plateNumber = plateNumber == null ? null : plateNumber.trim().toUpperCase(); }
    public void setBrand(String brand) { this.brand = brand; }
    public void setColor(String color) { this.color = color; }
    public void setStatus(String status) { this.status = status; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode == null ? null : qrCode.trim(); }
}
