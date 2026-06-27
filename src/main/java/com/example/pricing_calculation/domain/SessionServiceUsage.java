package com.example.pricing_calculation.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "SessionServiceUsages")
public class SessionServiceUsage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "usage_id")
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id")
    private PaymentModuleParkingSession session;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id")
    private AdditionalService service;
    @Column(nullable = false)
    private Integer quantity;
    @Column(name = "unit_price", precision = 18, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    public Long getId() { return id; }
    public PaymentModuleParkingSession getSession() { return session; }
    public AdditionalService getService() { return service; }
    public Integer getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setSession(PaymentModuleParkingSession session) { this.session = session; }
    public void setService(AdditionalService service) { this.service = service; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
}
