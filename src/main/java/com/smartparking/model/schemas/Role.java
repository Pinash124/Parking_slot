package com.smartparking.model.schemas;

public enum Role {
    CUSTOMER,
    STAFF,
    MANAGER;

    public static Role from(String value) {
        if (value == null || value.isBlank()) {
            return CUSTOMER;
        }

        String normalized = value.trim().toUpperCase();
        if ("USER".equals(normalized) || "DRIVER".equals(normalized)) {
            return CUSTOMER;
        }
        if ("ADMIN".equals(normalized)) {
            return MANAGER;
        }

        return Role.valueOf(normalized);
    }
}
