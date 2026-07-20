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
    Optional<PaymentModuleParkingSession> findFirstByTicketCodeIgnoreCaseOrderByEntryTimeDesc(String ticketCode);
    Optional<PaymentModuleParkingSession> findFirstByVehicleIdOrderByEntryTimeDesc(Long vehicleId);

    Optional<PaymentModuleParkingSession> findFirstByVehiclePlateNumberIgnoreCaseOrderByEntryTimeDesc(String plateNumber);
    Optional<PaymentModuleParkingSession> findFirstByVehicleUserIdAndStatusInOrderByEntryTimeDesc(Long userId, List<String> statuses);
    List<PaymentModuleParkingSession> findByVehicleUserIdOrderByEntryTimeDesc(Long userId);
    List<PaymentModuleParkingSession> findAllByOrderByEntryTimeDesc();
    List<PaymentModuleParkingSession> findByStatusIgnoreCaseOrderByEntryTimeDesc(String status);
    long countByVehicleIdAndStatusIn(Long vehicleId, List<String> statuses);


    @Query("""
            select count(session)
            from PaymentModuleParkingSession session
            join session.vehicle vehicle
            join vehicle.vehicleType vehicleType
            where upper(session.status) in :statuses
              and (
                    vehicleType.wheelCount = 2
                    or lower(vehicleType.name) like '%motor%'
                    or lower(vehicleType.name) like '%bike%'
                    or lower(vehicleType.name) like '%xe may%'
                    or lower(vehicleType.name) like '%2 banh%'
              )
            """)
    long countActiveTwoWheelSessions(@Param("statuses") List<String> statuses);
}
