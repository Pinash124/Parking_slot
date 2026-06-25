package com.example.pricing_calculation.repository;

import com.example.pricing_calculation.domain.SystemSetting;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemSettingRepository extends JpaRepository<SystemSetting, Long> {

    Optional<SystemSetting> findByKey(String key);

    List<SystemSetting> findAllByOrderByKeyAsc();
}
