package com.example.pricing_calculation.service;

import com.example.pricing_calculation.domain.Notification;
import com.example.pricing_calculation.domain.UserAccount;
import com.example.pricing_calculation.dto.NotificationDtos.NotificationResponse;
import com.example.pricing_calculation.repository.NotificationRepository;
import com.example.pricing_calculation.repository.UserAccountRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserAccountRepository userAccountRepository;

    public NotificationService(NotificationRepository notificationRepository, UserAccountRepository userAccountRepository) {
        this.notificationRepository = notificationRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional
    public NotificationResponse notifyUser(UserAccount user, String title, String message) {
        if (user == null) {
            throw new BadRequestException("user is required");
        }
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRead(Boolean.FALSE);
        notification.setCreatedAt(LocalDateTime.now());
        return NotificationResponse.from(notificationRepository.save(notification));
    }

    @Transactional
    public NotificationResponse notifyUser(Long userId, String title, String message) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        return notifyUser(user, title, message);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> list(Long userId, boolean unreadOnly) {
        List<Notification> notifications = unreadOnly
                ? notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId)
                : notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return notifications.stream().map(NotificationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> recent(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        return notificationRepository.findAll(PageRequest.of(0, safeLimit, Sort.by(Sort.Direction.DESC, "createdAt", "id")))
                .map(NotificationResponse::from)
                .toList();
    }

    @Transactional
    public NotificationResponse markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));
        notification.setRead(Boolean.TRUE);
        return NotificationResponse.from(notificationRepository.save(notification));
    }
}
