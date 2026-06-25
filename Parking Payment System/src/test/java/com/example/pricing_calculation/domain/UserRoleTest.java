package com.example.pricing_calculation.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UserRoleTest {

    @Test
    void parkingUserRoleContainsDriverCapabilities() {
        UserRole role = UserRole.PARKING_USER;

        assertEquals("PARKING_USER", role.code());
        assertEquals("Parking User / Driver", role.displayName());
        assertTrue(role.capabilities().stream().anyMatch(capability -> capability.contains("Xem thong tin bai xe")));
        assertTrue(role.capabilities().stream().anyMatch(capability -> capability.contains("Dat cho truoc")));
        assertTrue(role.capabilities().stream().anyMatch(capability -> capability.contains("Thanh toan phi gui xe")));
        assertTrue(role.capabilities().stream().anyMatch(capability -> capability.contains("Gui phan hoi")));
    }

    @Test
    void legacyCustomerAndDriverCodesMapToParkingUser() {
        assertEquals(UserRole.PARKING_USER, UserRole.fromCode("CUSTOMER"));
        assertEquals(UserRole.PARKING_USER, UserRole.fromCode("driver"));
        assertEquals(UserRole.PARKING_USER, UserRole.fromCode(" PARKING_USER "));
    }
}
