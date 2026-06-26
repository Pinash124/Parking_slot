package com.example.pricing_calculation.dto;

public record FeedbackCreateRequest(
        Long sessionId,
        String feedbackType,
        Integer rating,
        String content
) {
}
