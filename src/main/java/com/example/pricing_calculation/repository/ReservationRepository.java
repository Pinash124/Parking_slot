package com.example.pricing_calculation.repository;

import com.example.pricing_calculation.domain.Reservation;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationRepository extends
        JpaRepository<Reservation, Long>,
        JpaSpecificationExecutor<Reservation> {

    long countByStatusIgnoreCase(String status);

    @Query("""
            select count(reservation) from Reservation reservation
            where reservation.zone.id = :zoneId
              and upper(reservation.status) in ('PENDING', 'APPROVED', 'CONFIRMED')
              and reservation.startTime < :endTime
              and reservation.endTime > :startTime
            """)
    long countActiveOverlaps(
            @Param("zoneId") Long zoneId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    java.util.List<Reservation> findByVehicleIdAndStatusIgnoreCase(Long vehicleId, String status);
}
