package com.smartparking.service;

import com.smartparking.model.requests.AuthResponse;
import com.smartparking.model.requests.ForgotPasswordRequest;
import com.smartparking.model.requests.LoginRequest;
import com.smartparking.model.requests.PasswordResetResponse;
import com.smartparking.model.requests.RegisterRequest;
import com.smartparking.model.requests.ResetPasswordRequest;
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
import java.util.Objects;
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
        String email = request.getEmail().trim();
        String username = request.getUsername().trim();

        userRepository.findByEmail(email).ifPresent(existing -> {
            throw new IllegalArgumentException("Email is already registered");
        });
        userRepository.findByUsername(username).ifPresent(existing -> {
            throw new IllegalArgumentException("Username is already taken");
        });

        User user = new User();
        user.setFullName(request.getFullName().trim());
        user.setUsername(username);
        user.setEmail(email);
        user.setPhone(request.getPhone() == null ? null : request.getPhone().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setStatus("ACTIVE");
        user.setRole("USER");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);
        return new AuthResponse("Registration successful", savedUser.getEmail(), savedUser.getUsername());
    }

    public AuthResponse login(LoginRequest request) {
        String usernameOrEmail = request.getUsernameOrEmail().trim();

        User user = userRepository.findByEmailOrUsername(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid username/email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid username/email or password");
        }

        return new AuthResponse("Login successful", user.getEmail(), user.getUsername());
    }

    public PasswordResetResponse forgotPassword(ForgotPasswordRequest request) {
        String email = Objects.requireNonNull(request.getEmail(), "Email is required").trim();
        removeExpiredResetTokens();

        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isEmpty()) {
            return new PasswordResetResponse(
                    "If this email exists, a reset password link will be sent.",
                    null
            );
        }

        User user = optionalUser.get();
        long userId = Objects.requireNonNull(user.getUserId(), "User id is required");
        String userEmail = Objects.requireNonNull(user.getEmail(), "User email is required");
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
        removeExpiredResetTokens();

        PasswordResetToken resetToken = resetTokens.get(request.getToken());
        if (resetToken == null || resetToken.expiresAt().isBefore(LocalDateTime.now())) {
            resetTokens.remove(request.getToken());
            throw new IllegalArgumentException("Reset link is invalid or expired");
        }

        User user = userRepository.findById(resetToken.userId())
                .orElseThrow(() -> new IllegalArgumentException("User was not found"));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        User savedUser = userRepository.save(user);
        resetTokens.remove(request.getToken());

        return new AuthResponse("Password reset successful", savedUser.getEmail(), savedUser.getUsername());
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
            newUser.setRole("USER");
            newUser.setCreatedAt(now);
            return newUser;
        });

        if (fullName != null && !fullName.isBlank()) {
            user.setFullName(fullName.trim());
        }
        user.setUpdatedAt(now);

        User savedUser = userRepository.save(user);
        return new AuthResponse("Google login successful", savedUser.getEmail(), savedUser.getUsername());
    }

    private String generateGoogleUsername(String email) {
        String baseUsername = email.substring(0, email.indexOf("@"))
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

    private record PasswordResetToken(long userId, LocalDateTime expiresAt) {
    }
}
