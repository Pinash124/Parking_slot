package com.example.pricing_calculation.repository;
import com.example.pricing_calculation.domain.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;
public interface SystemSettingRepository extends JpaRepository<SystemSetting, String> { }
