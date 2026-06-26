package com.example.pricing_calculation.authtest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

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

    AuthFlowResult run(String email, String password) {
        long startedAt = System.nanoTime();
        int registrationStatus = 0;
        int invalidLoginStatus = 0;
        int loginStatus = 0;
        int logoutStatus = 0;
        int reusedTokenStatus = 0;
        try {
            HttpResponse<String> registration = post("/api/auth/register", Map.of(
                    "fullName", "Auth Test Bot",
                    "email", email,
                    "phone", "0900000000",
                    "password", password
            ), null);
            registrationStatus = registration.statusCode();
            requireStatus(registration, 201);
            JsonNode registeredUser = objectMapper.readTree(registration.body());
            requireText(registeredUser, "email", email);
            requireText(registeredUser, "status", "ACTIVE");
            requireText(registeredUser, "role", "PARKING_USER");
            if (registeredUser.has("password") || registeredUser.has("passwordHash")) {
                throw new IllegalStateException("Registration response exposed password data");
            }

            HttpResponse<String> invalidLogin = post("/api/auth/login", Map.of(
                    "email", email,
                    "password", password + "-wrong"
            ), null);
            invalidLoginStatus = invalidLogin.statusCode();
            requireStatus(invalidLogin, 401);

            HttpResponse<String> login = post("/api/auth/login", Map.of(
                    "email", email.toUpperCase(),
                    "password", password
            ), null);
            loginStatus = login.statusCode();
            requireStatus(login, 200);
            JsonNode loginBody = objectMapper.readTree(login.body());
            requireText(loginBody, "tokenType", "Bearer");
            requireText(loginBody, "email", email);
            requireText(loginBody, "role", "PARKING_USER");
            String accessToken = loginBody.path("accessToken").stringValue("");
            if (accessToken.isBlank() || loginBody.path("expiresAt").isMissingNode()) {
                throw new IllegalStateException("Login did not return a valid access token");
            }

            HttpResponse<String> logout = post("/api/auth/logout", Map.of(), accessToken);
            logoutStatus = logout.statusCode();
            requireStatus(logout, 200);
            requireText(objectMapper.readTree(logout.body()), "message", "Logout completed");

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
        String actual = node.path(field).stringValue("");
        if (!expected.equals(actual)) {
            throw new IllegalStateException(field + " expected " + expected + " but was " + actual);
        }
    }

    private long elapsedMillis(long startedAt) {
        return Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
    }
}
