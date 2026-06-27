package com.example.pricing_calculation.repository;
import com.example.pricing_calculation.domain.SystemDevice;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface SystemDeviceRepository extends JpaRepository<SystemDevice, Long> {
    List<SystemDevice> findByDeviceTypeIgnoreCase(String deviceType);
}
