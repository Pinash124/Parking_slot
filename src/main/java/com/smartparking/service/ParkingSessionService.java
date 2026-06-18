package com.smartparking.service;

import com.smartparking.model.enums.ParkingSessionStatus;
import com.smartparking.model.schemas.ParkingSession;
import com.smartparking.repository.ParkingSessionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ParkingSessionService {

    private final ParkingSessionRepository repository;

    public ParkingSessionService(ParkingSessionRepository repository) {
        this.repository = repository;
    }

    public List<ParkingSession> getAllSessions() {
        return repository.findAll();
    }

    public ParkingSession getSessionById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Session not found: " + id));
    }

    public List<ParkingSession> getSessionsByStatus(ParkingSessionStatus status) {
        return repository.findByStatus(status.name());
    }

    public List<ParkingSession> getSessionsByVehicle(Long vehicleId) {
        return repository.findByVehicleId(vehicleId);
    }

    public ParkingSession checkIn(ParkingSession session) {
        session.setEntryTime(LocalDateTime.now());
        session.setStatus(ParkingSessionStatus.ACTIVE.name());
        return repository.save(session);
    }

    public ParkingSession checkOut(Long id, Long exitStaffId, Long exitGateId) {
        ParkingSession session = getSessionById(id);
        session.setExitTime(LocalDateTime.now());
        session.setExitStaffId(exitStaffId);
        session.setExitGateId(exitGateId);
        session.setStatus(ParkingSessionStatus.COMPLETED.name());
        return repository.save(session);
    }

    public ParkingSession updateSession(Long id, ParkingSession updated) {
        ParkingSession existing = getSessionById(id);
        existing.setVehicleId(updated.getVehicleId());
        existing.setSlotId(updated.getSlotId());
        existing.setEntryStaffId(updated.getEntryStaffId());
        existing.setEntryGateId(updated.getEntryGateId());
        existing.setTicketCode(updated.getTicketCode());
        existing.setParkingFee(updated.getParkingFee());
        existing.setPenaltyFee(updated.getPenaltyFee());
        existing.setTotalFee(updated.getTotalFee());
        existing.setStatus(updated.getStatus());
        return repository.save(existing);
    }

    public ParkingSession updateStatus(Long id, ParkingSessionStatus status) {
        ParkingSession session = getSessionById(id);
        session.setStatus(status.name());
        return repository.save(session);
    }

    public void deleteSession(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Session not found: " + id);
        }
        repository.deleteById(id);
    }
}