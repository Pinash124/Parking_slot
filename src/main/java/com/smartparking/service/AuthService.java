package com.smartparking.service;

import com.smartparking.model.requests.AuthResponse;
import com.smartparking.model.requests.ChangePasswordRequest;
import com.smartparking.model.requests.ForgotPasswordRequest;
import com.smartparking.model.requests.LoginRequest;
import com.smartparking.model.requests.OtpChallengeResponse;
import com.smartparking.model.requests.PasswordResetResponse;
import com.smartparking.model.requests.RegisterRequest;
import com.smartparking.model.requests.ResetPasswordRequest;
import com.smartparking.model.requests.UpdateProfileRequest;
import com.smartparking.model.requests.VerifyOtpRequest;
import com.smartparking.model.schemas.Role;
import com.smartparking.model.schemas.User;
import com.smartparking.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.smartparking.service.NullSafety.requireNonNull;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetEmailService passwordResetEmailService;
    private final String frontendUrl;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, PendingRegistration> pendingRegistrations = new ConcurrentHashMap<>();
    private final Map<String, OtpChallenge> otpChallenges = new ConcurrentHashMap<>();
    private final Map<String, AuthSession> sessions = new ConcurrentHashMap<>();
    private static final int OTP_MINUTES = 10;
    private static final int SESSION_HOURS = 8;

    public AuthService(UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            PasswordResetEmailService passwordResetEmailService,
            @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordResetEmailService = passwordResetEmailService;
        this.frontendUrl = frontendUrl;
    }

    public OtpChallengeResponse register(RegisterRequest request) {
        String email = requireText(request.getEmail(), "Email is required");
        String username = requireText(request.getUsername(), "Username is required");
        String fullName = requireText(request.getFullName(), "Full name is required");
        String password = requireText(request.getPassword(), "Password is required");
        removeExpiredOtpChallenges();

        userRepository.findByEmail(email).ifPresent(existing -> {
            throw new IllegalArgumentException("Email is already registered");
        });
        userRepository.findByUsername(username).ifPresent(existing -> {
            throw new IllegalArgumentException("Username is already taken");
        });

        String challengeId = UUID.randomUUID().toString();
        String otp = generateOtp();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(OTP_MINUTES);
        pendingRegistrations.put(challengeId, new PendingRegistration(
                fullName,
                username,
                email,
                blankToNull(request.getPhone()),
                passwordEncoder.encode(password),
                otp,
                expiresAt));

        boolean sent = passwordResetEmailService.sendOtp(email, "ParkingSmart - Confirm registration", otp, OTP_MINUTES);
        return new OtpChallengeResponse(
                sent ? "Registration OTP was sent to your email." : "Registration OTP created. SMTP is not configured, use otp for testing.",
                challengeId,
                email,
                expiresAt,
                sent ? null : otp);
    }

    public AuthResponse verifyRegistration(VerifyOtpRequest request) {
        removeExpiredOtpChallenges();
        String challengeId = requireText(request.getChallengeId(), "Challenge ID is required");
        String otp = requireText(request.getOtp(), "OTP is required");
        PendingRegistration pending = pendingRegistrations.get(challengeId);
        if (pending == null || pending.expiresAt().isBefore(LocalDateTime.now())
                || !pending.otp().equals(otp)) {
            pendingRegistrations.remove(challengeId);
            throw new IllegalArgumentException("Registration OTP is invalid or expired");
        }

        userRepository.findByEmail(pending.email()).ifPresent(existing -> {
            throw new IllegalArgumentException("Email is already registered");
        });
        userRepository.findByUsername(pending.username()).ifPresent(existing -> {
            throw new IllegalArgumentException("Username is already taken");
        });

        User user = new User();
        user.setFullName(pending.fullName());
        user.setUsername(pending.username());
        user.setEmail(pending.email());
        user.setPhone(pending.phone());
        user.setPasswordHash(pending.passwordHash());
        user.setStatus("ACTIVE");
        user.setRole(Role.CUSTOMER.name());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);
        pendingRegistrations.remove(challengeId);
        return toAuthResponse("Registration successful", savedUser);
    }

    public OtpChallengeResponse login(LoginRequest request) {
        String usernameOrEmail = requireText(request.getUsernameOrEmail(), "Username or email is required");
        String password = requireText(request.getPassword(), "Password is required");
        removeExpiredOtpChallenges();

        User user = userRepository.findByEmailOrUsername(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid username/email or password"));

        if (user.getPasswordHash() == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid username/email or password");
        }

        String challengeId = UUID.randomUUID().toString();
        String otp = generateOtp();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(OTP_MINUTES);
        otpChallenges.put(challengeId, new OtpChallenge("LOGIN", user.getUserId(), user.getEmail(), otp, expiresAt));

        boolean sent = passwordResetEmailService.sendOtp(user.getEmail(), "ParkingSmart - Login OTP", otp, OTP_MINUTES);
        return new OtpChallengeResponse(
                sent ? "Login OTP was sent to your email." : "Login OTP created. SMTP is not configured, use otp for testing.",
                challengeId,
                user.getEmail(),
                expiresAt,
                sent ? null : otp);
    }

    public AuthResponse verifyLogin(VerifyOtpRequest request) {
        OtpChallenge challenge = requireOtpChallenge(
                requireText(request.getChallengeId(), "Challenge ID is required"),
                requireText(request.getOtp(), "OTP is required"),
                "LOGIN");
        User user = userRepository.findById(challenge.userId())
                .orElseThrow(() -> new UsernameNotFoundException("User was not found"));
        otpChallenges.remove(request.getChallengeId());
        return toAuthResponse("Login successful", user);
    }

    public User getCurrentUser(String authorizationHeader) {
        AuthSession session = requireSession(authorizationHeader);
        return userRepository.findById(session.userId())
                .orElseThrow(() -> new UsernameNotFoundException("User was not found"));
    }

    public User updateProfile(String authorizationHeader, UpdateProfileRequest request) {
        User user = getCurrentUser(authorizationHeader);
        if (request.getEmail() != null && !request.getEmail().isBlank()
                && !request.getEmail().equalsIgnoreCase(user.getEmail())) {
            String email = request.getEmail().trim();
            userRepository.findByEmail(email).ifPresent(existing -> {
                throw new IllegalArgumentException("Email is already registered");
            });
            user.setEmail(email);
        }
        if (request.getUsername() != null && !request.getUsername().isBlank()
                && !request.getUsername().equalsIgnoreCase(user.getUsername())) {
            String username = request.getUsername().trim();
            userRepository.findByUsername(username).ifPresent(existing -> {
                throw new IllegalArgumentException("Username is already taken");
            });
            user.setUsername(username);
        }
        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName().trim());
        }
        if (request.getPhone() != null) {
            user.setPhone(blankToNull(request.getPhone()));
        }
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    public AuthResponse changePassword(String authorizationHeader, ChangePasswordRequest request) {
        User user = getCurrentUser(authorizationHeader);
        String currentPassword = requireText(request.getCurrentPassword(), "Current password is required");
        String newPassword = requireText(request.getNewPassword(), "New password is required");
        if (user.getPasswordHash() == null || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BadCredentialsException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        User savedUser = userRepository.save(user);
        return toAuthResponse("Password changed successfully", savedUser);
    }

    public PasswordResetResponse forgotPassword(ForgotPasswordRequest request) {
        String email = requireText(request.getEmail(), "Email is required");
        removeExpiredOtpChallenges();

        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isEmpty()) {
            return new PasswordResetResponse(
                    "If this email exists, a reset password OTP will be sent.",
                    null);
        }

        User user = optionalUser.get();
        Long userId = user.getUserId();
        String userEmail = user.getEmail();
        if (userId == null || userEmail == null || userEmail.isBlank()) {
            throw new IllegalArgumentException("User account is missing required information");
        }
        String challengeId = UUID.randomUUID().toString();
        String otp = generateOtp();
        LocalDateTime expiresAt = requireNonNull(LocalDateTime.now().plusMinutes(OTP_MINUTES));
        otpChallenges.put(challengeId, new OtpChallenge("RESET_PASSWORD", requireNonNull(userId), userEmail, otp, expiresAt));

        boolean emailSent = passwordResetEmailService.sendOtp(userEmail, "ParkingSmart - Reset password OTP", otp, OTP_MINUTES);
        return new PasswordResetResponse(
                emailSent
                        ? "Reset password OTP was sent to your email."
                        : "Reset password OTP created. SMTP is not configured. Use challengeId=" + challengeId + " and otp=" + otp,
                frontendUrl + "/?resetChallengeId=" + challengeId);
    }

    public AuthResponse resetPassword(ResetPasswordRequest request) {
        removeExpiredOtpChallenges();
        String email = requireText(request.getEmail(), "Email is required");
        String otp = requireText(request.getToken(), "OTP is required");
        String newPassword = requireText(request.getNewPassword(), "New password is required");

        OtpChallenge resetToken = otpChallenges.values().stream()
                .filter(challenge -> "RESET_PASSWORD".equals(challenge.type()))
                .filter(challenge -> challenge.email().equalsIgnoreCase(email))
                .filter(challenge -> challenge.otp().equals(otp))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Reset OTP is invalid or expired"));

        if (resetToken.expiresAt().isBefore(LocalDateTime.now())) {
            removeExpiredOtpChallenges();
            throw new IllegalArgumentException("Reset OTP is invalid or expired");
        }

        User user = userRepository.findById(resetToken.userId())
                .orElseThrow(() -> new IllegalArgumentException("User was not found"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        User savedUser = userRepository.save(user);
        otpChallenges.entrySet().removeIf(entry -> entry.getValue().equals(resetToken));

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
            newUser.setUsername(generateGoogleUsername(requireNonNull(normalizedEmail)));
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

    private OtpChallenge requireOtpChallenge(String challengeId, String otp, String type) {
        removeExpiredOtpChallenges();
        OtpChallenge challenge = otpChallenges.get(challengeId);
        if (challenge == null || !type.equals(challenge.type()) || challenge.expiresAt().isBefore(LocalDateTime.now())
                || !challenge.otp().equals(otp)) {
            otpChallenges.remove(challengeId);
            throw new IllegalArgumentException("OTP is invalid or expired");
        }
        return challenge;
    }

    private String generateOtp() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }

    private void removeExpiredOtpChallenges() {
        LocalDateTime now = LocalDateTime.now();
        pendingRegistrations.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
        otpChallenges.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    private AuthResponse toAuthResponse(String message, User user) {
        Role role = Role.from(user.getRole());
        if (!role.name().equals(user.getRole())) {
            user.setRole(role.name());
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
        }
        return new AuthResponse(
                message,
                issueToken(user),
                user.getUserId() == null ? null : String.valueOf(user.getUserId()),
                user.getFullName(),
                user.getEmail(),
                role.name());
    }

    public void logout(String authorizationHeader) {
        String token = extractBearerToken(authorizationHeader);
        sessions.remove(token);
    }

    private String issueToken(User user) {
        Long userId = user.getUserId();
        if (userId == null) {
            return null;
        }
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sessions.put(token, new AuthSession(userId, LocalDateTime.now().plusHours(SESSION_HOURS)));
        return token;
    }

    private AuthSession requireSession(String authorizationHeader) {
        removeExpiredOtpChallenges();
        sessions.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(LocalDateTime.now()));
        String token = extractBearerToken(authorizationHeader);
        AuthSession session = sessions.get(token);
        if (session == null || session.expiresAt().isBefore(LocalDateTime.now())) {
            sessions.remove(token);
            throw new BadCredentialsException("Invalid or expired access token");
        }
        return session;
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            throw new BadCredentialsException("Bearer access token is required");
        }
        String token = authorizationHeader.substring(7).trim();
        if (token.isBlank()) {
            throw new BadCredentialsException("Bearer access token is required");
        }
        return token;
    }

    private String requireText(@Nullable String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return requireNonNull(value.trim());
    }

    @Nullable
    private String blankToNull(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private record PendingRegistration(
            String fullName,
            String username,
            String email,
            @Nullable String phone,
            String passwordHash,
            String otp,
            LocalDateTime expiresAt) {
    }

    private record OtpChallenge(String type, long userId, String email, String otp, LocalDateTime expiresAt) {
    }

    private record AuthSession(long userId, LocalDateTime expiresAt) {
    }
}
