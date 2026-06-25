package com.example.pricing_calculation.service;

import com.example.pricing_calculation.domain.LicensePlateScan;
import com.example.pricing_calculation.domain.ParkingSession;
import com.example.pricing_calculation.domain.UserAccount;
import com.example.pricing_calculation.dto.LicensePlateScanCreateRequest;
import com.example.pricing_calculation.dto.LicensePlateScanResponse;
import com.example.pricing_calculation.repository.LicensePlateScanRepository;
import com.example.pricing_calculation.repository.ParkingSessionRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LicensePlateScanService {

    private final LicensePlateScanRepository licensePlateScanRepository;
    private final ParkingSessionRepository parkingSessionRepository;
    private final AuditLogService auditLogService;

    public LicensePlateScanService(
            LicensePlateScanRepository licensePlateScanRepository,
            ParkingSessionRepository parkingSessionRepository,
            AuditLogService auditLogService) {
        this.licensePlateScanRepository = licensePlateScanRepository;
        this.parkingSessionRepository = parkingSessionRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public LicensePlateScanResponse create(UserAccount staff, LicensePlateScanCreateRequest request) {
        validate(request);
        ParkingSession session = parkingSessionRepository.findById(request.sessionId())
                .orElseThrow(() -> new ResourceNotFoundException("Parking session not found: " + request.sessionId()));
        LicensePlateScan scan = new LicensePlateScan();
        scan.setSession(session);
        scan.setLaneCode(request.laneCode());
        scan.setPlateNumber(request.plateNumber());
        scan.setImageUrl(request.imageUrl());
        scan.setConfidenceScore(request.confidenceScore());
        scan.setScanTime(request.scanTime() == null ? LocalDateTime.now() : request.scanTime());
        LicensePlateScan saved = licensePlateScanRepository.save(scan);
        auditLogService.record(staff, "CREATE_LICENSE_PLATE_SCAN", "LicensePlateScan", saved.getId());
        return LicensePlateScanResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<LicensePlateScanResponse> bySession(Long sessionId) {
        return licensePlateScanRepository.findBySessionIdOrderByScanTimeDesc(sessionId)
                .stream()
                .map(LicensePlateScanResponse::from)
                .toList();
    }

    private void validate(LicensePlateScanCreateRequest request) {
        if (request == null || request.sessionId() == null) {
            throw new BadRequestException("sessionId is required");
        }
        if (request.laneCode() == null || request.laneCode().isBlank()) {
            throw new BadRequestException("laneCode is required for multi-lane processing");
        }
        if (request.plateNumber() == null || request.plateNumber().isBlank()) {
            throw new BadRequestException("plateNumber is required");
        }
        BigDecimal confidence = request.confidenceScore();
        if (confidence != null
                && (confidence.compareTo(BigDecimal.ZERO) < 0 || confidence.compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw new BadRequestException("confidenceScore must be between 0 and 100");
        }
    }
}
