package com.example.pricing_calculation.repository;

import com.example.pricing_calculation.domain.PricingPolicy;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PricingPolicyRepository extends JpaRepository<PricingPolicy, Long> {

    @Query("""
            select policy from PricingPolicy policy
            where policy.vehicleType.id = :vehicleTypeId
              and (policy.status is null or upper(policy.status) = 'ACTIVE')
              and (policy.effectiveFrom is null or policy.effectiveFrom <= :atTime)
              and (policy.effectiveTo is null or policy.effectiveTo >= :atTime)
            order by policy.effectiveFrom desc, policy.id desc
            """)
    List<PricingPolicy> findActivePolicies(
            @Param("vehicleTypeId") Long vehicleTypeId,
            @Param("atTime") LocalDateTime atTime);
}
