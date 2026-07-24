package com.example.pricing_calculation.web;

import com.example.pricing_calculation.domain.UserAccount;
import com.example.pricing_calculation.domain.UserRole;
import com.example.pricing_calculation.dto.FeedbackCreateRequest;
import com.example.pricing_calculation.dto.FeedbackResponse;
import com.example.pricing_calculation.service.FeedbackService;
import com.example.pricing_calculation.service.PaymentModuleAuthService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/software-incidents")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Software incidents", description = "User, staff and manager report software issues to administrators")
public class SoftwareIncidentController {

    private final FeedbackService feedbackService;
    private final PaymentModuleAuthService authService;

    public SoftwareIncidentController(
            FeedbackService feedbackService,
            PaymentModuleAuthService authService) {
        this.feedbackService = feedbackService;
        this.authService = authService;
    }

    @PostMapping
    public ResponseEntity<FeedbackResponse> create(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody FeedbackCreateRequest request) {
        UserAccount reporter = authService.requireAnyRole(
                authorizationHeader,
                UserRole.PARKING_USER,
                UserRole.PARKING_STAFF,
                UserRole.PARKING_MANAGER);
        FeedbackCreateRequest softwareIssue = new FeedbackCreateRequest(
                null,
                "SOFTWARE_ISSUE",
                null,
                request == null ? null : request.content());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(feedbackService.create(reporter, softwareIssue));
    }

    @GetMapping("/me")
    public List<FeedbackResponse> mine(
            @RequestHeader("Authorization") String authorizationHeader) {
        UserAccount reporter = authService.requireAnyRole(
                authorizationHeader,
                UserRole.PARKING_USER,
                UserRole.PARKING_STAFF,
                UserRole.PARKING_MANAGER);
        return feedbackService.mySoftwareIssues(reporter);
    }

    @GetMapping
    public List<FeedbackResponse> all(
            @RequestHeader("Authorization") String authorizationHeader) {
        authService.requireAnyRole(authorizationHeader, UserRole.ADMINISTRATOR);
        return feedbackService.softwareIssues();
    }

    @PatchMapping("/{id}/resolve")
    public FeedbackResponse resolve(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long id) {
        authService.requireAnyRole(authorizationHeader, UserRole.ADMINISTRATOR);
        return feedbackService.resolveSoftwareIssue(id);
    }
}
