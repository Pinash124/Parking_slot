package com.smartparking.model.schemas;

public enum Role {
    CUSTOMER,
    STAFF,
    MANAGER,
    ADMIN;

    public static Role from(String value) {
        if (value == null || value.isBlank()) {
            return CUSTOMER;
        }

        String normalized = value.trim()
                .toUpperCase()
                .replace('-', '_')
                .replace(' ', '_');
        if ("USER".equals(normalized) || "DRIVER".equals(normalized)) {
            return CUSTOMER;
        }
        if ("PARKING_MANAGER".equals(normalized) || "PARKINGMANAGER".equals(normalized)) {
            return MANAGER;
        }
        if ("ADMIN".equals(normalized)
                || "SYSTEM_ADMIN".equals(normalized)
                || "SYSTEMADMIN".equals(normalized)
                || "SYS_ADMIN".equals(normalized)) {
            return ADMIN;
        }

        return Role.valueOf(normalized);
    }
}
