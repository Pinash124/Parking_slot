package com.smartparking.repository;

import com.smartparking.model.schemas.PricingPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PricingPolicyRepository extends JpaRepository<PricingPolicy, Long> {

    List<PricingPolicy> findByVehicleTypeId(Long vehicleTypeId);
}
