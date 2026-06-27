package com.example.pricing_calculation.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "SystemDevices")
public class SystemDevice {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "device_id")
    private Long id;
    @Column(name = "device_code", unique = true, nullable = false, length = 50)
    private String deviceCode;
    @Column(name = "device_type", nullable = false, length = 30)
    private String deviceType;
    @Column(name = "lane_code", length = 50)
    private String laneCode;
    @Column(length = 20)
    private String status;
    @Column(name = "configuration_json", length = 2000)
    private String configurationJson;
    public Long getId() { return id; }
    public String getDeviceCode() { return deviceCode; }
    public String getDeviceType() { return deviceType; }
    public String getLaneCode() { return laneCode; }
    public String getStatus() { return status; }
    public String getConfigurationJson() { return configurationJson; }
    public void setDeviceCode(String deviceCode) { this.deviceCode = deviceCode; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }
    public void setLaneCode(String laneCode) { this.laneCode = laneCode; }
    public void setStatus(String status) { this.status = status; }
    public void setConfigurationJson(String configurationJson) { this.configurationJson = configurationJson; }
}
