package com.example.pricing_calculation.repository;

import com.example.pricing_calculation.domain.PaymentModuleParkingSession;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface PaymentModuleParkingSessionRepository extends
        JpaRepository<PaymentModuleParkingSession, Long>,
        JpaSpecificationExecutor<PaymentModuleParkingSession> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from PaymentModuleParkingSession s where s.id = :id")
    Optional<PaymentModuleParkingSession> findByIdForUpdate(@Param("id") Long id);

    long countByStatusIgnoreCase(String status);

    Optional<PaymentModuleParkingSession> findByTicketCodeIgnoreCase(String ticketCode);

    Optional<PaymentModuleParkingSession> findFirstByVehiclePlateNumberIgnoreCaseOrderByEntryTimeDesc(String plateNumber);
    Optional<PaymentModuleParkingSession> findFirstByVehicleUserIdAndStatusInOrderByEntryTimeDesc(Long userId, List<String> statuses);
    List<PaymentModuleParkingSession> findByVehicleUserIdOrderByEntryTimeDesc(Long userId);
    long countByVehicleIdAndStatusIn(Long vehicleId, List<String> statuses);
}
