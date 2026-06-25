package com.example.pricing_calculation.service;

import com.example.pricing_calculation.domain.UserAccount;
import com.example.pricing_calculation.dto.AuthLoginRequest;
import com.example.pricing_calculation.dto.AuthLoginResponse;
import com.example.pricing_calculation.dto.AuthLogoutResponse;
import com.example.pricing_calculation.dto.AuthRegistrationRequest;
import com.example.pricing_calculation.dto.ChangePasswordRequest;
import com.example.pricing_calculation.dto.VerifyOtpRequest;
import com.example.pricing_calculation.dto.OtpResponse;
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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final int TOKEN_BYTES = 32;
    private static final int SESSION_HOURS = 8;

    private final UserAccountRepository userAccountRepository;
    private final PasswordHashService passwordHashService;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String fromAddress;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, AuthSession> sessions = new ConcurrentHashMap<>();

    private record OtpCacheEntry(
            String type, // "REGISTER" or "LOGIN"
            String email,
            String otp,
            LocalDateTime expiresAt,
            String fullName,
            String phone,
            String passwordHash,
            Long userId
    ) {}

    private final Map<String, OtpCacheEntry> otpStorage = new ConcurrentHashMap<>();

    @Autowired
    public AuthService(
            UserAccountRepository userAccountRepository,
            PasswordHashService passwordHashService,
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${spring.mail.username:}") String fromAddress) {
        this.userAccountRepository = userAccountRepository;
        this.passwordHashService = passwordHashService;
        this.mailSenderProvider = mailSenderProvider;
        this.fromAddress = fromAddress;
    }

    // Overloaded constructor for tests / backwards compatibility
    public AuthService(
            UserAccountRepository userAccountRepository,
            PasswordHashService passwordHashService) {
        this(userAccountRepository, passwordHashService, null, "");
    }

    @Transactional
    public OtpResponse register(AuthRegistrationRequest request) {
        validateRegistration(request);
        String email = normalizeEmail(request.email());
        if (userAccountRepository.existsByEmailIgnoreCase(email)) {
            throw new BadRequestException("Email is already registered");
        }

        String otp = generateOtp();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(5);

        otpStorage.put(email, new OtpCacheEntry(
                "REGISTER",
                email,
                otp,
                expiresAt,
                request.fullName(),
                request.phone(),
                passwordHashService.hash(request.password()),
                null
        ));

        String subject = "ParkingSmart - Confirm Registration OTP";
        String body = "Your registration OTP code is: " + otp + "\nIt will expire in 5 minutes.";
        sendEmail(email, subject, body, otp);

        return new OtpResponse(email, "OTP sent to your email. Please verify to complete registration.");
    }

    @Transactional(readOnly = true)
    public OtpResponse login(AuthLoginRequest request) {
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

        String otp = generateOtp();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(5);

        otpStorage.put(normalizeEmail(request.email()), new OtpCacheEntry(
                "LOGIN",
                normalizeEmail(request.email()),
                otp,
                expiresAt,
                null,
                null,
                null,
                user.getId()
        ));

        String subject = "ParkingSmart - Confirm Login OTP";
        String body = "Your login OTP code is: " + otp + "\nIt will expire in 5 minutes.";
        sendEmail(normalizeEmail(request.email()), subject, body, otp);

        return new OtpResponse(normalizeEmail(request.email()), "OTP sent to your email. Please verify to complete login.");
    }

    @Transactional
    public AuthLoginResponse verifyOtp(VerifyOtpRequest request) {
        if (request == null || request.email() == null || request.otp() == null) {
            throw new BadRequestException("Email and OTP are required");
        }
        String email = normalizeEmail(request.email());
        OtpCacheEntry entry = otpStorage.get(email);
        if (entry == null || entry.expiresAt().isBefore(LocalDateTime.now())) {
            otpStorage.remove(email);
            throw new BadRequestException("OTP is invalid or has expired");
        }

        if (!entry.otp().equals(request.otp().trim())) {
            throw new BadRequestException("Incorrect OTP code");
        }

        UserAccount user;
        if ("REGISTER".equals(entry.type())) {
            if (userAccountRepository.existsByEmailIgnoreCase(email)) {
                throw new BadRequestException("Email is already registered");
            }

            user = new UserAccount();
            user.setFullName(entry.fullName());
            user.setEmail(email);
            user.setPhone(entry.phone());
            user.setPasswordHash(entry.passwordHash());
            user.setStatus("ACTIVE");
            user.setRole("CUSTOMER");
            user = userAccountRepository.save(user);
        } else if ("CHANGE_PASSWORD".equals(entry.type())) {
            user = userAccountRepository.findById(entry.userId())
                    .orElseThrow(() -> new UnauthorizedException("User no longer exists"));
            user.setPasswordHash(entry.passwordHash());
            UserAccount saved = userAccountRepository.save(user);
            if (saved != null) {
                user = saved;
            }
        } else {
            user = userAccountRepository.findById(entry.userId())
                    .orElseThrow(() -> new UnauthorizedException("User no longer exists"));
        }

        purgeExpiredSessions();
        String rawToken = generateToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(SESSION_HOURS);
        sessions.put(hashToken(rawToken), new AuthSession(user.getId(), expiresAt));

        otpStorage.remove(email);

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

    @Transactional
    public OtpResponse changePassword(String authorizationHeader, ChangePasswordRequest request) {
        if (request == null || request.oldPassword() == null || request.newPassword() == null) {
            throw new BadRequestException("Old and new passwords are required");
        }
        validatePasswordStrength(request.newPassword());

        String token = extractBearerToken(authorizationHeader);
        AuthSession session = sessions.get(hashToken(token));
        if (session == null || session.expiresAt().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedException("Invalid or expired access token");
        }

        UserAccount user = userAccountRepository.findById(session.userId())
                .orElseThrow(() -> new UnauthorizedException("User no longer exists"));

        if (!passwordHashService.matches(request.oldPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Incorrect old password");
        }

        String email = normalizeEmail(user.getEmail());
        String otp = generateOtp();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(5);

        otpStorage.put(email, new OtpCacheEntry(
                "CHANGE_PASSWORD",
                email,
                otp,
                expiresAt,
                null,
                null,
                passwordHashService.hash(request.newPassword()),
                user.getId()
        ));

        String subject = "ParkingSmart - Confirm Password Change OTP";
        String body = "Your password change OTP code is: " + otp + "\nIt will expire in 5 minutes.";
        sendEmail(email, subject, body, otp);

        return new OtpResponse(email, "OTP sent to your email. Please verify to complete password change.");
    }

    public AuthLogoutResponse logout(String authorizationHeader) {
        String token = extractBearerToken(authorizationHeader);
        AuthSession removed = sessions.remove(hashToken(token));
        if (removed == null || removed.expiresAt().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedException("Invalid or expired access token");
        }
        return new AuthLogoutResponse("Logout completed");
    }

    @Transactional(readOnly = true)
    public UserAccount requireUser(String authorizationHeader) {
        String token = extractBearerToken(authorizationHeader);
        AuthSession session = sessions.get(hashToken(token));
        if (session == null || session.expiresAt().isBefore(LocalDateTime.now())) {
            if (session != null) {
                sessions.remove(hashToken(token));
            }
            throw new UnauthorizedException("Invalid or expired access token");
        }

        UserAccount user = userAccountRepository.findById(session.userId())
                .orElseThrow(() -> new UnauthorizedException("User no longer exists"));
        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            revokeSessions(user.getId());
            throw new UnauthorizedException("User account is not active");
        }
        return user;
    }

    @Transactional(readOnly = true)
    public UserAccount requireRole(String authorizationHeader, String requiredRole) {
        UserAccount user = requireUser(authorizationHeader);
        if (user.getRole() == null || !requiredRole.equalsIgnoreCase(user.getRole())) {
            throw new ForbiddenException(requiredRole + " role is required");
        }
        return user;
    }

    public void revokeSessions(Long userId) {
        if (userId != null) {
            sessions.entrySet().removeIf(entry -> userId.equals(entry.getValue().userId()));
        }
    }

    public void validatePasswordForAdministration(String password) {
        validatePasswordStrength(password);
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
        validatePasswordStrength(request.password());
    }

    private void validatePasswordStrength(String password) {
        if (password == null || password.length() < 8 || password.length() > 128) {
            throw new BadRequestException("Password must contain between 8 and 128 characters");
        }
        boolean hasUppercase = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;
        boolean hasLetter = false;

        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (Character.isUpperCase(c)) {
                hasUppercase = true;
            }
            if (Character.isDigit(c)) {
                hasDigit = true;
            }
            if (Character.isLetter(c)) {
                hasLetter = true;
            }
            if (!Character.isLetterOrDigit(c)) {
                hasSpecial = true;
            }
        }

        if (!hasUppercase) {
            throw new BadRequestException("Password must contain at least one uppercase letter");
        }
        if (!hasLetter) {
            throw new BadRequestException("Password must contain at least one letter");
        }
        if (!hasDigit) {
            throw new BadRequestException("Password must contain at least one number");
        }
        if (!hasSpecial) {
            throw new BadRequestException("Password must contain at least one special character");
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String generateOtp() {
        int code = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(code);
    }

    private void sendEmail(String toEmail, String subject, String body, String otpCode) {
        System.out.println("[OTP-SERVICE] Generated OTP for " + toEmail + ": " + otpCode);
        if (mailSenderProvider == null) {
            return;
        }
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null || fromAddress == null || fromAddress.isBlank()) {
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception ex) {
            System.err.println("[OTP-SERVICE] Failed to send email to " + toEmail + ": " + ex.getMessage());
        }
    }

    public String getPendingOtpForTesting(String email) {
        OtpCacheEntry entry = otpStorage.get(normalizeEmail(email));
        return entry == null ? null : entry.otp();
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
