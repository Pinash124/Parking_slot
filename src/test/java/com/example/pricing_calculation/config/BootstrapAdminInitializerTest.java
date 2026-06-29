package com.example.pricing_calculation.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.pricing_calculation.domain.UserAccount;
import com.example.pricing_calculation.domain.UserRole;
import com.example.pricing_calculation.repository.UserAccountRepository;
import com.example.pricing_calculation.service.PasswordHashService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class BootstrapAdminInitializerTest {

    @Test
    void createsAdminWhenOtherUsersAlreadyExist() throws Exception {
        UserAccountRepository users = mock(UserAccountRepository.class);
        PasswordHashService passwords = mock(PasswordHashService.class);
        when(users.existsByEmailIgnoreCase("admin@smartparking.local")).thenReturn(false);
        when(passwords.hash("Admin@12345")).thenReturn("hashed-password");

        new BootstrapAdminInitializer(
                users, passwords, " Admin@SmartParking.Local ", "Admin@12345").run();

        ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(users).save(captor.capture());
        UserAccount admin = captor.getValue();
        assertEquals("admin@smartparking.local", admin.getEmail());
        assertEquals("hashed-password", admin.getPasswordHash());
        assertEquals("ACTIVE", admin.getStatus());
        assertEquals(UserRole.ADMINISTRATOR.code(), admin.getRole());
    }

    @Test
    void doesNotDuplicateExistingBootstrapAdmin() throws Exception {
        UserAccountRepository users = mock(UserAccountRepository.class);
        PasswordHashService passwords = mock(PasswordHashService.class);
        when(users.existsByEmailIgnoreCase("admin@smartparking.local")).thenReturn(true);

        new BootstrapAdminInitializer(
                users, passwords, "admin@smartparking.local", "Admin@12345").run();

        verify(users, never()).save(any());
        verify(passwords, never()).hash(any());
    }
}
