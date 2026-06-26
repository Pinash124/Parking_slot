package com.smartparking.controller;

import com.smartparking.model.requests.AuthResponse;
import com.smartparking.model.requests.ChangePasswordRequest;
import com.smartparking.model.requests.ForgotPasswordRequest;
import com.smartparking.model.requests.LoginRequest;
import com.smartparking.model.requests.OtpChallengeResponse;
import com.smartparking.model.requests.PasswordResetResponse;
import com.smartparking.model.requests.RegisterRequest;
import com.smartparking.model.requests.ResetPasswordRequest;
import com.smartparking.model.requests.UpdateProfileRequest;
import com.smartparking.model.requests.UserResponse;
import com.smartparking.model.requests.VerifyOtpRequest;
import com.smartparking.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final ObjectProvider<ClientRegistrationRepository> clientRegistrations;

    public AuthController(AuthService authService,
            ObjectProvider<ClientRegistrationRepository> clientRegistrations) {
        this.authService = authService;
        this.clientRegistrations = clientRegistrations;
    }

    @PostMapping("/register")
    public ResponseEntity<OtpChallengeResponse> register(@Valid @RequestBody RegisterRequest request) {
        OtpChallengeResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register/verify")
    public ResponseEntity<AuthResponse> verifyRegistration(@Valid @RequestBody VerifyOtpRequest request) {
        AuthResponse response = authService.verifyRegistration(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<OtpChallengeResponse> login(@Valid @RequestBody LoginRequest request) {
        OtpChallengeResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login/verify")
    public ResponseEntity<AuthResponse> verifyLogin(@Valid @RequestBody VerifyOtpRequest request) {
        AuthResponse response = authService.verifyLogin(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<PasswordResetResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        PasswordResetResponse response = authService.forgotPassword(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<AuthResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        AuthResponse response = authService.resetPassword(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/google")
    public ResponseEntity<?> loginWithGoogle() {
        ClientRegistrationRepository registrations = clientRegistrations.getIfAvailable();
        if (registrations == null || registrations.findByRegistrationId("google") == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("message",
                            "Google login is not configured. Please set GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET."));
        }

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, URI.create("/oauth2/authorization/google").toString())
                .build();
    }

    @GetMapping("/oauth2/success")
    public ResponseEntity<?> googleLoginSuccess(@AuthenticationPrincipal OAuth2User googleUser) {
        AuthResponse response = authService.loginWithGoogle(googleUser);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        return ResponseEntity.ok(UserResponse.from(authService.getCurrentUser(authorizationHeader)));
    }

    @PatchMapping("/profile")
    public ResponseEntity<UserResponse> updateProfile(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(UserResponse.from(authService.updateProfile(authorizationHeader, request)));
    }

    @PostMapping("/change-password")
    public ResponseEntity<AuthResponse> changePassword(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @Valid @RequestBody ChangePasswordRequest request) {
        return ResponseEntity.ok(authService.changePassword(authorizationHeader, request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        authService.logout(authorizationHeader);
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        return ResponseEntity.ok(Map.of("message", "Logout successful"));
    }
}
