package com.example.pricing_calculation.service;

import com.example.pricing_calculation.domain.PaymentModulePricingPolicy;
import com.example.pricing_calculation.domain.VehicleTypeEntity;
import com.example.pricing_calculation.domain.Vehicle;
import com.example.pricing_calculation.dto.PricingQuoteResponse;
import com.example.pricing_calculation.repository.PaymentModulePricingPolicyRepository;
import com.example.pricing_calculation.repository.PaymentModuleVehicleTypeRepository;
import com.example.pricing_calculation.repository.MonthlyParkingPassRepository;
import com.example.pricing_calculation.repository.VehicleRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PricingService {

    private final PaymentModulePricingPolicyRepository pricingPolicyRepository;
    private final PaymentModuleVehicleTypeRepository vehicleTypeRepository;
    private final VehicleRepository vehicleRepository;
    private final MonthlyParkingPassRepository monthlyParkingPassRepository;

    public PricingService(
            PaymentModulePricingPolicyRepository pricingPolicyRepository,
            PaymentModuleVehicleTypeRepository vehicleTypeRepository,
            VehicleRepository vehicleRepository,
            MonthlyParkingPassRepository monthlyParkingPassRepository) {
        this.pricingPolicyRepository = pricingPolicyRepository;
        this.vehicleTypeRepository = vehicleTypeRepository;
        this.vehicleRepository = vehicleRepository;
        this.monthlyParkingPassRepository = monthlyParkingPassRepository;
    }

    @Transactional(readOnly = true)
    public PricingQuoteResponse estimate(
            Long vehicleTypeId,
            LocalDateTime entryTime,
            LocalDateTime exitTime,
            boolean lostTicket,
            Integer overtimeMinutes) {
        if (vehicleTypeId == null) {
            throw new BadRequestException("vehicleTypeId is required");
        }
        if (entryTime == null || exitTime == null) {
            throw new BadRequestException("entryTime and exitTime are required");
        }
        if (!exitTime.isAfter(entryTime)) {
            throw new BadRequestException("exitTime must be after entryTime");
        }

        return estimateInternal(
                vehicleTypeId,
                entryTime,
                exitTime,
                lostTicket,
                overtimeMinutes,
                false
        );
    }

    @Transactional(readOnly = true)
    public PricingQuoteResponse estimateForVehicle(
            Long vehicleId,
            LocalDateTime entryTime,
            LocalDateTime exitTime,
            boolean lostTicket,
            Integer overtimeMinutes) {
        if (vehicleId == null) {
            throw new BadRequestException("vehicleId is required");
        }
        if (entryTime == null || exitTime == null) {
            throw new BadRequestException("entryTime and exitTime are required");
        }
        if (!exitTime.isAfter(entryTime)) {
            throw new BadRequestException("exitTime must be after entryTime");
        }
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + vehicleId));
        boolean monthlyPassActive = hasActiveMonthlyPass(vehicleId, entryTime);
        return estimateInternal(
                vehicle.getVehicleType().getId(),
                entryTime,
                exitTime,
                lostTicket,
                overtimeMinutes,
                monthlyPassActive
        );
    }

    @Transactional(readOnly = true)
    public BigDecimal monthlyRateForVehicleType(Long vehicleTypeId, LocalDateTime atTime) {
        if (vehicleTypeId == null) {
            throw new BadRequestException("vehicleTypeId is required");
        }
        PaymentModulePricingPolicy policy = findPolicy(vehicleTypeId, atTime == null ? LocalDateTime.now() : atTime);
        return firstPositive(policy == null ? null : policy.getMonthlyRate(), BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public List<PaymentModulePricingPolicy> activePolicies(Long vehicleTypeId, LocalDateTime atTime) {
        if (vehicleTypeId == null) {
            throw new BadRequestException("vehicleTypeId is required");
        }
        return pricingPolicyRepository.findActivePolicies(
                vehicleTypeId,
                atTime == null ? LocalDateTime.now() : atTime
        );
    }

    private PricingQuoteResponse estimateInternal(
            Long vehicleTypeId,
            LocalDateTime entryTime,
            LocalDateTime exitTime,
            boolean lostTicket,
            Integer overtimeMinutes,
            boolean monthlyPassActive) {
        VehicleTypeEntity vehicleType = vehicleTypeRepository.findById(vehicleTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle type not found: " + vehicleTypeId));
        PaymentModulePricingPolicy policy = findPolicy(vehicleTypeId, entryTime);
        BigDecimal hourlyRate = firstPositive(
                policy == null ? null : policy.getHourlyRate(),
                vehicleType.getDefaultHourlyFee(),
                BigDecimal.ZERO
        );
        BigDecimal dailyRate = firstPositive(policy == null ? null : policy.getDailyRate(), BigDecimal.ZERO);
        BigDecimal lostTicketFee = lostTicket
                ? firstPositive(policy == null ? null : policy.getLostTicketFee(), BigDecimal.ZERO)
                : BigDecimal.ZERO;
        BigDecimal overtimeFeeRate = firstPositive(policy == null ? null : policy.getOvertimeFee(), BigDecimal.ZERO);
        BigDecimal fixedSurcharge = firstPositive(policy == null ? null : policy.getFixedSurcharge(), BigDecimal.ZERO);
        int safeOvertimeMinutes = Math.max(0, overtimeMinutes == null ? 0 : overtimeMinutes);
        long durationMinutes = Duration.between(entryTime, exitTime).toMinutes();
        long billableHours = Math.max(1, divideCeiling(durationMinutes, 60));
        BigDecimal parkingFee = monthlyPassActive
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : calculateParkingFee(billableHours, hourlyRate, dailyRate);
        BigDecimal overtimeFee = overtimeFeeRate.multiply(BigDecimal.valueOf(divideCeiling(safeOvertimeMinutes, 60)));
        BigDecimal penaltyFee = lostTicketFee.add(overtimeFee).add(fixedSurcharge);
        BigDecimal totalFee = parkingFee.add(penaltyFee).setScale(2, RoundingMode.HALF_UP);

        return new PricingQuoteResponse(
                vehicleType.getId(),
                vehicleType.getName(),
                policy == null ? null : policy.getId(),
                policy == null ? "Default vehicle type fee" : policy.getPolicyName(),
                entryTime,
                exitTime,
                durationMinutes,
                billableHours,
                hourlyRate,
                dailyRate,
                parkingFee,
                lostTicketFee,
                overtimeFee,
                fixedSurcharge,
                penaltyFee,
                totalFee,
                "VND"
        );
    }

    private boolean hasActiveMonthlyPass(Long vehicleId, LocalDateTime atTime) {
        LocalDate date = (atTime == null ? LocalDateTime.now() : atTime).toLocalDate();
        return monthlyParkingPassRepository.findByVehicleIdOrderByCreatedAtDesc(vehicleId).stream()
                .anyMatch(pass -> pass.isActiveAt(date));
    }

    private PaymentModulePricingPolicy findPolicy(Long vehicleTypeId, LocalDateTime atTime) {
        return pricingPolicyRepository.findActivePolicies(vehicleTypeId, atTime).stream()
                .findFirst()
                .orElse(null);
    }

    private BigDecimal calculateParkingFee(long billableHours, BigDecimal hourlyRate, BigDecimal dailyRate) {
        if (dailyRate.compareTo(BigDecimal.ZERO) > 0 && billableHours >= 24) {
            long days = billableHours / 24;
            long remainingHours = billableHours % 24;
            return dailyRate.multiply(BigDecimal.valueOf(days))
                    .add(hourlyRate.multiply(BigDecimal.valueOf(remainingHours)))
                    .setScale(2, RoundingMode.HALF_UP);
        }
        return hourlyRate.multiply(BigDecimal.valueOf(billableHours)).setScale(2, RoundingMode.HALF_UP);
    }

    private long divideCeiling(long value, long divisor) {
        if (value <= 0) {
            return 0;
        }
        return (value + divisor - 1) / divisor;
    }

    private BigDecimal firstPositive(BigDecimal... values) {
        for (BigDecimal value : values) {
            if (value != null && value.compareTo(BigDecimal.ZERO) > 0) {
                return value.setScale(2, RoundingMode.HALF_UP);
            }
        }
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
}
