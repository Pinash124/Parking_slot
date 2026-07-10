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
    private final TimeBandParkingFeeCalculator timeBandCalculator;

    public PricingService(
            PaymentModulePricingPolicyRepository pricingPolicyRepository,
            PaymentModuleVehicleTypeRepository vehicleTypeRepository,
            VehicleRepository vehicleRepository,
            MonthlyParkingPassRepository monthlyParkingPassRepository,
            TimeBandParkingFeeCalculator timeBandCalculator) {
        this.pricingPolicyRepository = pricingPolicyRepository;
        this.vehicleTypeRepository = vehicleTypeRepository;
        this.vehicleRepository = vehicleRepository;
        this.monthlyParkingPassRepository = monthlyParkingPassRepository;
        this.timeBandCalculator = timeBandCalculator;
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
        VehicleTypeEntity vehicleType = vehicleTypeRepository.findById(vehicleTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle type not found: " + vehicleTypeId));
        LocalDateTime effectiveAt = atTime == null ? LocalDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")) : atTime;
        PaymentModulePricingPolicy policy = findPolicy(vehicleTypeId, effectiveAt);
        if (policy != null && policy.getMonthlyRate() != null && policy.getMonthlyRate().compareTo(BigDecimal.ZERO) > 0) {
            return policy.getMonthlyRate().setScale(2, RoundingMode.HALF_UP);
        }
        return firstPositive(vehicleType.getMonthlyRate(), BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public List<PaymentModulePricingPolicy> activePolicies(Long vehicleTypeId, LocalDateTime atTime) {
        if (vehicleTypeId == null) {
            throw new BadRequestException("vehicleTypeId is required");
        }
        return pricingPolicyRepository.findActivePolicies(
                vehicleTypeId,
                atTime == null ? LocalDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")) : atTime
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
        
        int wheelCount = VehicleTypeClassifier.wheelCount(vehicleType);
        TimeBandParkingFeeCalculator.Result timeBandFee = timeBandCalculator.calculate(
                wheelCount, entryTime, exitTime);
        BigDecimal hourlyRate = timeBandFee.nightHourlyRate();
        BigDecimal dailyRate = timeBandFee.dayTurnRate();
        BigDecimal lostTicketFee = lostTicket ? BigDecimal.valueOf(50000) : BigDecimal.ZERO;
        BigDecimal overtimeFeeRate = BigDecimal.ZERO;
        BigDecimal fixedSurcharge = BigDecimal.ZERO;

        if (policy != null) {
            fixedSurcharge = firstPositive(policy.getFixedSurcharge(), BigDecimal.ZERO);
            if (lostTicket && policy.getLostTicketFee() != null && policy.getLostTicketFee().compareTo(BigDecimal.ZERO) > 0) {
                lostTicketFee = policy.getLostTicketFee();
            } else if (lostTicket) {
                lostTicketFee = BigDecimal.valueOf(50000);
            }
            overtimeFeeRate = firstPositive(policy.getOvertimeFee(), BigDecimal.ZERO);
        }

        int safeOvertimeMinutes = Math.max(0, overtimeMinutes == null ? 0 : overtimeMinutes);
        long durationMinutes = Duration.between(entryTime, exitTime).toMinutes();
        long billableHours = timeBandFee.nightHours();
        
        BigDecimal parkingFee;
        BigDecimal overtimeFee;
        BigDecimal penaltyFee;
        BigDecimal fixedSurchargeVal;
        BigDecimal totalFee;

        if (monthlyPassActive) {
            parkingFee = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            overtimeFee = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            penaltyFee = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            fixedSurchargeVal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            totalFee = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        } else {
            parkingFee = timeBandFee.total().setScale(2, RoundingMode.HALF_UP);
            overtimeFee = overtimeFeeRate.multiply(BigDecimal.valueOf(safeOvertimeMinutes)).setScale(2, RoundingMode.HALF_UP);
            penaltyFee = lostTicketFee;
            fixedSurchargeVal = fixedSurcharge;
            totalFee = parkingFee.add(penaltyFee).add(fixedSurchargeVal).add(overtimeFee).setScale(2, RoundingMode.HALF_UP);
        }

        return new PricingQuoteResponse(
                vehicleType.getId(),
                vehicleType.getName(),
                policy != null ? policy.getId() : null,
                policy != null ? policy.getPolicyName() : "Day/night parking tariff",
                entryTime,
                exitTime,
                durationMinutes,
                billableHours,
                hourlyRate,
                dailyRate,
                parkingFee,
                monthlyPassActive ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : lostTicketFee,
                overtimeFee,
                fixedSurchargeVal,
                penaltyFee,
                totalFee,
                "VND"
        );
    }

    private boolean hasActiveMonthlyPass(Long vehicleId, LocalDateTime atTime) {
        LocalDate date = (atTime == null ? LocalDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")) : atTime).toLocalDate();
        return monthlyParkingPassRepository.findByVehicleIdOrderByCreatedAtDesc(vehicleId).stream()
                .anyMatch(pass -> pass.isActiveAt(date));
    }

    private PaymentModulePricingPolicy findPolicy(Long vehicleTypeId, LocalDateTime atTime) {
        return pricingPolicyRepository.findActivePolicies(vehicleTypeId, atTime).stream()
                .findFirst()
                .orElse(null);
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
