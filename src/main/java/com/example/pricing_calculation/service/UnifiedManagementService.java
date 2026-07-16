package com.example.pricing_calculation.service;

import com.example.pricing_calculation.domain.*;
import com.example.pricing_calculation.dto.ManagementDtos.*;
import com.example.pricing_calculation.repository.*;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UnifiedManagementService {
    private static final String DAY_START_KEY = "pricing.dayStart";
    private static final String NIGHT_START_KEY = "pricing.nightStart";
    private static final List<String> BILLING_MODES = List.of("PER_TURN", "PER_HOUR", "PER_BLOCK");
    private static final List<String> SLOT_STATUSES = List.of("AVAILABLE", "OCCUPIED", "RESERVED", "MAINTENANCE",
            "LOCKED", "MONTHLY_HELD", "MONTHLY_RESERVED", "MONTHLY_OCCUPIED");
    private static final List<String> ZONE_TYPES = List.of("CAR_NORMAL", "CAR_MONTHLY", "MOTORBIKE");
    private final BuildingRepository buildings;
    private final FloorRepository floors;
    private final PaymentModuleVehicleTypeRepository vehicleTypes;
    private final ZoneRepository zones;
    private final PaymentModuleParkingSlotRepository slots;
    private final PaymentModulePricingPolicyRepository policies;
    private final AdditionalServiceRepository services;
    private final SystemSettingRepository settings;

    public UnifiedManagementService(BuildingRepository buildings, FloorRepository floors,
            PaymentModuleVehicleTypeRepository vehicleTypes, ZoneRepository zones,
            PaymentModuleParkingSlotRepository slots, PaymentModulePricingPolicyRepository policies,
            AdditionalServiceRepository services, SystemSettingRepository settings) {
        this.buildings = buildings;
        this.floors = floors;
        this.vehicleTypes = vehicleTypes;
        this.zones = zones;
        this.slots = slots;
        this.policies = policies;
        this.services = services;
        this.settings = settings;
    }

    @Transactional(readOnly = true)
    public List<BuildingView> buildings() {
        return buildings.findAll().stream().map(BuildingView::from).toList();
    }

    @Transactional
    public BuildingView saveBuilding(Long id, BuildingRequest r) {
        require(r != null && text(r.name()), "name is required");
        Building x = id == null ? new Building() : building(id);
        x.setName(r.name().trim());
        x.setAddress(r.address());
        x.setStatus(normal(r.status(), "ACTIVE"));
        return BuildingView.from(buildings.save(x));
    }

    @Transactional
    public void deleteBuilding(Long id) {
        buildings.delete(building(id));
    }

    @Transactional(readOnly = true)
    public List<FloorView> floors(Long buildingId) {
        return (buildingId == null ? floors.findAll() : floors.findByBuildingIdOrderByFloorNumberAsc(buildingId))
                .stream().map(FloorView::from).toList();
    }

    @Transactional
    public FloorView saveFloor(Long id, FloorRequest r) {
        require(r != null && r.buildingId() != null && text(r.floorName()), "buildingId and floorName are required");
        Floor x = id == null ? new Floor() : floor(id);
        x.setBuilding(building(r.buildingId()));
        x.setFloorName(r.floorName().trim());
        x.setFloorNumber(r.floorNumber());
        return FloorView.from(floors.save(x));
    }

    @Transactional
    public void deleteFloor(Long id) {
        floors.delete(floor(id));
    }

    @Transactional(readOnly = true)
    public List<VehicleTypeView> vehicleTypes() {
        return vehicleTypes.findAll().stream().map(VehicleTypeView::from).toList();
    }

    @Transactional
    public VehicleTypeView saveVehicleType(Long id, VehicleTypeRequest r) {
        require(r != null && text(r.name()), "name is required");
        VehicleTypeEntity x = id == null ? new VehicleTypeEntity() : vehicleType(id);
        x.setName(r.name().trim());
        x.setDescription(r.description());
        x.setDefaultHourlyFee(nonNegative(r.defaultHourlyFee()));
        x.setDailyRate(nonNegative(r.dailyRate()));
        x.setMonthlyRate(nonNegative(r.monthlyRate()));
        if (r.wheelCount() != null) {
            require(r.wheelCount() == 2 || r.wheelCount() == 4, "wheelCount must be 2 or 4");
            x.setWheelCount(r.wheelCount());
        }
        return VehicleTypeView.from(vehicleTypes.save(x));
    }

    @Transactional
    public void deleteVehicleType(Long id) {
        vehicleTypes.delete(vehicleType(id));
    }

    @Transactional(readOnly = true)
    public List<ZoneView> zones(Long floorId, Long vehicleTypeId) {
        List<Zone> list = floorId != null ? zones.findByFloorId(floorId)
                : vehicleTypeId != null ? zones.findByVehicleTypeId(vehicleTypeId) : zones.findAll();
        return list.stream().map(ZoneView::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ZoneView> zonesForPurpose(Long floorId, String purpose) {
        String p = normal(purpose, "RESERVATION");
        List<String> allowed = switch (p) {
            case "MONTHLY" -> List.of("CAR_MONTHLY");
            case "RESERVATION" -> List.of("CAR_NORMAL");
            case "PARKING" -> List.of("CAR_NORMAL", "MOTORBIKE");
            default -> throw new BadRequestException("purpose must be RESERVATION, MONTHLY or PARKING");
        };
        return zones(floorId, null).stream().filter(z -> allowed.contains(z.zoneType())).toList();
    }

    @Transactional
    public ZoneView saveZone(Long id, ZoneRequest r) {
        require(r != null && r.floorId() != null && r.vehicleTypeId() != null && text(r.zoneName()),
                "floorId, vehicleTypeId and zoneName are required");
        Zone x = id == null ? new Zone() : zone(id);
        VehicleTypeEntity type = vehicleType(r.vehicleTypeId());
        String zoneType = normal(r.zoneType(), VehicleTypeClassifier.isCar(type) ? "CAR_NORMAL" : "MOTORBIKE");
        require(ZONE_TYPES.contains(zoneType), "zoneType must be one of " + ZONE_TYPES);
        require(VehicleTypeClassifier.isCar(type) ? zoneType.startsWith("CAR_") : "MOTORBIKE".equals(zoneType),
                "zoneType does not match vehicle type");
        x.setFloor(floor(r.floorId()));
        x.setVehicleType(type);
        x.setZoneName(r.zoneName().trim());
        x.setZoneType(zoneType);
        return ZoneView.from(zones.save(x));
    }

    @Transactional
    public void deleteZone(Long id) {
        zones.delete(zone(id));
    }

    @Transactional(readOnly = true)
    public List<SlotView> slots(Long zoneId, Long vehicleTypeId, String status) {
        return slots.findAll().stream().filter(x -> zoneId == null || x.getZone().getId().equals(zoneId))
                .filter(x -> vehicleTypeId == null || x.getZone().getVehicleType().getId().equals(vehicleTypeId))
                .filter(x -> !text(status) || status.equalsIgnoreCase(x.getStatus())).map(SlotView::from).toList();
    }

    @Transactional
    public SlotView saveSlot(Long id, SlotRequest r) {
        require(r != null && r.zoneId() != null && text(r.slotCode()), "zoneId and slotCode are required");
        PaymentModuleParkingSlot x = id == null ? new PaymentModuleParkingSlot() : slot(id);
        x.setZone(zone(r.zoneId()));
        x.setSlotCode(r.slotCode());
        x.setStatus(slotStatus(r.status()));
        PaymentModuleParkingSlot saved = slots.save(x);
        if (VehicleTypeClassifier.isCar(saved.getZone().getVehicleType()))
            rebalanceCarSlots(saved.getZone().getFloor().getId());
        return SlotView.from(saved);
    }

    @Transactional
    public SlotView updateSlotStatus(Long id, String status) {
        PaymentModuleParkingSlot x = slot(id);
        x.setStatus(slotStatus(status));
        return SlotView.from(slots.save(x));
    }

    @Transactional
    public void deleteSlot(Long id) {
        PaymentModuleParkingSlot x = slot(id);
        Long floorId = x.getZone().getFloor().getId();
        boolean car = VehicleTypeClassifier.isCar(x.getZone().getVehicleType());
        slots.delete(x);
        if (car)
            rebalanceCarSlots(floorId);
    }

    @Transactional
    public List<ZoneView> rebalanceCarSlots(Long floorId) {
        List<Zone> floorZones = zones.findByFloorId(floorId);
        Zone normalZone = floorZones.stream().filter(z -> "CAR_NORMAL".equalsIgnoreCase(z.getZoneType())).findFirst()
                .orElseThrow(() -> new BadRequestException("CAR_NORMAL zone is required"));
        Zone monthlyZone = floorZones.stream().filter(z -> "CAR_MONTHLY".equalsIgnoreCase(z.getZoneType())).findFirst()
                .orElseThrow(() -> new BadRequestException("CAR_MONTHLY zone is required"));
        List<PaymentModuleParkingSlot> carSlots = slots.findAll().stream().filter(
                s -> s.getZone().getId().equals(normalZone.getId()) || s.getZone().getId().equals(monthlyZone.getId()))
                .toList();
        int monthlyTarget = CarZoneAllocation.monthlySlots(carSlots.size());
        long monthlyCurrent = carSlots.stream().filter(s -> s.getZone().getId().equals(monthlyZone.getId())).count();
        int move = (int) Math.abs(monthlyTarget - monthlyCurrent);
        if (move > 0) {
            Zone from = monthlyCurrent < monthlyTarget ? normalZone : monthlyZone;
            Zone to = monthlyCurrent < monthlyTarget ? monthlyZone : normalZone;
            List<PaymentModuleParkingSlot> candidates = carSlots.stream()
                    .filter(s -> s.getZone().getId().equals(from.getId())
                            && "AVAILABLE".equalsIgnoreCase(s.getStatus()))
                    .sorted(java.util.Comparator.comparing(PaymentModuleParkingSlot::getSlotCode).reversed())
                    .limit(move).toList();
            require(candidates.size() == move, "Not enough AVAILABLE car slots to maintain the 1/3 monthly allocation");
            candidates.forEach(s -> s.setZone(to));
            slots.saveAll(candidates);
        }
        renameCarSlots(normalZone, monthlyZone, carSlots);
        return List.of(ZoneView.from(normalZone), ZoneView.from(monthlyZone));
    }

    @Transactional(readOnly = true)
    public List<PricingPolicyView> policies() {
        return policies.findAll().stream().map(PricingPolicyView::from).toList();
    }

    @Transactional
    public PricingPolicyView savePolicy(Long id, PricingPolicyRequest r) {
        require(r != null && r.vehicleTypeId() != null && text(r.policyName()),
                "vehicleTypeId and policyName are required");
        PaymentModulePricingPolicy x = id == null ? new PaymentModulePricingPolicy() : policy(id);
        x.setVehicleType(vehicleType(r.vehicleTypeId()));
        x.setPolicyName(r.policyName());
        x.setHourlyRate(nonNegative(r.hourlyRate()));
        x.setHourlyBillingMode(billingMode(r.hourlyBillingMode(), "PER_HOUR"));
        x.setHourlyBillingBlockHours(blockHours(r.hourlyBillingBlockHours(), x.getHourlyBillingMode()));
        x.setDailyRate(nonNegative(r.dailyRate()));
        x.setDailyBillingMode(billingMode(r.dailyBillingMode(), "PER_TURN"));
        x.setDailyBillingBlockHours(blockHours(r.dailyBillingBlockHours(), x.getDailyBillingMode()));
        x.setMonthlyRate(nonNegative(r.monthlyRate()));
        x.setFixedSurcharge(nonNegative(r.fixedSurcharge()));
        x.setLostTicketFee(nonNegative(r.lostTicketFee()));
        x.setOvertimeFee(nonNegative(r.overtimeFee()));
        x.setEffectiveFrom(r.effectiveFrom());
        x.setEffectiveTo(r.effectiveTo());
        x.setStatus(normal(r.status(), "ACTIVE"));
        return PricingPolicyView.from(policies.save(x));
    }

    @Transactional(readOnly = true)
    public PricingRuleSettingsView pricingRules() {
        return new PricingRuleSettingsView(
                settingValue(DAY_START_KEY, "07:00"),
                settingValue(NIGHT_START_KEY, "22:00")
        );
    }

    @Transactional
    public PricingRuleSettingsView savePricingRules(PricingRuleSettingsRequest r) {
        require(r != null && text(r.dayStart()) && text(r.nightStart()), "dayStart and nightStart are required");
        String dayStart = normalizeTime(r.dayStart(), "dayStart");
        String nightStart = normalizeTime(r.nightStart(), "nightStart");
        require(!dayStart.equals(nightStart), "dayStart and nightStart must be different");
        saveSetting(DAY_START_KEY, dayStart, "Gio bat dau khung ban ngay tinh theo luot");
        saveSetting(NIGHT_START_KEY, nightStart, "Gio bat dau khung qua dem tinh theo gio");
        return new PricingRuleSettingsView(dayStart, nightStart);
    }

    @Transactional
    public void deletePolicy(Long id) {
        policies.delete(policy(id));
    }

    @Transactional(readOnly = true)
    public List<AdditionalServiceView> additionalServices() {
        return services.findAll().stream().map(AdditionalServiceView::from).toList();
    }

    @Transactional
    public AdditionalServiceView saveAdditionalService(Long id, AdditionalServiceRequest r) {
        require(r != null && text(r.name()), "name is required");
        AdditionalService x = id == null ? new AdditionalService() : additionalService(id);
        x.setName(r.name().trim());
        x.setPrice(nonNegative(r.price()));
        x.setStatus(normal(r.status(), "ACTIVE"));
        return AdditionalServiceView.from(services.save(x));
    }

    @Transactional
    public void deleteAdditionalService(Long id) {
        services.delete(additionalService(id));
    }

    private Building building(Long id) {
        return buildings.findById(id).orElseThrow(() -> new ResourceNotFoundException("Building not found: " + id));
    }

    private Floor floor(Long id) {
        return floors.findById(id).orElseThrow(() -> new ResourceNotFoundException("Floor not found: " + id));
    }

    private VehicleTypeEntity vehicleType(Long id) {
        return vehicleTypes.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle type not found: " + id));
    }

    private Zone zone(Long id) {
        return zones.findById(id).orElseThrow(() -> new ResourceNotFoundException("Zone not found: " + id));
    }

    private PaymentModuleParkingSlot slot(Long id) {
        return slots.findById(id).orElseThrow(() -> new ResourceNotFoundException("Slot not found: " + id));
    }

    private PaymentModulePricingPolicy policy(Long id) {
        return policies.findById(id).orElseThrow(() -> new ResourceNotFoundException("Policy not found: " + id));
    }

    private AdditionalService additionalService(Long id) {
        return services.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Additional service not found: " + id));
    }

    private String slotStatus(String value) {
        String v = normal(value, "AVAILABLE");
        require(SLOT_STATUSES.contains(v), "status must be one of " + SLOT_STATUSES);
        return v;
    }

    private void renameCarSlots(Zone normalZone, Zone monthlyZone, List<PaymentModuleParkingSlot> carSlots) {
        String prefix = normalZone.getZoneName().replaceFirst("(?i)-CAR-.*$", "");
        List<PaymentModuleParkingSlot> monthly = carSlots.stream()
                .filter(s -> s.getZone().getId().equals(monthlyZone.getId()))
                .sorted(java.util.Comparator.comparing(PaymentModuleParkingSlot::getSlotCode)).toList();
        List<PaymentModuleParkingSlot> normalSlots = carSlots.stream()
                .filter(s -> s.getZone().getId().equals(normalZone.getId()))
                .sorted(java.util.Comparator.comparing(PaymentModuleParkingSlot::getSlotCode)).toList();
        carSlots.forEach(s -> s.setSlotCode("TMP-" + s.getId()));
        slots.saveAll(carSlots);
        slots.flush();
        for (int i = 0; i < monthly.size(); i++)
            monthly.get(i).setSlotCode(prefix + "-CAR-" + String.format("%03d", i + 1));
        for (int i = 0; i < normalSlots.size(); i++)
            normalSlots.get(i).setSlotCode(prefix + "-CAR-" + String.format("%03d", monthly.size() + i + 1));
        slots.saveAll(carSlots);
    }

    private String normal(String v, String d) {
        return text(v) ? v.trim().toUpperCase(Locale.ROOT) : d;
    }

    private boolean text(String v) {
        return v != null && !v.isBlank();
    }

    private BigDecimal nonNegative(BigDecimal v) {
        if (v == null)
            return BigDecimal.ZERO;
        require(v.signum() >= 0, "amount cannot be negative");
        return v;
    }

    private String billingMode(String value, String fallback) {
        String mode = text(value) ? value.trim().toUpperCase(Locale.ROOT) : fallback;
        require(BILLING_MODES.contains(mode), "Invalid billing mode: " + value);
        return mode;
    }

    private Integer blockHours(Integer value, String mode) {
        if (!"PER_BLOCK".equals(mode)) {
            return 1;
        }
        int hours = value == null ? 1 : value;
        require(hours > 0 && hours <= 24, "Block hours must be between 1 and 24");
        return hours;
    }

    private String settingValue(String key, String fallback) {
        return settings.findById(key).map(SystemSetting::getValue).filter(this::text).orElse(fallback);
    }

    private void saveSetting(String key, String value, String description) {
        SystemSetting setting = settings.findById(key).orElseGet(SystemSetting::new);
        setting.setKey(key);
        setting.setValue(value);
        setting.setDescription(description);
        settings.save(setting);
    }

    private String normalizeTime(String value, String field) {
        try {
            return LocalTime.parse(value.trim()).toString();
        } catch (Exception e) {
            throw new BadRequestException(field + " must use HH:mm format");
        }
    }

    private void require(boolean ok, String message) {
        if (!ok)
            throw new BadRequestException(message);
    }
}
