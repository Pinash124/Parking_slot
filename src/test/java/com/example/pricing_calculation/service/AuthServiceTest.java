package com.example.pricing_calculation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.pricing_calculation.domain.UserAccount;
import com.example.pricing_calculation.dto.AuthLoginRequest;
import com.example.pricing_calculation.dto.AuthLoginResponse;
import com.example.pricing_calculation.dto.AuthRegistrationRequest;
import com.example.pricing_calculation.dto.ChangePasswordRequest;
import com.example.pricing_calculation.dto.VerifyOtpRequest;
import com.example.pricing_calculation.dto.OtpResponse;
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
    void registerTriggersOtpAndVerifyingOtpSavesUserAndNeverStoresThePlainPassword() {
        String email = "customer@example.com";
        when(userAccountRepository.existsByEmailIgnoreCase(email)).thenReturn(false);
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OtpResponse otpResponse = authService.register(new AuthRegistrationRequest(
                "  Nguyen Van A  ",
                "  CUSTOMER@Example.COM ",
                " 0901234567 ",
                "Safe-password-123!"
        ));

        assertEquals("customer@example.com", otpResponse.email());
        assertEquals("OTP sent to your email. Please verify to complete registration.", otpResponse.message());

        String otp = authService.getPendingOtpForTesting(email);
        assertNotNull(otp);

        // Verify save was NOT called yet
        verify(userAccountRepository, never()).save(any(UserAccount.class));

        // Call unified verification (which registers the user and logs them in)
        AuthLoginResponse response = authService.verifyOtp(new VerifyOtpRequest(email, otp));

        ArgumentCaptor<UserAccount> userCaptor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountRepository).save(userCaptor.capture());
        UserAccount savedUser = userCaptor.getValue();

        assertEquals("Nguyen Van A", savedUser.getFullName());
        assertEquals("customer@example.com", savedUser.getEmail());
        assertEquals("0901234567", savedUser.getPhone());
        assertEquals("ACTIVE", savedUser.getStatus());
        assertEquals("CUSTOMER", savedUser.getRole());
        assertNotEquals("Safe-password-123!", savedUser.getPasswordHash());
        assertFalse(savedUser.getPasswordHash().isBlank());

        // Assert login elements of response
        assertNotNull(response.accessToken());
        assertEquals("Bearer", response.tokenType());
    }

    @Test
    void registerRejectsDuplicateEmailBeforeSendingOtp() {
        when(userAccountRepository.existsByEmailIgnoreCase("customer@example.com")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authService.register(new AuthRegistrationRequest(
                "Nguyen Van A",
                "customer@example.com",
                null,
                "Safe-password-123!"
        )));

        verify(userAccountRepository, never()).save(any(UserAccount.class));
    }

    @Test
    void registerRejectsWeakPasswords() {
        // Missing uppercase
        assertThrows(BadRequestException.class, () -> authService.register(new AuthRegistrationRequest(
                "Nguyen Van A", "customer@example.com", null, "safe-password-123!"
        )));
        // Missing special char
        assertThrows(BadRequestException.class, () -> authService.register(new AuthRegistrationRequest(
                "Nguyen Van A", "customer@example.com", null, "SafePassword123"
        )));
        // Missing number
        assertThrows(BadRequestException.class, () -> authService.register(new AuthRegistrationRequest(
                "Nguyen Van A", "customer@example.com", null, "Safe-Password!"
        )));
        // Missing letter
        assertThrows(BadRequestException.class, () -> authService.register(new AuthRegistrationRequest(
                "Nguyen Van A", "customer@example.com", null, "12345678?!"
        )));
    }

    @Test
    void loginTriggersOtpAndVerifyingOtpCreatesBearerTokenAndLogoutInvalidatesIt() {
        String email = "customer@example.com";
        UserAccount user = activeUser(email, "Safe-password-123!");
        when(userAccountRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
        when(userAccountRepository.findById(user.getId())).thenReturn(Optional.of(user));

        OtpResponse otpResponse = authService.login(new AuthLoginRequest(
                " CUSTOMER@example.com ",
                "Safe-password-123!"
        ));

        assertEquals("customer@example.com", otpResponse.email());
        String otp = authService.getPendingOtpForTesting(email);
        assertNotNull(otp);

        AuthLoginResponse login = authService.verifyOtp(new VerifyOtpRequest(email, otp));

        assertNotNull(login.accessToken());
        assertFalse(login.accessToken().isBlank());
        assertEquals("Bearer", login.tokenType());
        assertEquals("customer@example.com", login.email());
        assertEquals("CUSTOMER", login.role());
        assertEquals("Logout completed", authService.logout("Bearer " + login.accessToken()).message());
        assertThrows(UnauthorizedException.class,
                () -> authService.logout("Bearer " + login.accessToken()));
    }

    @Test
    void loginDoesNotRevealWhetherEmailOrPasswordWasWrong() {
        UserAccount user = activeUser("customer@example.com", "Safe-password-123!");
        when(userAccountRepository.findByEmailIgnoreCase("customer@example.com")).thenReturn(Optional.of(user));

        UnauthorizedException wrongPassword = assertThrows(
                UnauthorizedException.class,
                () -> authService.login(new AuthLoginRequest("customer@example.com", "wrong-Password-123!"))
        );
        UnauthorizedException unknownEmail = assertThrows(
                UnauthorizedException.class,
                () -> authService.login(new AuthLoginRequest("unknown@example.com", "wrong-Password-123!"))
        );

        assertEquals("Invalid email or password", wrongPassword.getMessage());
        assertEquals("Invalid email or password", unknownEmail.getMessage());
    }

    @Test
    void requireRoleUsesCurrentDatabaseRoleAndRejectsNonAdmin() {
        String email = "customer@example.com";
        UserAccount user = activeUser(email, "Safe-password-123!");
        when(userAccountRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
        when(userAccountRepository.findById(user.getId())).thenReturn(Optional.of(user));

        authService.login(new AuthLoginRequest(email, "Safe-password-123!"));
        AuthLoginResponse login = authService.verifyOtp(
                new VerifyOtpRequest(email, authService.getPendingOtpForTesting(email))
        );

        assertThrows(
                ForbiddenException.class,
                () -> authService.requireRole("Bearer " + login.accessToken(), "ADMIN")
        );

        user.setRole("ADMIN");
        assertEquals(
                user,
                authService.requireRole("Bearer " + login.accessToken(), "ADMIN")
        );
    }

    @Test
    void changePasswordUpdatesPasswordWhenAuthenticatedAndOtpVerified() {
        String email = "customer@example.com";
        UserAccount user = activeUser(email, "Old-password-123!");
        user.setId(5L);
        when(userAccountRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
        when(userAccountRepository.findById(5L)).thenReturn(Optional.of(user));
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Perform login to get session token
        OtpResponse loginOtpResponse = authService.login(new AuthLoginRequest(email, "Old-password-123!"));
        String loginOtp = authService.getPendingOtpForTesting(email);
        AuthLoginResponse login = authService.verifyOtp(new VerifyOtpRequest(email, loginOtp));

        String token = "Bearer " + login.accessToken();

        // Perform password change (triggers OTP)
        OtpResponse changeOtpResponse = authService.changePassword(token, new ChangePasswordRequest("Old-password-123!", "New-secure-password-456!"));
        assertEquals(email, changeOtpResponse.email());
        assertEquals("OTP sent to your email. Please verify to complete password change.", changeOtpResponse.message());

        String changeOtp = authService.getPendingOtpForTesting(email);
        assertNotNull(changeOtp);

        // Verify the change password OTP
        AuthLoginResponse changeVerifyResponse = authService.verifyOtp(new VerifyOtpRequest(email, changeOtp));
        assertNotNull(changeVerifyResponse.accessToken());

        verify(userAccountRepository).save(user);
        // Verify new password matches
        assertTrue(passwordHashService.matches("New-secure-password-456!", user.getPasswordHash()));
    }

    private UserAccount activeUser(String email, String password) {
        UserAccount user = new UserAccount();
        user.setId(1L);
        user.setFullName("Nguyen Van A");
        user.setEmail(email);
        user.setPhone("0901234567");
        user.setPasswordHash(passwordHashService.hash(password));
        user.setStatus("ACTIVE");
        user.setRole("CUSTOMER");
        return user;
    }
}
