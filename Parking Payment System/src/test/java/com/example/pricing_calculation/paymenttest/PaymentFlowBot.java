package com.example.pricing_calculation.paymenttest;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

final class PaymentFlowBot {

    private final String baseUrl;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    PaymentFlowBot(String baseUrl, ObjectMapper objectMapper) {
        this.baseUrl = baseUrl;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    PaymentFlowResult run(PaymentBotSeed seed, String requestedGateway) {
        long startedAt = System.nanoTime();
        String gateway = requestedGateway.toUpperCase(Locale.ROOT);
        try {
            JsonNode reservation = post("/api/reservations", Map.of(
                    "userId", seed.userId(),
                    "vehicleId", seed.vehicleId(),
                    "zoneId", seed.zoneId(),
                    "startTime", LocalDateTime.now().plusHours(1).toString(),
                    "endTime", LocalDateTime.now().plusHours(3).toString()
            ));
            requireText(reservation, "status", "APPROVED");
            long reservationId = reservation.path("id").asLong();

            JsonNode checkIn = post("/api/parking-sessions/check-in", Map.of(
                    "reservationId", reservationId,
                    "vehicleId", seed.vehicleId(),
                    "slotId", seed.slotId(),
                    "ticketCode", "T-" + seed.code(),
                    "entryTime", LocalDateTime.now().minusHours(2).toString()
            ));
            requireText(checkIn, "status", "ACTIVE");
            long sessionId = checkIn.path("id").asLong();

            JsonNode checkout = post("/api/payment-checkout/prepare", Map.of(
                    "licensePlate", seed.code(),
                    "exitTime", LocalDateTime.now().toString(),
                    "lostTicket", false,
                    "overtimeMinutes", 0
            ));
            requireText(checkout, "sessionStatus", "CHECKED_OUT");
            BigDecimal totalFee = checkout.path("totalFee").decimalValue();
            if (totalFee.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalStateException("Calculated totalFee must be positive");
            }

            Map<String, Object> gatewayRequest = new LinkedHashMap<>();
            gatewayRequest.put("sessionId", sessionId);
            gatewayRequest.put("returnUrl", "https://payment-bot.test/return?code=" + seed.code());
            gatewayRequest.put("orderInfo", "Payment bot " + seed.code());
            JsonNode payment = post("/api/payment-gateways/" + gateway.toLowerCase(Locale.ROOT), gatewayRequest);
            String referenceCode = payment.path("referenceCode").stringValue("");
            long paymentId = payment.path("paymentId").asLong();
            if ("CASH".equals(gateway)) {
                requireText(payment, "status", "COMPLETED");
            } else {
                requireText(payment, "status", "PENDING");
                if (payment.path("paymentUrl").stringValue("").isBlank()
                        || payment.path("qrContent").stringValue("").isBlank()) {
                    throw new IllegalStateException("Online gateway must return paymentUrl and qrContent");
                }
                payment = post("/api/payment-gateways/" + gateway.toLowerCase(Locale.ROOT) + "/confirm", Map.of(
                        "referenceCode", referenceCode,
                        "status", "SUCCESS",
                        "transactionNo", "BOT-" + seed.code(),
                        "message", "Bot payment confirmed"
                ));
                requireText(payment, "status", "COMPLETED");
            }
            if (payment.path("exitDeadline").isMissingNode() || payment.path("exitDeadline").isNull()) {
                throw new IllegalStateException("Completed payment must return exitDeadline");
            }

            JsonNode paymentStatus = get("/api/payment-checkout/sessions/" + sessionId + "/status");
            if (!paymentStatus.path("paid").asBoolean()) {
                throw new IllegalStateException("Payment checkout status must be paid");
            }
            requireText(paymentStatus, "paymentStatus", "COMPLETED");
            if (paymentStatus.path("exitWindowMinutes").asInt() != 15) {
                throw new IllegalStateException("Expected 15 minute exit window");
            }

            JsonNode exitValidation = post("/api/payment-checkout/validate-exit", Map.of(
                    "licensePlate", seed.code(),
                    "detectedAt", LocalDateTime.now().toString()
            ));
            if (!exitValidation.path("openBarrier").asBoolean()) {
                throw new IllegalStateException(
                        "Barrier should open but decision was "
                                + exitValidation.path("decision").stringValue(""));
            }
            requireText(exitValidation, "decision", "OPEN_PAYMENT_VERIFIED");

            return new PaymentFlowResult(
                    seed.code(),
                    gateway,
                    true,
                    elapsedMillis(startedAt),
                    totalFee,
                    paymentId,
                    referenceCode,
                    null
            );
        } catch (Exception exception) {
            return new PaymentFlowResult(
                    seed.code(),
                    gateway,
                    false,
                    elapsedMillis(startedAt),
                    BigDecimal.ZERO,
                    null,
                    null,
                    exception.getMessage()
            );
        }
    }

    private JsonNode get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();
        return send(request);
    }

    private JsonNode post(String path, Object body) throws Exception {
        String json = objectMapper.writeValueAsString(body);
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return send(request);
    }

    private JsonNode send(HttpRequest request) throws Exception {
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException(
                    request.method() + " " + request.uri().getPath()
                            + " returned " + response.statusCode() + ": " + response.body());
        }
        return objectMapper.readTree(response.body());
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
