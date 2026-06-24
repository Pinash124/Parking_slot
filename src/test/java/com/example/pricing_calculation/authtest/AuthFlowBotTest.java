package com.example.pricing_calculation.authtest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.pricing_calculation.repository.UserAccountRepository;
import com.example.pricing_calculation.service.AuthService;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:authbot;MODE=MSSQLServer;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.show-sql=false"
        }
)
class AuthFlowBotTest {

    private static final String PASSWORD = "Auth-bot-password-123!";

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private AuthService authService;

    private String email;

    @BeforeEach
    void setUp() {
        email = "auth-bot-" + UUID.randomUUID() + "@example.com";
    }

    @AfterEach
    void cleanUp() {
        userAccountRepository.findByEmailIgnoreCase(email).ifPresent(userAccountRepository::delete);
        assertFalse(userAccountRepository.existsByEmailIgnoreCase(email), "Auth bot data must be cleaned up");
    }

    @Test
    void botCompletesRegistrationLoginAndLogoutFlow() {
        AuthFlowBot bot = new AuthFlowBot("http://localhost:" + port, objectMapper);

        AuthFlowResult result = bot.run(email, PASSWORD, e -> authService.getPendingOtpForTesting(e));

        assertTrue(result.successful(), () -> "Auth bot failed: " + result.error());
        assertTrue(result.durationMillis() < 20000, "Auth flow should finish within 20 seconds");
        System.out.printf(
                "[auth-bot] result=PASS durationMs=%d register=%d invalidLogin=%d login=%d logout=%d reusedToken=%d%n",
                result.durationMillis(),
                result.registrationStatus(),
                result.invalidLoginStatus(),
                result.loginStatus(),
                result.logoutStatus(),
                result.reusedTokenStatus()
        );
    }
}
