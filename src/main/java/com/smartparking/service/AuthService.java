package com.smartparking.service;

import com.smartparking.model.requests.AuthResponse;
import com.smartparking.model.requests.ForgotPasswordRequest;
import com.smartparking.model.requests.LoginRequest;
import com.smartparking.model.requests.PasswordResetResponse;
import com.smartparking.model.requests.RegisterRequest;
import com.smartparking.model.requests.ResetPasswordRequest;
import com.smartparking.model.schemas.Role;
import com.smartparking.model.schemas.User;
import com.smartparking.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetEmailService passwordResetEmailService;
    private final String frontendUrl;
    private final Map<String, PasswordResetToken> resetTokens = new ConcurrentHashMap<>();

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       PasswordResetEmailService passwordResetEmailService,
                       @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordResetEmailService = passwordResetEmailService;
        this.frontendUrl = frontendUrl;
    }

    public AuthResponse register(RegisterRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Registration information is required");
        }

        String email = requireText(request.getEmail(), "Email is required");
        String username = requireText(request.getUsername(), "Username is required");
        String fullName = requireText(request.getFullName(), "Full name is required");
        String password = requireText(request.getPassword(), "Password is required");

        userRepository.findByEmail(email).ifPresent(existing -> {
            throw new IllegalArgumentException("Email is already registered");
        });
        userRepository.findByUsername(username).ifPresent(existing -> {
            throw new IllegalArgumentException("Username is already taken");
        });

        User user = new User();
        user.setFullName(fullName);
        user.setUsername(username);
        user.setEmail(email);
        user.setPhone(blankToNull(request.getPhone()));
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setStatus("ACTIVE");
        user.setRole(Role.CUSTOMER.name());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);
        return toAuthResponse("Registration successful", savedUser);
    }

    public AuthResponse login(LoginRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Login information is required");
        }

        String usernameOrEmail = requireText(request.getUsernameOrEmail(), "Username or email is required");
        String password = requireText(request.getPassword(), "Password is required");

        User user = userRepository.findByEmailOrUsername(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid username/email or password"));

        if (user.getPasswordHash() == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid username/email or password");
        }

        return toAuthResponse("Login successful", user);
    }

    public PasswordResetResponse forgotPassword(ForgotPasswordRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Email is required");
        }

        String email = requireText(request.getEmail(), "Email is required");
        removeExpiredResetTokens();

        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isEmpty()) {
            return new PasswordResetResponse(
                    "If this email exists, a reset password link will be sent.",
                    null
            );
        }

        User user = optionalUser.get();
        Long userId = user.getUserId();
        String userEmail = user.getEmail();
        if (userId == null || userEmail == null || userEmail.isBlank()) {
            throw new IllegalArgumentException("User account is missing required information");
        }
        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(30);
        resetTokens.put(token, new PasswordResetToken(userId, expiresAt));

        String resetLink = frontendUrl + "/?resetToken=" + token;
        boolean emailSent = passwordResetEmailService.sendResetLink(userEmail, resetLink);
        return new PasswordResetResponse(
                emailSent
                        ? "Reset password link was sent to your email."
                        : "Reset password link created. SMTP is not configured, use resetLink for testing.",
                resetLink
        );
    }

    public AuthResponse resetPassword(ResetPasswordRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Reset password information is required");
        }

        removeExpiredResetTokens();
        String token = requireText(request.getToken(), "Reset token is required");
        String newPassword = requireText(request.getNewPassword(), "New password is required");

        PasswordResetToken resetToken = resetTokens.get(token);
        if (resetToken == null || resetToken.expiresAt().isBefore(LocalDateTime.now())) {
            resetTokens.remove(token);
            throw new IllegalArgumentException("Reset link is invalid or expired");
        }

        User user = userRepository.findById(resetToken.userId())
                .orElseThrow(() -> new IllegalArgumentException("User was not found"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        User savedUser = userRepository.save(user);
        resetTokens.remove(token);

        return toAuthResponse("Password reset successful", savedUser);
    }

    public AuthResponse loginWithGoogle(OAuth2User googleUser) {
        String email = googleUser.getAttribute("email");
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Google account does not provide an email");
        }

        String normalizedEmail = email.trim();
        String fullName = googleUser.getAttribute("name");
        LocalDateTime now = LocalDateTime.now();

        User user = userRepository.findByEmail(normalizedEmail).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(normalizedEmail);
            newUser.setUsername(generateGoogleUsername(normalizedEmail));
            newUser.setPasswordHash(passwordEncoder.encode("GOOGLE_LOGIN:" + UUID.randomUUID()));
            newUser.setStatus("ACTIVE");
            newUser.setRole(Role.CUSTOMER.name());
            newUser.setCreatedAt(now);
            return newUser;
        });

        if (fullName != null && !fullName.isBlank()) {
            user.setFullName(fullName.trim());
        }
        user.setRole(Role.from(user.getRole()).name());
        user.setUpdatedAt(now);

        User savedUser = userRepository.save(user);
        return toAuthResponse("Google login successful", savedUser);
    }

    private String generateGoogleUsername(String email) {
        int atIndex = email.indexOf("@");
        String base = atIndex > 0 ? email.substring(0, atIndex) : email;
        String baseUsername = base
                .replaceAll("[^a-zA-Z0-9_]", "")
                .toLowerCase();
        if (baseUsername.isBlank()) {
            baseUsername = "google_user";
        }

        String username = baseUsername;
        int suffix = 1;
        while (userRepository.findByUsername(username).isPresent()) {
            username = baseUsername + suffix;
            suffix++;
        }
        return username;
    }

    private void removeExpiredResetTokens() {
        LocalDateTime now = LocalDateTime.now();
        resetTokens.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    private AuthResponse toAuthResponse(String message, User user) {
        Role role = Role.from(user.getRole());
        if (!role.name().equals(user.getRole())) {
            user.setRole(role.name());
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
        }
        return new AuthResponse(message, user.getEmail(), user.getUsername(), role.name());
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private record PasswordResetToken(long userId, LocalDateTime expiresAt) {
    }
}
