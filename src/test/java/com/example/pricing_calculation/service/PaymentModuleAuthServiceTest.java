package com.example.pricing_calculation.service;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.pricing_calculation.domain.AuthAccessSession;
import com.example.pricing_calculation.domain.UserAccount;
import com.example.pricing_calculation.domain.UserRole;
import com.example.pricing_calculation.dto.AuthLoginRequest;
import com.example.pricing_calculation.dto.AuthLoginResponse;
import com.example.pricing_calculation.repository.AuthAccessSessionRepository;
import com.example.pricing_calculation.repository.UserAccountRepository;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;

class PaymentModuleAuthServiceTest {

    @Test
    void keepsMultiplePersistentLoginSessionsValid() {
        UserAccountRepository users = mock(UserAccountRepository.class);
        PasswordHashService passwords = mock(PasswordHashService.class);
        OtpDeliveryService otpDelivery = mock(OtpDeliveryService.class);
        AuthAccessSessionRepository sessions = mock(AuthAccessSessionRepository.class);
        Map<String, AuthAccessSession> storedSessions = new ConcurrentHashMap<>();

        UserAccount staff = mock(UserAccount.class);
        when(staff.getId()).thenReturn(7L);
        when(staff.getFullName()).thenReturn("Parking Staff");
        when(staff.getEmail()).thenReturn("staff@example.com");
        when(staff.getPasswordHash()).thenReturn("hash");
        when(staff.getStatus()).thenReturn("ACTIVE");
        when(staff.getRole()).thenReturn(UserRole.PARKING_STAFF.code());
        when(users.findByEmailIgnoreCase("staff@example.com")).thenReturn(Optional.of(staff));
        when(users.findById(7L)).thenReturn(Optional.of(staff));
        when(passwords.matches("Password@123", "hash")).thenReturn(true);
        when(sessions.save(any(AuthAccessSession.class))).thenAnswer(invocation -> {
            AuthAccessSession session = invocation.getArgument(0);
            storedSessions.put(session.getTokenHash(), session);
            return session;
        });
        when(sessions.findById(any(String.class)))
                .thenAnswer(invocation -> Optional.ofNullable(storedSessions.get(invocation.getArgument(0))));

        PaymentModuleAuthService firstInstance = new PaymentModuleAuthService(users, passwords, otpDelivery, sessions);
        AuthLoginResponse first = firstInstance.loginDirect(new AuthLoginRequest("staff@example.com", "Password@123"));
        AuthLoginResponse second = firstInstance.loginDirect(new AuthLoginRequest("staff@example.com", "Password@123"));

        assertNotEquals(first.accessToken(), second.accessToken());
        assertSame(staff, firstInstance.authenticate("Bearer " + first.accessToken()));
        assertSame(staff, firstInstance.authenticate("Bearer " + second.accessToken()));

        PaymentModuleAuthService restartedInstance = new PaymentModuleAuthService(users, passwords, otpDelivery, sessions);
        assertSame(staff, restartedInstance.authenticate("Bearer " + first.accessToken()));
        assertSame(staff, restartedInstance.authenticate("Bearer " + second.accessToken()));
    }
}
