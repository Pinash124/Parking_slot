package com.smartparking.security;

import com.example.pricing_calculation.config.BearerTokenFilter;
import com.example.pricing_calculation.config.MutationAuditFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, BearerTokenFilter bearerTokenFilter,
                                                    MutationAuditFilter mutationAuditFilter,
                                                    ObjectProvider<ClientRegistrationRepository> clients) throws Exception {

        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/api/auth/**", "/api/parking-info/**", "/api/pricing/estimate", "/api/roles/**", "/ws/**").permitAll()
                .requestMatchers("/api/payment-gateways/*/confirm").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMINISTRATOR")
                .requestMatchers("/api/manager/**").hasAnyRole("PARKING_MANAGER", "ADMINISTRATOR")
                .requestMatchers("/api/staff/**", "/api/license-plate-scans/**").hasAnyRole("PARKING_STAFF", "PARKING_MANAGER", "ADMINISTRATOR")
                .requestMatchers("/api/user/**").hasRole("PARKING_USER")
                .requestMatchers("/api/payment-checkout/**", "/api/payments/**", "/api/reservations/**", "/api/parking-sessions/**", "/api/transaction-history/**").hasAnyRole("PARKING_STAFF", "PARKING_MANAGER", "ADMINISTRATOR")
                .requestMatchers("/api/dashboard/**").hasAnyRole("PARKING_MANAGER", "ADMINISTRATOR")
                .anyRequest().authenticated()
            )
            .addFilterBefore(bearerTokenFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(mutationAuditFilter, BearerTokenFilter.class);

        if (clients.getIfAvailable() != null) {
            http.oauth2Login(oauth -> oauth.defaultSuccessUrl("/api/auth/oauth2/success", true));
        }

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
