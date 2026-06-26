package com.example.pricing_calculation.repository;

import com.example.pricing_calculation.domain.PaymentModuleParkingSlot;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentModuleParkingSlotRepository extends JpaRepository<PaymentModuleParkingSlot, Long> {

    long countByStatusIgnoreCase(String status);

    long countByZoneId(Long zoneId);

    long countByZoneIdAndStatusIgnoreCase(Long zoneId, String status);

    List<PaymentModuleParkingSlot> findByZoneIdAndStatusIgnoreCaseOrderBySlotCodeAsc(Long zoneId, String status);

    @Query("""
            select slot
            from PaymentModuleParkingSlot slot
            join fetch slot.zone zone
            join fetch zone.vehicleType vehicleType
            left join fetch zone.floor floor
            left join fetch floor.building building
            where upper(slot.status) = upper(:status)
              and (:zoneId is null or zone.id = :zoneId)
              and (:vehicleTypeId is null or vehicleType.id = :vehicleTypeId)
            order by zone.zoneName asc, slot.slotCode asc
            """)
    List<PaymentModuleParkingSlot> searchAvailableSlots(
            @Param("zoneId") Long zoneId,
            @Param("vehicleTypeId") Long vehicleTypeId,
            @Param("status") String status
    );

    @Query("""
            select count(slot)
            from PaymentModuleParkingSlot slot
            join slot.zone zone
            join zone.vehicleType vehicleType
            where upper(slot.status) = upper(:status)
              and (:zoneId is null or zone.id = :zoneId)
              and (:vehicleTypeId is null or vehicleType.id = :vehicleTypeId)
            """)
    long countAvailableSlots(
            @Param("zoneId") Long zoneId,
            @Param("vehicleTypeId") Long vehicleTypeId,
            @Param("status") String status
    );
}
