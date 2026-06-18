package com.smartparking.repository;

import com.smartparking.model.schemas.ParkingSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParkingSessionRepository extends JpaRepository<ParkingSession, Long> {
    List<ParkingSession> findByStatus(String status);
    List<ParkingSession> findByVehicleId(Long vehicleId);
}