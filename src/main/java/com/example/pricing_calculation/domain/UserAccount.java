package com.example.pricing_calculation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "Users")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "role", length = 20)
    private String role;

    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getStatus() {
        return status;
    }

    public String getRole() {
        return role;
    }

    public void setFullName(String fullName) {
        this.fullName = normalize(fullName);
    }

    public void setEmail(String email) {
        this.email = normalize(email);
    }

    public void setPhone(String phone) {
        this.phone = normalize(phone);
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = normalize(passwordHash);
    }

    public void setStatus(String status) {
        this.status = normalize(status);
    }

    public void setRole(String role) {
        this.role = normalize(role);
    }

    public java.time.LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.time.LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
