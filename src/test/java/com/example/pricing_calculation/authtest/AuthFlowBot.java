package com.example.pricing_calculation.authtest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.function.Function;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

final class AuthFlowBot {

    private final String baseUrl;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    AuthFlowBot(String baseUrl, ObjectMapper objectMapper) {
        this.baseUrl = baseUrl;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    AuthFlowResult run(String email, String password, Function<String, String> otpSupplier) {
        long startedAt = System.nanoTime();
        int registrationStatus = 0;
        int invalidLoginStatus = 0;
        int loginStatus = 0;
        int logoutStatus = 0;
        int reusedTokenStatus = 0;
        try {
            // 1. Register - Triggers OTP
            HttpResponse<String> registration = post("/api/auth/register", Map.of(
                    "fullName", "Auth Test Bot",
                    "email", email,
                    "phone", "0900000000",
                    "password", password
            ), null);
            registrationStatus = registration.statusCode();
            requireStatus(registration, 200);
            requireText(objectMapper.readTree(registration.body()), "email", email);

            // 2. Verify Registration OTP via unified verify-otp endpoint
            String regOtp = otpSupplier.apply(email);
            if (regOtp == null) {
                throw new IllegalStateException("Could not retrieve registration OTP");
            }
            HttpResponse<String> verifyRegister = post("/api/auth/verify-otp", Map.of(
                    "email", email,
                    "otp", regOtp
            ), null);
            requireStatus(verifyRegister, 200);
            JsonNode registeredUser = objectMapper.readTree(verifyRegister.body());
            requireText(registeredUser, "email", email);
            requireText(registeredUser, "tokenType", "Bearer");
            
            // Re-login after registration is not strictly needed since registration logs them in,
            // but we can test invalid login and correct login flow next.

            // 3. Login with wrong password (should not trigger OTP, returns 401)
            HttpResponse<String> invalidLogin = post("/api/auth/login", Map.of(
                    "email", email,
                    "password", password + "-wrong"
            ), null);
            invalidLoginStatus = invalidLogin.statusCode();
            requireStatus(invalidLogin, 401);

            // 4. Login with correct password - Triggers OTP
            HttpResponse<String> login = post("/api/auth/login", Map.of(
                    "email", email.toUpperCase(),
                    "password", password
            ), null);
            loginStatus = login.statusCode();
            requireStatus(login, 200);
            requireText(objectMapper.readTree(login.body()), "email", email);

            // 5. Verify Login OTP via unified verify-otp endpoint
            String loginOtp = otpSupplier.apply(email);
            if (loginOtp == null) {
                throw new IllegalStateException("Could not retrieve login OTP");
            }
            HttpResponse<String> verifyLogin = post("/api/auth/verify-otp", Map.of(
                    "email", email,
                    "otp", loginOtp
            ), null);
            requireStatus(verifyLogin, 200);
            JsonNode loginBody = objectMapper.readTree(verifyLogin.body());
            requireText(loginBody, "tokenType", "Bearer");
            requireText(loginBody, "email", email);
            String accessToken = loginBody.path("accessToken").asText("");
            if (accessToken.isBlank() || loginBody.path("expiresAt").isMissingNode()) {
                throw new IllegalStateException("Login did not return a valid access token");
            }

            // 6. Logout
            HttpResponse<String> logout = post("/api/auth/logout", Map.of(), accessToken);
            logoutStatus = logout.statusCode();
            requireStatus(logout, 200);
            requireText(objectMapper.readTree(logout.body()), "message", "Logout completed");

            // 7. Reuse Token
            HttpResponse<String> reuseToken = post("/api/auth/logout", Map.of(), accessToken);
            reusedTokenStatus = reuseToken.statusCode();
            requireStatus(reuseToken, 401);

            return new AuthFlowResult(
                    email,
                    true,
                    elapsedMillis(startedAt),
                    registrationStatus,
                    invalidLoginStatus,
                    loginStatus,
                    logoutStatus,
                    reusedTokenStatus,
                    null
            );
        } catch (Exception exception) {
            return new AuthFlowResult(
                    email,
                    false,
                    elapsedMillis(startedAt),
                    registrationStatus,
                    invalidLoginStatus,
                    loginStatus,
                    logoutStatus,
                    reusedTokenStatus,
                    exception.getMessage()
            );
        }
    }

    private HttpResponse<String> post(String path, Object body, String accessToken) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json");
        if (accessToken != null) {
            request.header("Authorization", "Bearer " + accessToken);
        }
        return httpClient.send(
                request.POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body))).build(),
                HttpResponse.BodyHandlers.ofString()
        );
    }

    private void requireStatus(HttpResponse<String> response, int expected) {
        if (response.statusCode() != expected) {
            throw new IllegalStateException(
                    response.request().method() + " " + response.request().uri().getPath()
                            + " expected HTTP " + expected + " but returned "
                            + response.statusCode() + ": " + response.body()
            );
        }
    }

    private void requireText(JsonNode node, String field, String expected) {
        String actual = node.path(field).asText("");
        if (!expected.equals(actual)) {
            throw new IllegalStateException(field + " expected " + expected + " but was " + actual);
        }
    }

    private long elapsedMillis(long startedAt) {
        return Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
    }
}
