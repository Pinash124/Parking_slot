package com.example.pricing_calculation.repository;

import com.example.pricing_calculation.domain.PaymentModuleParkingSession;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PaymentModuleParkingSessionRepository extends
        JpaRepository<PaymentModuleParkingSession, Long>,
        JpaSpecificationExecutor<PaymentModuleParkingSession> {

    long countByStatusIgnoreCase(String status);

    Optional<PaymentModuleParkingSession> findByTicketCodeIgnoreCase(String ticketCode);

    Optional<PaymentModuleParkingSession> findFirstByVehiclePlateNumberIgnoreCaseOrderByEntryTimeDesc(String plateNumber);
}
