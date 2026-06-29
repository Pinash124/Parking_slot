package com.example.pricing_calculation.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class PasswordHashServiceTest {

    private final PasswordHashService passwords = new PasswordHashService();

    @Test
    void acceptsLegacyBcryptPasswordFromDatabase() {
        String databaseHash = new BCryptPasswordEncoder().encode("Thong@654321");

        assertTrue(passwords.matches("Thong@654321", databaseHash));
        assertFalse(passwords.matches("wrong-password", databaseHash));
    }

    @Test
    void stillAcceptsCurrentPbkdf2Passwords() {
        String hash = passwords.hash("Thong@654321");

        assertTrue(passwords.matches("Thong@654321", hash));
        assertFalse(passwords.matches("wrong-password", hash));
    }
}
