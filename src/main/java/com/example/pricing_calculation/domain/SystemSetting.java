package com.example.pricing_calculation.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "SystemSettings")
public class SystemSetting {
    @Id @Column(name = "setting_key", length = 100)
    private String key;
    @Column(name = "setting_value", length = 1000)
    private String value;
    @Column(length = 255)
    private String description;
    public String getKey() { return key; }
    public String getValue() { return value; }
    public String getDescription() { return description; }
    public void setKey(String key) { this.key = key; }
    public void setValue(String value) { this.value = value; }
    public void setDescription(String description) { this.description = description; }
}
