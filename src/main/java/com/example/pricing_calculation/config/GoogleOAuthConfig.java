package com.example.pricing_calculation.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

@Configuration
@ConditionalOnExpression("'${google.oauth.client-id:}' != ''")
public class GoogleOAuthConfig {
    @Bean
    ClientRegistrationRepository clientRegistrationRepository(
            @Value("${google.oauth.client-id}") String clientId,
            @Value("${google.oauth.client-secret}") String clientSecret) {
        ClientRegistration google=CommonOAuth2Provider.GOOGLE.getBuilder("google")
                .clientId(clientId).clientSecret(clientSecret)
                .scope("openid","email","profile")
                .build();
        return new InMemoryClientRegistrationRepository(google);
    }
}
