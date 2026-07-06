package com.example.pricing_calculation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "VehicleTypes")
public class VehicleTypeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vehicle_type_id")
    private Long id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "default_hourly_fee", precision = 18, scale = 2)
    private BigDecimal defaultHourlyFee;

    @Column(name = "daily_rate", precision = 18, scale = 2)
    private BigDecimal dailyRate;

    @Column(name = "monthly_rate", precision = 18, scale = 2)
    private BigDecimal monthlyRate;

    @Column(name = "wheel_count")
    private Integer wheelCount;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getDefaultHourlyFee() {
        return defaultHourlyFee;
    }

    public BigDecimal getDailyRate() {
        return dailyRate;
    }

    public BigDecimal getMonthlyRate() {
        return monthlyRate;
    }

    public Integer getWheelCount() { return wheelCount; }

    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setDefaultHourlyFee(BigDecimal defaultHourlyFee) { this.defaultHourlyFee = defaultHourlyFee; }
    public void setDailyRate(BigDecimal dailyRate) { this.dailyRate = dailyRate; }
    public void setMonthlyRate(BigDecimal monthlyRate) { this.monthlyRate = monthlyRate; }
    public void setWheelCount(Integer wheelCount) { this.wheelCount = wheelCount; }
}
