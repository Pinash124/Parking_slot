package com.example.pricing_calculation.dto;

import com.example.pricing_calculation.domain.Feedback;
import java.time.LocalDateTime;

public record FeedbackResponse(
        Long id,
        Long userId,
        Long sessionId,
        String feedbackType,
        Integer rating,
        String content,
        String status,
        LocalDateTime createdAt
) {

    public static FeedbackResponse from(Feedback feedback) {
        return new FeedbackResponse(
                feedback.getId(),
                feedback.getUser() == null ? null : feedback.getUser().getId(),
                feedback.getSession() == null ? null : feedback.getSession().getId(),
                feedback.getFeedbackType(),
                feedback.getRating(),
                feedback.getContent(),
                feedback.getStatus(),
                feedback.getCreatedAt()
        );
    }
}
