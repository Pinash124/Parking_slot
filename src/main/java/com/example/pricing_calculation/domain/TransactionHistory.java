package com.example.pricing_calculation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Entity
@Table(name = "Transactions")
public class TransactionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Column(name = "reference_code", length = 100)
    private String referenceCode;

    @Column(name = "gateway", length = 50)
    private String gateway;

    @Column(name = "status", length = 20)
    private String status;

    @PrePersist
    void prePersist() {
        if (referenceCode == null || referenceCode.isBlank()) {
            referenceCode = generateReferenceCode();
        }
        if (status == null || status.isBlank()) {
            status = TransactionStatus.PENDING.name();
        }
    }

    private String generateReferenceCode() {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "TXN-" + date + "-" + suffix;
    }

    public Long getId() {
        return id;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public String getReferenceCode() {
        return referenceCode;
    }

    public void setReferenceCode(String referenceCode) {
        this.referenceCode = normalize(referenceCode);
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = normalize(gateway);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = normalize(status);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
