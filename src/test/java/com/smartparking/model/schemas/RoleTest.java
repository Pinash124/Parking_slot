package com.smartparking.model.schemas;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RoleTest {

    @Test
    void recognizesApprovedRoleAliases() {
        assertEquals(Role.CUSTOMER, Role.from("user"));
        assertEquals(Role.MANAGER, Role.from("parking manager"));
        assertEquals(Role.ADMIN, Role.from("system-admin"));
        assertEquals(Role.STAFF, Role.from("staff"));
    }
}
