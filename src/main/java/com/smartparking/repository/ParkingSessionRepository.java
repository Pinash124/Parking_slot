package com.smartparking.repository;

import com.smartparking.model.schemas.ParkingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ParkingSessionRepository extends JpaRepository<ParkingSession, Long> {
    List<ParkingSession> findByStatus(String status);
    List<ParkingSession> findByVehicleId(Long vehicleId);
    Optional<ParkingSession> findFirstByTicketCodeOrderByEntryTimeDesc(String ticketCode);
    boolean existsBySlotIdAndStatusIn(Long slotId, Collection<String> statuses);
    boolean existsByVehicleIdAndStatusIn(Long vehicleId, Collection<String> statuses);

    long countByEntryTimeBetween(LocalDateTime from, LocalDateTime to);

    long countByExitTimeBetween(LocalDateTime from, LocalDateTime to);

    @Query("select coalesce(sum(p.totalFee), 0) from ParkingSession p where p.exitTime between :from and :to")
    BigDecimal sumRevenueBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select hour(p.entryTime), count(p) from ParkingSession p where p.entryTime between :from and :to group by hour(p.entryTime)")
    List<Object[]> countEntriesByHour(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
