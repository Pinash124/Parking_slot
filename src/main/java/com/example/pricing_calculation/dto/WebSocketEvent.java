package com.example.pricing_calculation.dto;

import java.time.LocalDateTime;

public record WebSocketEvent(
        String type,
        String message,
        Object payload,
        LocalDateTime occurredAt
) {

    public static WebSocketEvent of(String type, String message, Object payload) {
        return new WebSocketEvent(type, message, payload, LocalDateTime.now());
    }
}
