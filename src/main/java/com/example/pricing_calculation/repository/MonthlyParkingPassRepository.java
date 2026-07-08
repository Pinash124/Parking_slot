package com.example.pricing_calculation.repository;

import com.example.pricing_calculation.domain.MonthlyParkingPass;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonthlyParkingPassRepository extends JpaRepository<MonthlyParkingPass, Long> {

    @EntityGraph(attributePaths = {
            "user",
            "vehicle",
            "vehicle.vehicleType",
            "vehicleType",
            "reservedSlot",
            "reservedSlot.zone",
            "reservedSlot.zone.floor",
            "reservedSlot.zone.vehicleType"
    })
    List<MonthlyParkingPass> findByVehicleUserIdOrderByCreatedAtDesc(Long userId);

    List<MonthlyParkingPass> findByVehicleIdOrderByCreatedAtDesc(Long vehicleId);

    @EntityGraph(attributePaths = {
            "user",
            "vehicle",
            "vehicle.vehicleType",
            "vehicleType",
            "reservedSlot",
            "reservedSlot.zone",
            "reservedSlot.zone.floor",
            "reservedSlot.zone.vehicleType"
    })
    List<MonthlyParkingPass> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {
            "user",
            "vehicle",
            "vehicle.vehicleType",
            "vehicleType",
            "reservedSlot",
            "reservedSlot.zone",
            "reservedSlot.zone.floor",
            "reservedSlot.zone.vehicleType"
    })
    Optional<MonthlyParkingPass> findByPaymentReferenceIgnoreCase(String paymentReference);
}
