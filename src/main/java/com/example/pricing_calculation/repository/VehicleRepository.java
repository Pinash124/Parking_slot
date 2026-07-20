package com.example.pricing_calculation.repository;

import com.example.pricing_calculation.domain.Vehicle;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    Optional<Vehicle> findByPlateNumberIgnoreCase(String plateNumber);
    Optional<Vehicle> findByQrCode(String qrCode);

    @Query("""
            select v from Vehicle v
            where upper(replace(replace(replace(v.plateNumber, '-', ''), '.', ''), ' ', '')) = :plate
            """)
    Optional<Vehicle> findByNormalizedPlate(@Param("plate") String plate);
    List<Vehicle> findByUserIdOrderByPlateNumberAsc(Long userId);
}
