package com.example.pricing_calculation.service;

import com.example.pricing_calculation.domain.Building;
import com.example.pricing_calculation.domain.PaymentModuleParkingSlot;
import com.example.pricing_calculation.domain.PaymentModulePricingPolicy;
import com.example.pricing_calculation.domain.VehicleTypeEntity;
import com.example.pricing_calculation.dto.AvailableSlotResponse;
import com.example.pricing_calculation.dto.ParkingFacilityInfoResponse;
import com.example.pricing_calculation.repository.BuildingRepository;
import com.example.pricing_calculation.repository.PaymentModuleParkingSlotRepository;
import com.example.pricing_calculation.repository.PaymentModulePricingPolicyRepository;
import com.example.pricing_calculation.repository.PaymentModuleVehicleTypeRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ParkingInfoService {

    private final BuildingRepository buildingRepository;
    private final PaymentModuleVehicleTypeRepository vehicleTypeRepository;
    private final PaymentModulePricingPolicyRepository pricingPolicyRepository;
    private final PaymentModuleParkingSlotRepository parkingSlotRepository;
    private final String operationHours;
    private final String defaultName;
    private final String defaultAddress;

    public ParkingInfoService(
            BuildingRepository buildingRepository,
            PaymentModuleVehicleTypeRepository vehicleTypeRepository,
            PaymentModulePricingPolicyRepository pricingPolicyRepository,
            PaymentModuleParkingSlotRepository parkingSlotRepository,
            @Value("${parking.info.operation-hours:24/7}") String operationHours,
            @Value("${parking.info.name:SmartParking}") String defaultName,
            @Value("${parking.info.address:Configured in Buildings table}") String defaultAddress) {
        this.buildingRepository = buildingRepository;
        this.vehicleTypeRepository = vehicleTypeRepository;
        this.pricingPolicyRepository = pricingPolicyRepository;
        this.parkingSlotRepository = parkingSlotRepository;
        this.operationHours = operationHours;
        this.defaultName = defaultName;
        this.defaultAddress = defaultAddress;
    }

    @Transactional(readOnly = true)
    public ParkingFacilityInfoResponse getFacilityInfo() {
        Building building = buildingRepository.findAll().stream().findFirst().orElse(null);
        List<VehicleTypeEntity> vehicleTypes = vehicleTypeRepository.findAll();
        List<PaymentModulePricingPolicy> policies = pricingPolicyRepository.findAll();
        return new ParkingFacilityInfoResponse(
                building == null || building.getName() == null ? defaultName : building.getName(),
                building == null || building.getAddress() == null ? defaultAddress : building.getAddress(),
                operationHours,
                parkingRules(),
                vehicleTypes.stream().map(type -> new ParkingFacilityInfoResponse.VehicleTypeInfo(
                        type.getId(),
                        type.getName(),
                        type.getDescription(),
                        type.getDefaultHourlyFee()
                )).toList(),
                policies.stream().map(policy -> new ParkingFacilityInfoResponse.PricingInfo(
                        policy.getId(),
                        policy.getPolicyName(),
                        policy.getVehicleType() == null ? null : policy.getVehicleType().getId(),
                        policy.getVehicleType() == null ? null : policy.getVehicleType().getName(),
                        policy.getHourlyRate(),
                        policy.getDailyRate(),
                        policy.getLostTicketFee(),
                        policy.getOvertimeFee(),
                        policy.getStatus()
                )).toList(),
                parkingSlotRepository.count(),
                parkingSlotRepository.countByStatusIgnoreCase("AVAILABLE"),
                parkingSlotRepository.countByStatusIgnoreCase("OCCUPIED"),
                parkingSlotRepository.countByStatusIgnoreCase("RESERVED")
        );
    }

    @Transactional(readOnly = true)
    public List<AvailableSlotResponse> availableSlots(Long zoneId, Long vehicleTypeId) {
        return parkingSlotRepository.searchAvailableSlots(zoneId, vehicleTypeId, "AVAILABLE")
                .stream()
                .map(AvailableSlotResponse::from)
                .toList();
    }

    private List<String> parkingRules() {
        return List.of(
                "Xe vao bai phai co ticket code, QR/RFID hoac parking session hop le",
                "Moi slot chi chua mot xe tai mot thoi diem",
                "Xe ra bai phai hoan tat thanh toan va con trong cua so validate exit",
                "Slot da dat truoc khong duoc su dung boi xe khac",
                "Mat ve, sai phi, kho tim xe hoac slot bi chiem can gui feedback/incident de nhan vien xu ly"
        );
    }
}
