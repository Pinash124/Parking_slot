package com.example.pricing_calculation.service;

import com.example.pricing_calculation.domain.UserAccount;
import com.example.pricing_calculation.dto.AuthLoginRequest;
import com.example.pricing_calculation.dto.AuthLoginResponse;
import com.example.pricing_calculation.dto.AuthLogoutResponse;
import com.example.pricing_calculation.dto.AuthRegistrationRequest;
import com.example.pricing_calculation.dto.AuthRegistrationResponse;
import com.example.pricing_calculation.repository.UserAccountRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final int TOKEN_BYTES = 32;
    private static final int SESSION_HOURS = 8;

    private final UserAccountRepository userAccountRepository;
    private final PasswordHashService passwordHashService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, AuthSession> sessions = new ConcurrentHashMap<>();

    public AuthService(
            UserAccountRepository userAccountRepository,
            PasswordHashService passwordHashService) {
        this.userAccountRepository = userAccountRepository;
        this.passwordHashService = passwordHashService;
    }

    @Transactional
    public AuthRegistrationResponse register(AuthRegistrationRequest request) {
        validateRegistration(request);
        String email = normalizeEmail(request.email());
        if (userAccountRepository.existsByEmailIgnoreCase(email)) {
            throw new BadRequestException("Email is already registered");
        }

        UserAccount user = new UserAccount();
        user.setFullName(request.fullName());
        user.setEmail(email);
        user.setPhone(request.phone());
        user.setPasswordHash(passwordHashService.hash(request.password()));
        user.setStatus("ACTIVE");
        user.setRole("CUSTOMER");
        UserAccount saved = userAccountRepository.save(user);

        return new AuthRegistrationResponse(
                saved.getId(),
                saved.getFullName(),
                saved.getEmail(),
                saved.getPhone(),
                saved.getStatus(),
                saved.getRole(),
                "Registration completed"
        );
    }

    @Transactional(readOnly = true)
    public AuthLoginResponse login(AuthLoginRequest request) {
        if (request == null || request.email() == null || request.email().isBlank()
                || request.password() == null || request.password().isBlank()) {
            throw new UnauthorizedException("Invalid email or password");
        }
        UserAccount user = userAccountRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())
                || !passwordHashService.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        purgeExpiredSessions();
        String rawToken = generateToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(SESSION_HOURS);
        sessions.put(hashToken(rawToken), new AuthSession(user.getId(), expiresAt));
        return new AuthLoginResponse(
                rawToken,
                "Bearer",
                expiresAt,
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole()
        );
    }

    public AuthLogoutResponse logout(String authorizationHeader) {
        String token = extractBearerToken(authorizationHeader);
        AuthSession removed = sessions.remove(hashToken(token));
        if (removed == null || removed.expiresAt().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedException("Invalid or expired access token");
        }
        return new AuthLogoutResponse("Logout completed");
    }

    private void validateRegistration(AuthRegistrationRequest request) {
        if (request == null) {
            throw new BadRequestException("Registration request is required");
        }
        if (request.fullName() == null || request.fullName().trim().length() < 2
                || request.fullName().trim().length() > 100) {
            throw new BadRequestException("fullName must contain between 2 and 100 characters");
        }
        String email = normalizeEmail(request.email());
        if (email.length() > 100 || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new BadRequestException("A valid email is required");
        }
        if (request.phone() != null && request.phone().trim().length() > 20) {
            throw new BadRequestException("phone must not exceed 20 characters");
        }
        if (request.password() == null || request.password().length() < 8
                || request.password().length() > 128) {
            throw new BadRequestException("password must contain between 8 and 128 characters");
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String generateToken() {
        byte[] token = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(token);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(token);
    }

    private String hashToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Token hashing is not available", exception);
        }
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            throw new UnauthorizedException("Bearer access token is required");
        }
        String token = authorizationHeader.substring(7).trim();
        if (token.isEmpty()) {
            throw new UnauthorizedException("Bearer access token is required");
        }
        return token;
    }

    private void purgeExpiredSessions() {
        LocalDateTime now = LocalDateTime.now();
        sessions.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    private record AuthSession(Long userId, LocalDateTime expiresAt) {
    }
}
