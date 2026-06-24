package com.example.pricing_calculation.repository;

import com.example.pricing_calculation.domain.ParkingSession;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ParkingSessionRepository extends
        JpaRepository<ParkingSession, Long>,
        JpaSpecificationExecutor<ParkingSession> {

    long countByStatusIgnoreCase(String status);

    Optional<ParkingSession> findByTicketCodeIgnoreCase(String ticketCode);

    Optional<ParkingSession> findFirstByVehiclePlateNumberIgnoreCaseOrderByEntryTimeDesc(String plateNumber);
}
