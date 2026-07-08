package com.example.pricing_calculation.repository;

import com.example.pricing_calculation.domain.MonthlyParkingPass;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonthlyParkingPassRepository extends JpaRepository<MonthlyParkingPass, Long> {

    List<MonthlyParkingPass> findByVehicleUserIdOrderByCreatedAtDesc(Long userId);

    List<MonthlyParkingPass> findByVehicleIdOrderByCreatedAtDesc(Long vehicleId);

    List<MonthlyParkingPass> findAllByOrderByCreatedAtDesc();

    Optional<MonthlyParkingPass> findByPaymentReferenceIgnoreCase(String paymentReference);
}
