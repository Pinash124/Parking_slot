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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static com.smartparking.service.NullSafety.requireNonNull;

@Service
public class StaffService {

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
            return requireNonNull(parkingSessionRepository.findByStatus(status.trim().toUpperCase()));
        }
        if (vehicleId != null) {
            return requireNonNull(parkingSessionRepository.findByVehicleId(vehicleId));
        }
        return requireNonNull(parkingSessionRepository.findAll());
    }

    public ParkingSession getSessionById(Long id) {
        ParkingSession session = requireNonNull(parkingSessionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Parking session not found: " + id)));
        return session;
    }

    public ParkingSession getSessionByTicketCode(@Nullable String ticketCode) {
        if (ticketCode == null || ticketCode.isBlank()) {
            throw new IllegalArgumentException("Ticket code is required");
        }
        ParkingSession session = requireNonNull(parkingSessionRepository.findFirstByTicketCodeOrderByEntryTimeDesc(ticketCode.trim())
                .orElseThrow(() -> new IllegalArgumentException("Parking session not found for ticket code: " + ticketCode)));
        return session;
    }

    public ParkingSession createSession(StaffCheckInRequest request) {
        Long slotId = requireNonNull(request.getSlotId());
        ParkingSlot slot = parkingSlotRepository.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Parking slot not found: " + request.getSlotId()));

        String currentStatus = slot.getStatus() == null ? ParkingSlotStatus.AVAILABLE.name() : slot.getStatus().trim().toUpperCase();
        if (!ParkingSlotStatus.AVAILABLE.name().equals(currentStatus) && !ParkingSlotStatus.RESERVED.name().equals(currentStatus)) {
            throw new IllegalArgumentException("Parking slot is not available for check-in");
        }

        ParkingSession session = new ParkingSession();
        session.setReservationId(request.getReservationId());
        session.setVehicleId(request.getVehicleId());
        session.setSlotId(slotId);
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

    public ParkingSession checkOut(Long sessionId, StaffCheckOutRequest request) {
        ParkingSession session = getSessionById(sessionId);
        Long slotId = requireNonNull(session.getSlotId());
        ParkingSlot slot = parkingSlotRepository.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Parking slot not found: " + session.getSlotId()));

        BigDecimal parkingFee = requireNonNull(defaultAmount(request.getParkingFee()));
        BigDecimal penaltyFee = requireNonNull(defaultAmount(request.getPenaltyFee()));
        BigDecimal totalFee = request.getTotalFee() != null
                ? request.getTotalFee()
                : parkingFee.add(penaltyFee);

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
            return requireNonNull(parkingSlotRepository.findByStatus(status.trim().toUpperCase()));
        }
        if (zoneId != null) {
            return requireNonNull(parkingSlotRepository.findByZoneId(zoneId));
        }
        return requireNonNull(parkingSlotRepository.findAll());
    }

    public ParkingSlot updateSlotStatus(Long id, SlotStatusUpdateRequest request) {
        ParkingSlot slot = parkingSlotRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Parking slot not found: " + id));
        slot.setStatus(request.getStatus().name());
        return parkingSlotRepository.save(slot);
    }

    public List<ParkingIncident> getIncidents(@Nullable String status) {
        if (status != null && !status.isBlank()) {
            return requireNonNull(parkingIncidentRepository.findByStatus(status.trim().toUpperCase()));
        }
        return requireNonNull(parkingIncidentRepository.findAll());
    }

    public ParkingIncident createIncident(ParkingIncident incident) {
        incident.setCreatedAt(LocalDateTime.now());
        if (incident.getCreatedBy() == null) {
            incident.setCreatedBy(incident.getReportedBy());
        }
        if (incident.getStatus() == null || incident.getStatus().isBlank()) {
            incident.setStatus("OPEN");
        }
        return parkingIncidentRepository.save(incident);
    }

    public ParkingIncident resolveIncident(Long id) {
        ParkingIncident incident = parkingIncidentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found: " + id));
        incident.setStatus("RESOLVED");
        incident.setResolvedAt(LocalDateTime.now());
        return parkingIncidentRepository.save(incident);
    }

    private BigDecimal defaultAmount(@Nullable BigDecimal value) {
        if (value == null) {
            return new BigDecimal("0");
        }
        return value;
    }

    @Nullable
    private String blankToNull(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
