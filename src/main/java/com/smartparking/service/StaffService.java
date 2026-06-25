package com.smartparking.service;

import com.smartparking.model.enums.ParkingSessionStatus;
import com.smartparking.model.enums.ParkingSlotStatus;
import com.smartparking.model.requests.SlotStatusUpdateRequest;
import com.smartparking.model.requests.StaffCheckInRequest;
import com.smartparking.model.requests.StaffCheckOutRequest;
import com.smartparking.model.schemas.ParkingIncident;
import com.smartparking.model.schemas.ParkingSession;
import com.smartparking.model.schemas.ParkingSlot;
import com.smartparking.repository.ParkingIncidentRepository;
import com.smartparking.repository.ParkingSessionRepository;
import com.smartparking.repository.ParkingSlotRepository;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class StaffService {

    private static final Set<String> OPEN_SESSION_STATUSES = Set.of(
            ParkingSessionStatus.CREATED.name(),
            ParkingSessionStatus.ACTIVE.name(),
            ParkingSessionStatus.OVERDUE.name(),
            ParkingSessionStatus.VIOLATION.name(),
            ParkingSessionStatus.PAYMENT_PENDING.name()
    );

    private final ParkingSessionRepository parkingSessionRepository;
    private final ParkingSlotRepository parkingSlotRepository;
    private final ParkingIncidentRepository parkingIncidentRepository;

    public StaffService(ParkingSessionRepository parkingSessionRepository,
                        ParkingSlotRepository parkingSlotRepository,
                        ParkingIncidentRepository parkingIncidentRepository) {
        this.parkingSessionRepository = parkingSessionRepository;
        this.parkingSlotRepository = parkingSlotRepository;
        this.parkingIncidentRepository = parkingIncidentRepository;
    }

    public List<ParkingSession> getSessions(@Nullable String status, @Nullable Long vehicleId) {
        if (status != null && !status.isBlank()) {
            return parkingSessionRepository.findByStatus(normalize(status));
        }
        if (vehicleId != null) {
            return parkingSessionRepository.findByVehicleId(vehicleId);
        }
        return parkingSessionRepository.findAll();
    }

    public ParkingSession getSessionById(Long id) {
        return parkingSessionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Parking session not found: " + id));
    }

    public ParkingSession getSessionByTicketCode(String ticketCode) {
        if (ticketCode == null || ticketCode.isBlank()) {
            throw new IllegalArgumentException("Ticket code is required");
        }
        return parkingSessionRepository.findFirstByTicketCodeOrderByEntryTimeDesc(ticketCode.trim())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Parking session not found for ticket code: " + ticketCode));
    }

    @Transactional
    public ParkingSession createSession(StaffCheckInRequest request) {
        ParkingSlot slot = parkingSlotRepository.findByIdForUpdate(request.getSlotId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Parking slot not found: " + request.getSlotId()));

        String slotStatus = slot.getStatus() == null
                ? ParkingSlotStatus.AVAILABLE.name()
                : normalize(slot.getStatus());

        if (ParkingSlotStatus.RESERVED.name().equals(slotStatus) && request.getReservationId() == null) {
            throw new IllegalArgumentException("Reserved slot requires a matching reservation");
        }
        if (!ParkingSlotStatus.AVAILABLE.name().equals(slotStatus)
                && !ParkingSlotStatus.RESERVED.name().equals(slotStatus)) {
            throw new IllegalArgumentException("Parking slot is not available for check-in");
        }
        if (parkingSessionRepository.existsBySlotIdAndStatusIn(request.getSlotId(), OPEN_SESSION_STATUSES)) {
            throw new IllegalArgumentException("Parking slot already has an active vehicle");
        }
        if (parkingSessionRepository.existsByVehicleIdAndStatusIn(request.getVehicleId(), OPEN_SESSION_STATUSES)) {
            throw new IllegalArgumentException("Vehicle already has an active parking session");
        }

        ParkingSession session = new ParkingSession();
        session.setReservationId(request.getReservationId());
        session.setVehicleId(request.getVehicleId());
        session.setSlotId(request.getSlotId());
        session.setEntryStaffId(request.getEntryStaffId());
        session.setEntryGateId(request.getEntryGateId());
        session.setTicketCode(blankToNull(request.getTicketCode()));
        session.setEntryTime(LocalDateTime.now());
        session.setParkingFee(BigDecimal.ZERO);
        session.setPenaltyFee(BigDecimal.ZERO);
        session.setTotalFee(BigDecimal.ZERO);
        session.setStatus(ParkingSessionStatus.ACTIVE.name());

        slot.setStatus(ParkingSlotStatus.OCCUPIED.name());
        parkingSlotRepository.save(slot);
        return parkingSessionRepository.save(session);
    }

    @Transactional
    public ParkingSession checkOut(Long sessionId, StaffCheckOutRequest request) {
        ParkingSession session = getSessionById(sessionId);
        String sessionStatus = session.getStatus() == null ? "" : normalize(session.getStatus());

        if (ParkingSessionStatus.COMPLETED.name().equals(sessionStatus)
                || ParkingSessionStatus.CLOSED.name().equals(sessionStatus)
                || session.getExitTime() != null) {
            throw new IllegalArgumentException("Parking session has already been checked out");
        }
        if (ParkingSessionStatus.CANCELLED.name().equals(sessionStatus)) {
            throw new IllegalArgumentException("Cancelled parking session cannot be checked out");
        }
        if (ParkingSessionStatus.PAYMENT_PENDING.name().equals(sessionStatus)) {
            throw new IllegalArgumentException("Parking session must be paid before check-out");
        }

        ParkingSlot slot = parkingSlotRepository.findByIdForUpdate(session.getSlotId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Parking slot not found: " + session.getSlotId()));

        BigDecimal parkingFee = defaultAmount(request.getParkingFee());
        BigDecimal penaltyFee = defaultAmount(request.getPenaltyFee());
        BigDecimal calculatedTotal = parkingFee.add(penaltyFee);
        BigDecimal totalFee = request.getTotalFee() == null ? calculatedTotal : request.getTotalFee();
        if (totalFee.compareTo(calculatedTotal) < 0) {
            throw new IllegalArgumentException("Total fee cannot be less than parking fee plus penalty fee");
        }

        session.setExitTime(LocalDateTime.now());
        session.setExitStaffId(request.getExitStaffId());
        session.setExitGateId(request.getExitGateId());
        session.setParkingFee(parkingFee);
        session.setPenaltyFee(penaltyFee);
        session.setTotalFee(totalFee);
        session.setStatus(ParkingSessionStatus.COMPLETED.name());

        slot.setStatus(ParkingSlotStatus.AVAILABLE.name());
        parkingSlotRepository.save(slot);
        return parkingSessionRepository.save(session);
    }

    public List<ParkingSlot> getSlots(@Nullable String status, @Nullable Long zoneId) {
        if (status != null && !status.isBlank()) {
            return parkingSlotRepository.findByStatus(normalize(status));
        }
        if (zoneId != null) {
            return parkingSlotRepository.findByZoneId(zoneId);
        }
        return parkingSlotRepository.findAll();
    }

    @Transactional
    public ParkingSlot updateSlotStatus(Long id, SlotStatusUpdateRequest request) {
        ParkingSlot slot = parkingSlotRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Parking slot not found: " + id));
        if (parkingSessionRepository.existsBySlotIdAndStatusIn(id, OPEN_SESSION_STATUSES)
                && request.getStatus() != ParkingSlotStatus.OCCUPIED) {
            throw new IllegalArgumentException("Cannot release or lock a slot with an active vehicle");
        }
        slot.setStatus(request.getStatus().name());
        slot.setNote(request.getNote());
        slot.setUpdatedAt(LocalDateTime.now());
        return parkingSlotRepository.save(slot);
    }

    public List<ParkingIncident> getIncidents(@Nullable String status) {
        if (status != null && !status.isBlank()) {
            return parkingIncidentRepository.findByStatus(normalize(status));
        }
        return parkingIncidentRepository.findAll();
    }

    public ParkingIncident createIncident(ParkingIncident incident) {
        if (incident.getSessionId() == null) {
            throw new IllegalArgumentException("Parking session ID is required");
        }
        if (incident.getIncidentType() == null || incident.getIncidentType().isBlank()) {
            throw new IllegalArgumentException("Incident type is required");
        }
        if (!parkingSessionRepository.existsById(incident.getSessionId())) {
            throw new IllegalArgumentException("Parking session not found: " + incident.getSessionId());
        }
        incident.setIncidentType(normalize(incident.getIncidentType()));
        incident.setCreatedAt(LocalDateTime.now());
        if (incident.getStatus() == null || incident.getStatus().isBlank()) {
            incident.setStatus("OPEN");
        } else {
            incident.setStatus(normalize(incident.getStatus()));
        }
        return parkingIncidentRepository.save(incident);
    }

    public ParkingIncident resolveIncident(Long id, @Nullable Long staffId) {
        ParkingIncident incident = parkingIncidentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found: " + id));
        if ("RESOLVED".equalsIgnoreCase(incident.getStatus())) {
            throw new IllegalArgumentException("Incident has already been resolved");
        }
        incident.setStatus("RESOLVED");
        incident.setResolvedBy(staffId);
        incident.setResolvedAt(LocalDateTime.now());
        return parkingIncidentRepository.save(incident);
    }

    private BigDecimal defaultAmount(@Nullable BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String normalize(String value) {
        return value.trim().toUpperCase();
    }

    @Nullable
    private String blankToNull(@Nullable String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
