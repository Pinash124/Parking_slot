package com.example.pricing_calculation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.pricing_calculation.domain.UserAccount;
import com.example.pricing_calculation.domain.UserRole;
import com.example.pricing_calculation.dto.AuthLoginRequest;
import com.example.pricing_calculation.dto.AuthLoginResponse;
import com.example.pricing_calculation.dto.AuthRegistrationRequest;
import com.example.pricing_calculation.dto.AuthRegistrationResponse;
import com.example.pricing_calculation.repository.UserAccountRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AuthServiceTest {

    private UserAccountRepository userAccountRepository;
    private PasswordHashService passwordHashService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userAccountRepository = mock(UserAccountRepository.class);
        passwordHashService = new PasswordHashService();
        authService = new AuthService(userAccountRepository, passwordHashService);
    }

    @Test
    void registerNormalizesInputAndNeverStoresThePlainPassword() {
        when(userAccountRepository.existsByEmailIgnoreCase("customer@example.com")).thenReturn(false);
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthRegistrationResponse response = authService.register(new AuthRegistrationRequest(
                "  Nguyen Van A  ",
                "  CUSTOMER@Example.COM ",
                " 0901234567 ",
                "safe-password-123"
        ));

        ArgumentCaptor<UserAccount> userCaptor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountRepository).save(userCaptor.capture());
        UserAccount savedUser = userCaptor.getValue();

        assertEquals("Nguyen Van A", savedUser.getFullName());
        assertEquals("customer@example.com", savedUser.getEmail());
        assertEquals("0901234567", savedUser.getPhone());
        assertEquals("ACTIVE", savedUser.getStatus());
        assertEquals(UserRole.PARKING_USER.code(), savedUser.getRole());
        assertNotEquals("safe-password-123", savedUser.getPasswordHash());
        assertFalse(savedUser.getPasswordHash().isBlank());
        assertEquals("Registration completed", response.message());
    }

    @Test
    void registerRejectsDuplicateEmailBeforeSaving() {
        when(userAccountRepository.existsByEmailIgnoreCase("customer@example.com")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authService.register(new AuthRegistrationRequest(
                "Nguyen Van A",
                "customer@example.com",
                null,
                "safe-password-123"
        )));

        verify(userAccountRepository, never()).save(any(UserAccount.class));
    }

    @Test
    void loginCreatesBearerTokenAndLogoutInvalidatesIt() {
        UserAccount user = activeUser("customer@example.com", "safe-password-123");
        when(userAccountRepository.findByEmailIgnoreCase("customer@example.com")).thenReturn(Optional.of(user));

        AuthLoginResponse login = authService.login(new AuthLoginRequest(
                " CUSTOMER@example.com ",
                "safe-password-123"
        ));

        assertNotNull(login.accessToken());
        assertFalse(login.accessToken().isBlank());
        assertEquals("Bearer", login.tokenType());
        assertEquals("customer@example.com", login.email());
        assertEquals(UserRole.PARKING_USER.code(), login.role());
        assertEquals("Logout completed", authService.logout("Bearer " + login.accessToken()).message());
        assertThrows(UnauthorizedException.class,
                () -> authService.logout("Bearer " + login.accessToken()));
    }

    @Test
    void loginDoesNotRevealWhetherEmailOrPasswordWasWrong() {
        UserAccount user = activeUser("customer@example.com", "safe-password-123");
        when(userAccountRepository.findByEmailIgnoreCase("customer@example.com")).thenReturn(Optional.of(user));

        UnauthorizedException wrongPassword = assertThrows(
                UnauthorizedException.class,
                () -> authService.login(new AuthLoginRequest("customer@example.com", "wrong-password"))
        );
        UnauthorizedException unknownEmail = assertThrows(
                UnauthorizedException.class,
                () -> authService.login(new AuthLoginRequest("unknown@example.com", "wrong-password"))
        );

        assertEquals("Invalid email or password", wrongPassword.getMessage());
        assertEquals("Invalid email or password", unknownEmail.getMessage());
    }

    private UserAccount activeUser(String email, String password) {
        UserAccount user = new UserAccount();
        user.setFullName("Nguyen Van A");
        user.setEmail(email);
        user.setPhone("0901234567");
        user.setPasswordHash(passwordHashService.hash(password));
        user.setStatus("ACTIVE");
        user.setRole(UserRole.PARKING_USER.code());
        return user;
    }
}
