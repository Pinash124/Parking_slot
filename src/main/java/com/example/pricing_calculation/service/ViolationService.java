package com.example.pricing_calculation.service;

import com.example.pricing_calculation.domain.PaymentModuleParkingSession;
import com.example.pricing_calculation.domain.UserAccount;
import com.example.pricing_calculation.domain.Violation;
import com.example.pricing_calculation.dto.ViolationDtos.ViolationRequest;
import com.example.pricing_calculation.dto.ViolationDtos.ViolationResponse;
import com.example.pricing_calculation.repository.NotificationRepository;
import com.example.pricing_calculation.repository.PaymentModuleParkingSessionRepository;
import com.example.pricing_calculation.repository.ViolationRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ViolationService {

    private final ViolationRepository violationRepository;
    private final PaymentModuleParkingSessionRepository sessionRepository;
    private final NotificationService notificationService;

    public ViolationService(
            ViolationRepository violationRepository,
            PaymentModuleParkingSessionRepository sessionRepository,
            NotificationService notificationService) {
        this.violationRepository = violationRepository;
        this.sessionRepository = sessionRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<ViolationResponse> list(String status) {
        return violationRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt", "id"))
                .stream()
                .filter(violation -> status == null || status.isBlank() || status.equalsIgnoreCase(violation.getStatus()))
                .map(ViolationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ViolationResponse getById(Long id) {
        return ViolationResponse.from(findViolation(id));
    }

    @Transactional
    public ViolationResponse create(UserAccount staff, ViolationRequest request) {
        if (request == null || request.sessionId() == null || request.violationType() == null || request.violationType().isBlank()) {
            throw new BadRequestException("sessionId and violationType are required");
        }
        PaymentModuleParkingSession session = sessionRepository.findById(request.sessionId())
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + request.sessionId()));

        Violation violation = new Violation();
        violation.setSession(session);
        violation.setViolationType(request.violationType());
        violation.setDescription(request.description());
        violation.setPenaltyAmount(request.penaltyAmount());
        violation.setStatus(request.status() == null || request.status().isBlank() ? "OPEN" : request.status().trim().toUpperCase());
        violation.setCreatedAt(LocalDateTime.now());

        Violation saved = violationRepository.save(violation);
        UserAccount owner = session.getVehicle().getUser();
        notificationService.notifyUser(
                owner,
                "Violation recorded",
                "A violation was recorded for ticket " + session.getTicketCode() + ": " + saved.getViolationType());
        return ViolationResponse.from(saved);
    }

    @Transactional
    public ViolationResponse resolve(UserAccount staff, Long id) {
        Violation violation = findViolation(id);
        violation.setStatus("RESOLVED");
        Violation saved = violationRepository.save(violation);
        notificationService.notifyUser(
                saved.getSession().getVehicle().getUser(),
                "Violation resolved",
                "Violation " + saved.getViolationType() + " for ticket " + saved.getSession().getTicketCode() + " has been resolved");
        return ViolationResponse.from(saved);
    }

    @Transactional
    public void delete(Long id) {
        violationRepository.delete(findViolation(id));
    }

    private Violation findViolation(Long id) {
        return violationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Violation not found: " + id));
    }
}
