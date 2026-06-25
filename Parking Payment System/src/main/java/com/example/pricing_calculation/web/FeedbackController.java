package com.example.pricing_calculation.web;

import com.example.pricing_calculation.domain.UserAccount;
import com.example.pricing_calculation.domain.UserRole;
import com.example.pricing_calculation.dto.FeedbackCreateRequest;
import com.example.pricing_calculation.dto.FeedbackResponse;
import com.example.pricing_calculation.service.AuthService;
import com.example.pricing_calculation.service.FeedbackService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feedback")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Feedback", description = "Phan hoi mat ve, sai phi, kho tim xe, slot bi chiem va van de trong bai")
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final AuthService authService;

    public FeedbackController(FeedbackService feedbackService, AuthService authService) {
        this.feedbackService = feedbackService;
        this.authService = authService;
    }

    @PostMapping
    public ResponseEntity<FeedbackResponse> create(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestBody FeedbackCreateRequest request) {
        UserAccount user = authService.requireAnyRole(authorizationHeader, UserRole.PARKING_USER);
        return ResponseEntity.status(HttpStatus.CREATED).body(feedbackService.create(user, request));
    }

    @GetMapping("/me")
    public List<FeedbackResponse> myFeedback(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        UserAccount user = authService.requireAnyRole(authorizationHeader, UserRole.PARKING_USER);
        return feedbackService.myFeedback(user);
    }

    @GetMapping
    public List<FeedbackResponse> allFeedback(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        authService.requireAnyRole(
                authorizationHeader,
                UserRole.PARKING_MANAGER,
                UserRole.PARKING_STAFF,
                UserRole.ADMINISTRATOR
        );
        return feedbackService.allFeedback();
    }
}
