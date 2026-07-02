package com.example.pricing_calculation.dto;

import com.example.pricing_calculation.domain.Notification;
import java.time.LocalDateTime;

public final class NotificationDtos {

    private NotificationDtos() {
    }

    public record NotificationResponse(
            Long id,
            Long userId,
            String userEmail,
            String title,
            String message,
            Boolean read,
            LocalDateTime createdAt) {
        public static NotificationResponse from(Notification notification) {
            return new NotificationResponse(
                    notification.getId(),
                    notification.getUser().getId(),
                    notification.getUser().getEmail(),
                    notification.getTitle(),
                    notification.getMessage(),
                    notification.getRead(),
                    notification.getCreatedAt()
            );
        }
    }
}
