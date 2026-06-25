package com.example.pricing_calculation.config;

import com.example.pricing_calculation.domain.UserAccount;
import com.example.pricing_calculation.repository.UserAccountRepository;
import com.example.pricing_calculation.service.AuthService;
import com.example.pricing_calculation.service.PasswordHashService;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminBootstrapConfig implements ApplicationRunner {

    private final UserAccountRepository userAccountRepository;
    private final PasswordHashService passwordHashService;
    private final AuthService authService;
    private final String email;
    private final String password;
    private final String fullName;

    public AdminBootstrapConfig(
            UserAccountRepository userAccountRepository,
            PasswordHashService passwordHashService,
            AuthService authService,
            @Value("${app.bootstrap-admin.email:}") String email,
            @Value("${app.bootstrap-admin.password:}") String password,
            @Value("${app.bootstrap-admin.full-name:System Administrator}") String fullName) {
        this.userAccountRepository = userAccountRepository;
        this.passwordHashService = passwordHashService;
        this.authService = authService;
        this.email = email;
        this.password = password;
        this.fullName = fullName;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return;
        }

        authService.validatePasswordForAdministration(password);
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        UserAccount admin = userAccountRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseGet(UserAccount::new);
        if (admin.getId() == null) {
            admin.setEmail(normalizedEmail);
            admin.setFullName(fullName == null || fullName.isBlank()
                    ? "System Administrator"
                    : fullName.trim());
        }
        admin.setPasswordHash(passwordHashService.hash(password));
        admin.setStatus("ACTIVE");
        admin.setRole("ADMIN");
        userAccountRepository.save(admin);
    }
}
