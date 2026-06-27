package com.example.pricing_calculation.config;

import com.example.pricing_calculation.domain.UserAccount;
import com.example.pricing_calculation.domain.UserRole;
import com.example.pricing_calculation.repository.UserAccountRepository;
import com.example.pricing_calculation.service.PasswordHashService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class BootstrapAdminInitializer implements CommandLineRunner {
    private final UserAccountRepository users; private final PasswordHashService passwords;
    private final String email; private final String password;
    public BootstrapAdminInitializer(UserAccountRepository users,PasswordHashService passwords,
            @Value("${parking.bootstrap.admin-email:admin@smartparking.local}")String email,
            @Value("${parking.bootstrap.admin-password:Admin@12345}")String password){this.users=users;this.passwords=passwords;this.email=email;this.password=password;}
    @Override public void run(String...args){if(users.count()==0){UserAccount admin=new UserAccount();admin.setFullName("System Administrator");admin.setEmail(email);admin.setPasswordHash(passwords.hash(password));admin.setStatus("ACTIVE");admin.setRole(UserRole.ADMINISTRATOR.code());users.save(admin);}}
}
