package com.example.pricing_calculation.dto;

import com.example.pricing_calculation.domain.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class ManagementDtos {
    private ManagementDtos() { }

    public record BuildingRequest(String name, String address, String status) { }
    public record BuildingView(Long id, String name, String address, String status) {
        public static BuildingView from(Building x) { return new BuildingView(x.getId(), x.getName(), x.getAddress(), x.getStatus()); }
    }
    public record FloorRequest(Long buildingId, String floorName, Integer floorNumber) { }
    public record FloorView(Long id, Long buildingId, String buildingName, String floorName, Integer floorNumber) {
        public static FloorView from(Floor x) { return new FloorView(x.getId(), x.getBuilding().getId(), x.getBuilding().getName(), x.getFloorName(), x.getFloorNumber()); }
    }
    public record VehicleTypeRequest(String name, String description, BigDecimal defaultHourlyFee, BigDecimal dailyRate, BigDecimal monthlyRate, Integer wheelCount) { }
    public record VehicleTypeView(Long id, String name, String description, BigDecimal defaultHourlyFee, BigDecimal dailyRate, BigDecimal monthlyRate, Integer wheelCount) {
        public static VehicleTypeView from(VehicleTypeEntity x) { return new VehicleTypeView(x.getId(), x.getName(), x.getDescription(), x.getDefaultHourlyFee(), x.getDailyRate(), x.getMonthlyRate(), x.getWheelCount()); }
    }
    public record ZoneRequest(Long floorId, Long vehicleTypeId, String zoneName, String zoneType) { }
    public record ZoneView(Long id, Long floorId, String floorName, Long vehicleTypeId, String vehicleTypeName, String zoneName, String zoneType) {
        public static ZoneView from(Zone x) { return new ZoneView(x.getId(), x.getFloor().getId(), x.getFloor().getFloorName(), x.getVehicleType().getId(), x.getVehicleType().getName(), x.getZoneName(), x.getZoneType()); }
    }
    public record SlotRequest(Long zoneId, String slotCode, String status) { }
    public record SlotView(Long id, Long zoneId, String zoneName, String zoneType, Long vehicleTypeId, String vehicleTypeName, String slotCode, String status) {
        public static SlotView from(PaymentModuleParkingSlot x) { return new SlotView(x.getId(), x.getZone().getId(), x.getZone().getZoneName(), x.getZone().getZoneType(), x.getZone().getVehicleType().getId(), x.getZone().getVehicleType().getName(), x.getSlotCode(), x.getStatus()); }
    }
    public record PricingPolicyRequest(Long vehicleTypeId, String policyName, BigDecimal hourlyRate, BigDecimal dailyRate,
                                       BigDecimal monthlyRate, BigDecimal fixedSurcharge, BigDecimal lostTicketFee, BigDecimal overtimeFee, LocalDateTime effectiveFrom,
                                       LocalDateTime effectiveTo, String status) { }
    public record PricingPolicyView(Long id, Long vehicleTypeId, String vehicleTypeName, String policyName,
                                    BigDecimal hourlyRate, BigDecimal dailyRate, BigDecimal monthlyRate, BigDecimal fixedSurcharge, BigDecimal lostTicketFee,
                                    BigDecimal overtimeFee, LocalDateTime effectiveFrom, LocalDateTime effectiveTo,
                                    String status) {
        public static PricingPolicyView from(PaymentModulePricingPolicy x) { return new PricingPolicyView(x.getId(), x.getVehicleType().getId(), x.getVehicleType().getName(), x.getPolicyName(), x.getHourlyRate(), x.getDailyRate(), x.getMonthlyRate(), x.getFixedSurcharge(), x.getLostTicketFee(), x.getOvertimeFee(), x.getEffectiveFrom(), x.getEffectiveTo(), x.getStatus()); }
    }
    public record AdditionalServiceRequest(String name, BigDecimal price, String status) { }
    public record AdditionalServiceView(Long id, String name, BigDecimal price, String status) {
        public static AdditionalServiceView from(AdditionalService x) { return new AdditionalServiceView(x.getId(), x.getName(), x.getPrice(), x.getStatus()); }
    }
    public record VehicleRequest(Long vehicleTypeId, String plateNumber, String brand, String color) { }
    public record VehicleView(Long id, Long vehicleTypeId, String vehicleTypeName, String plateNumber, String brand, String color, String status, String qrCode) {
        public static VehicleView from(Vehicle x) { return new VehicleView(x.getId(), x.getVehicleType().getId(), x.getVehicleType().getName(), x.getPlateNumber(), x.getBrand(), x.getColor(), x.getStatus(), x.getQrCode()); }
    }
    public record UserView(Long id, String fullName, String email, String phone, String status, String role, LocalDateTime createdAt) {
        public static UserView from(UserAccount x) { return new UserView(x.getId(), x.getFullName(), x.getEmail(), x.getPhone(), x.getStatus(), UserRole.fromCode(x.getRole()).code(), x.getCreatedAt()); }
    }
    public record UserUpdateRequest(String fullName, String phone, String status, String role, String newPassword) { }
    public record AdminUserCreateRequest(String fullName, String email, String phone, String password, String role) { }
    public record SettingRequest(String value, String description) { }
    public record DeviceRequest(String deviceCode, String deviceType, String laneCode, String status, String configurationJson) { }
}
