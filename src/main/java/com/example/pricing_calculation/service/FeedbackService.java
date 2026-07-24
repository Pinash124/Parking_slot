package com.example.pricing_calculation.service;

import com.example.pricing_calculation.domain.Feedback;
import com.example.pricing_calculation.domain.IncidentReport;
import com.example.pricing_calculation.domain.PaymentModuleParkingSession;
import com.example.pricing_calculation.domain.UserAccount;
import com.example.pricing_calculation.dto.FeedbackCreateRequest;
import com.example.pricing_calculation.dto.FeedbackResponse;
import com.example.pricing_calculation.repository.FeedbackRepository;
import com.example.pricing_calculation.repository.IncidentReportRepository;
import com.example.pricing_calculation.repository.PaymentModuleParkingSessionRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeedbackService {

    private static final Set<String> SUPPORTED_TYPES = Set.of(
            "LOST_TICKET",
            "WRONG_FEE",
            "HARD_TO_FIND_VEHICLE",
            "OCCUPIED_SLOT",
            "INCIDENT",
            "SOFTWARE_ISSUE",
            "COMPLAINT",
            "SUGGESTION",
            "OTHER"
    );

    private final FeedbackRepository feedbackRepository;
    private final IncidentReportRepository incidentReportRepository;
    private final PaymentModuleParkingSessionRepository parkingSessionRepository;
    private final AuditLogService auditLogService;

    public FeedbackService(
            FeedbackRepository feedbackRepository,
            IncidentReportRepository incidentReportRepository,
            PaymentModuleParkingSessionRepository parkingSessionRepository,
            AuditLogService auditLogService) {
        this.feedbackRepository = feedbackRepository;
        this.incidentReportRepository = incidentReportRepository;
        this.parkingSessionRepository = parkingSessionRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public FeedbackResponse create(UserAccount user, FeedbackCreateRequest request) {
        validate(request);
        PaymentModuleParkingSession session = null;
        if (request.sessionId() != null) {
            session = parkingSessionRepository.findById(request.sessionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parking session not found: " + request.sessionId()));
            if (!session.getVehicle().getUser().getId().equals(user.getId())) {
                throw new ForbiddenException("Parking session does not belong to current user");
            }
        }
        String type = normalizeType(request.feedbackType());

        Feedback feedback = new Feedback();
        feedback.setUser(user);
        feedback.setSession(session);
        feedback.setFeedbackType(type);
        feedback.setRating(request.rating());
        feedback.setContent(request.content());
        feedback.setStatus("OPEN");
        feedback.setCreatedAt(LocalDateTime.now());
        Feedback saved = feedbackRepository.save(feedback);

        if (session != null || "INCIDENT".equals(type)) {
            IncidentReport incident = new IncidentReport();
            incident.setSession(session);
            incident.setReportedBy(user);
            incident.setIncidentType(type);
            incident.setDescription(request.content());
            incident.setStatus("OPEN");
            incident.setCreatedAt(LocalDateTime.now());
            incidentReportRepository.save(incident);
        }
        auditLogService.record(user, "CREATE_FEEDBACK", "Feedback", saved.getId());
        return FeedbackResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<FeedbackResponse> myFeedback(UserAccount user) {
        return feedbackRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(FeedbackResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FeedbackResponse> allFeedback() {
        return feedbackRepository.findAll()
                .stream()
                .map(FeedbackResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FeedbackResponse> softwareIssues() {
        return feedbackRepository
                .findByFeedbackTypeIgnoreCaseOrderByCreatedAtDesc("SOFTWARE_ISSUE")
                .stream()
                .map(FeedbackResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FeedbackResponse> mySoftwareIssues(UserAccount user) {
        return feedbackRepository
                .findByUserIdAndFeedbackTypeIgnoreCaseOrderByCreatedAtDesc(
                        user.getId(),
                        "SOFTWARE_ISSUE")
                .stream()
                .map(FeedbackResponse::from)
                .toList();
    }

    @Transactional
    public FeedbackResponse resolveSoftwareIssue(Long id) {
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Software issue not found: " + id));
        if (!"SOFTWARE_ISSUE".equalsIgnoreCase(feedback.getFeedbackType())) {
            throw new BadRequestException("Feedback is not a software issue");
        }
        feedback.setStatus("RESOLVED");
        return FeedbackResponse.from(feedbackRepository.save(feedback));
    }

    private void validate(FeedbackCreateRequest request) {
        if (request == null) {
            throw new BadRequestException("Feedback request is required");
        }
        String type = normalizeType(request.feedbackType());
        if (!SUPPORTED_TYPES.contains(type)) {
            throw new BadRequestException("feedbackType must be one of " + SUPPORTED_TYPES);
        }
        if (request.rating() != null && (request.rating() < 1 || request.rating() > 5)) {
            throw new BadRequestException("rating must be between 1 and 5");
        }
        if (request.content() == null || request.content().trim().length() < 5) {
            throw new BadRequestException("content must contain at least 5 characters");
        }
    }

    private String normalizeType(String type) {
        return type == null ? "" : type.trim().toUpperCase(Locale.ROOT);
    }
}
