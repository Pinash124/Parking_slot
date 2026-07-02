package com.example.pricing_calculation.web;

import com.example.pricing_calculation.domain.UserAccount;
import com.example.pricing_calculation.domain.UserRole;
import com.example.pricing_calculation.dto.NotificationDtos.NotificationResponse;
import com.example.pricing_calculation.service.NotificationService;
import com.example.pricing_calculation.service.PaymentModuleAuthService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/notifications")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Notifications", description = "Thong bao cho nguoi dung")
public class NotificationController {

    private final NotificationService notificationService;
    private final PaymentModuleAuthService authService;

    public NotificationController(NotificationService notificationService, PaymentModuleAuthService authService) {
        this.notificationService = notificationService;
        this.authService = authService;
    }

    private UserAccount user(String header) {
        return authService.requireAnyRole(header, UserRole.PARKING_USER);
    }

    @GetMapping
    public List<NotificationResponse> list(
            @RequestHeader("Authorization") String header,
            @RequestParam(defaultValue = "false") boolean unreadOnly) {
        UserAccount current = user(header);
        return notificationService.list(current.getId(), unreadOnly);
    }

    @PatchMapping("/{id}/read")
    public NotificationResponse markAsRead(@RequestHeader("Authorization") String header, @PathVariable Long id) {
        UserAccount current = user(header);
        return notificationService.markAsRead(current.getId(), id);
    }
}
