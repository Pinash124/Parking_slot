package com.example.pricing_calculation.repository;

import com.example.pricing_calculation.domain.Vehicle;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    Optional<Vehicle> findByPlateNumberIgnoreCase(String plateNumber);
    Optional<Vehicle> findByQrCode(String qrCode);
    List<Vehicle> findByUserIdOrderByPlateNumberAsc(Long userId);
}
