package com.example.pricing_calculation.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "AdditionalServices")
public class AdditionalService {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "service_id")
    private Long id;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(precision = 18, scale = 2, nullable = false)
    private BigDecimal price;
    @Column(length = 20)
    private String status;

    public Long getId() { return id; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
    public String getStatus() { return status; }
    public void setName(String name) { this.name = name; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public void setStatus(String status) { this.status = status; }
}
