package com.smartparking.service;

import com.smartparking.model.requests.AuthResponse;
import com.smartparking.model.requests.LoginRequest;
import com.smartparking.model.requests.RegisterRequest;
import com.smartparking.model.schemas.User;
import com.smartparking.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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
}
