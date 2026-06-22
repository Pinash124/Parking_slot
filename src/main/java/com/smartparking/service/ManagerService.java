package com.smartparking.service;

import com.smartparking.model.enums.ParkingSlotStatus;
import com.smartparking.model.requests.ManagerOverviewReport;
import com.smartparking.model.requests.SlotStatusUpdateRequest;
import com.smartparking.model.requests.UserRoleUpdateRequest;
import com.smartparking.model.requests.UserStatusUpdateRequest;
import com.smartparking.model.requests.VehicleTypeReport;
import com.smartparking.model.schemas.ParkingBuilding;
import com.smartparking.model.schemas.ParkingIncident;
import com.smartparking.model.schemas.ParkingSlot;
import com.smartparking.model.schemas.ParkingZone;
import com.smartparking.model.schemas.PricingPolicy;
import com.smartparking.model.schemas.Role;
import com.smartparking.model.schemas.SystemConfig;
import com.smartparking.model.schemas.User;
import com.smartparking.model.schemas.VehicleType;
import com.smartparking.repository.ParkingBuildingRepository;
import com.smartparking.repository.ParkingIncidentRepository;
import com.smartparking.repository.ParkingSessionRepository;
import com.smartparking.repository.ParkingSlotRepository;
import com.smartparking.repository.ParkingZoneRepository;
import com.smartparking.repository.PricingPolicyRepository;
import com.smartparking.repository.SystemConfigRepository;
import com.smartparking.repository.UserRepository;
import com.smartparking.repository.VehicleTypeRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ManagerService {

    private final ParkingBuildingRepository buildingRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final ParkingZoneRepository zoneRepository;
    private final ParkingSlotRepository slotRepository;
    private final PricingPolicyRepository pricingPolicyRepository;
    private final ParkingIncidentRepository incidentRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final UserRepository userRepository;
    private final ParkingSessionRepository parkingSessionRepository;

    public ManagerService(ParkingBuildingRepository buildingRepository,
                          VehicleTypeRepository vehicleTypeRepository,
                          ParkingZoneRepository zoneRepository,
                          ParkingSlotRepository slotRepository,
                          PricingPolicyRepository pricingPolicyRepository,
                          ParkingIncidentRepository incidentRepository,
                          SystemConfigRepository systemConfigRepository,
                          UserRepository userRepository,
                          ParkingSessionRepository parkingSessionRepository) {
        this.buildingRepository = buildingRepository;
        this.vehicleTypeRepository = vehicleTypeRepository;
        this.zoneRepository = zoneRepository;
        this.slotRepository = slotRepository;
        this.pricingPolicyRepository = pricingPolicyRepository;
        this.incidentRepository = incidentRepository;
        this.systemConfigRepository = systemConfigRepository;
        this.userRepository = userRepository;
        this.parkingSessionRepository = parkingSessionRepository;
    }

    public ParkingBuilding createBuilding(ParkingBuilding building) {
        markCreated(building);
        return buildingRepository.save(building);
    }

    public ParkingBuilding updateBuilding(Long id, ParkingBuilding updated) {
        ParkingBuilding building = buildingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Building not found: " + id));
        building.setName(updated.getName());
        building.setAddress(updated.getAddress());
        building.setDescription(updated.getDescription());
        building.setStatus(updated.getStatus());
        building.setUpdatedAt(LocalDateTime.now());
        return buildingRepository.save(building);
    }

    public VehicleType createVehicleType(VehicleType vehicleType) {
        vehicleTypeRepository.findByName(vehicleType.getName()).ifPresent(existing -> {
            throw new IllegalArgumentException("Vehicle type name already exists");
        });
        markCreated(vehicleType);
        return vehicleTypeRepository.save(vehicleType);
    }

    public VehicleType updateVehicleType(Long id, VehicleType updated) {
        VehicleType vehicleType = vehicleTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle type not found: " + id));
        vehicleType.setName(updated.getName());
        vehicleType.setDescription(updated.getDescription());
        vehicleType.setDefaultHourlyFee(updated.getDefaultHourlyFee());
        return vehicleTypeRepository.save(vehicleType);
    }

    public ParkingZone createZone(ParkingZone zone) {
        markCreated(zone);
        return zoneRepository.save(zone);
    }

    public ParkingZone updateZone(Long id, ParkingZone updated) {
        ParkingZone zone = zoneRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Parking zone not found: " + id));
        zone.setFloorId(updated.getFloorId());
        zone.setVehicleTypeId(updated.getVehicleTypeId());
        zone.setName(updated.getName());
        return zoneRepository.save(zone);
    }

    public ParkingSlot createSlot(ParkingSlot slot) {
        slotRepository.findBySlotCode(slot.getSlotCode()).ifPresent(existing -> {
            throw new IllegalArgumentException("Slot code already exists");
        });
        if (slot.getStatus() == null || slot.getStatus().isBlank()) {
            slot.setStatus(ParkingSlotStatus.AVAILABLE.name());
        }
        markCreated(slot);
        return slotRepository.save(slot);
    }

    public ParkingSlot updateSlot(Long id, ParkingSlot updated) {
        ParkingSlot slot = slotRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Parking slot not found: " + id));
        slot.setZoneId(updated.getZoneId());
        slot.setVehicleTypeId(updated.getVehicleTypeId());
        slot.setSlotCode(updated.getSlotCode());
        slot.setStatus(updated.getStatus());
        slot.setNote(updated.getNote());
        slot.setUpdatedAt(LocalDateTime.now());
        return slotRepository.save(slot);
    }

    public ParkingSlot updateSlotStatus(Long id, SlotStatusUpdateRequest request) {
        ParkingSlot slot = slotRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Parking slot not found: " + id));
        slot.setStatus(request.getStatus().name());
        slot.setNote(request.getNote());
        slot.setUpdatedAt(LocalDateTime.now());
        return slotRepository.save(slot);
    }

    public PricingPolicy createPricingPolicy(PricingPolicy policy) {
        markCreated(policy);
        return pricingPolicyRepository.save(policy);
    }

    public PricingPolicy updatePricingPolicy(Long id, PricingPolicy updated) {
        PricingPolicy policy = pricingPolicyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pricing policy not found: " + id));
        policy.setVehicleTypeId(updated.getVehicleTypeId());
        policy.setName(updated.getName());
        policy.setPriceAmount(updated.getPriceAmount());
        policy.setPricingUnit(updated.getPricingUnit());
        policy.setPeakStartTime(updated.getPeakStartTime());
        policy.setPeakEndTime(updated.getPeakEndTime());
        policy.setPeakMultiplier(updated.getPeakMultiplier());
        policy.setRules(updated.getRules());
        policy.setStatus(updated.getStatus());
        policy.setUpdatedAt(LocalDateTime.now());
        return pricingPolicyRepository.save(policy);
    }

    public ParkingIncident createIncident(ParkingIncident incident) {
        incident.setCreatedAt(LocalDateTime.now());
        if (incident.getStatus() == null || incident.getStatus().isBlank()) {
            incident.setStatus("OPEN");
        }
        return incidentRepository.save(incident);
    }

    public ParkingIncident resolveIncident(Long id, Long managerId) {
        ParkingIncident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found: " + id));
        incident.setStatus("RESOLVED");
        incident.setResolvedBy(managerId);
        incident.setResolvedAt(LocalDateTime.now());
        return incidentRepository.save(incident);
    }

    public User updateUserRole(Long id, UserRoleUpdateRequest request) {
        if (request == null || request.getRole() == null || request.getRole().isBlank()) {
            throw new IllegalArgumentException("Role is required");
        }

        Role role = Role.from(request.getRole());
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        user.setRole(role.name());
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    public User updateUserStatus(Long id, UserStatusUpdateRequest request) {
        if (request == null || request.getStatus() == null || request.getStatus().isBlank()) {
            throw new IllegalArgumentException("Status is required");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        user.setStatus(request.getStatus().trim().toUpperCase());
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    public SystemConfig saveSystemConfig(SystemConfig config) {
        SystemConfig target = systemConfigRepository.findByConfigKey(config.getConfigKey()).orElse(config);
        target.setConfigKey(config.getConfigKey());
        target.setConfigValue(config.getConfigValue());
        target.setDescription(config.getDescription());
        target.setUpdatedAt(LocalDateTime.now());
        return systemConfigRepository.save(target);
    }

    public ManagerOverviewReport getOverviewReport(LocalDate from, LocalDate to) {
        LocalDateTime start = from == null ? LocalDate.now().atStartOfDay() : from.atStartOfDay();
        LocalDateTime end = to == null ? LocalDateTime.now() : to.plusDays(1).atStartOfDay().minusNanos(1);
        long totalSlots = slotRepository.count();
        long available = slotRepository.countByStatus(ParkingSlotStatus.AVAILABLE.name());
        long occupied = slotRepository.countByStatus(ParkingSlotStatus.OCCUPIED.name());
        long reserved = slotRepository.countByStatus(ParkingSlotStatus.RESERVED.name());
        long maintenance = slotRepository.countByStatus(ParkingSlotStatus.MAINTENANCE.name());
        long locked = slotRepository.countByStatus(ParkingSlotStatus.LOCKED.name());
        double occupancyRate = totalSlots == 0 ? 0 : (occupied * 100.0) / totalSlots;

        Map<Integer, Long> peakHours = new LinkedHashMap<>();
        parkingSessionRepository.countEntriesByHour(start, end).forEach(row ->
                peakHours.put(((Number) row[0]).intValue(), ((Number) row[1]).longValue()));

        return new ManagerOverviewReport(
                start,
                end,
                parkingSessionRepository.countByEntryTimeBetween(start, end),
                parkingSessionRepository.countByExitTimeBetween(start, end),
                parkingSessionRepository.sumRevenueBetween(start, end),
                totalSlots,
                available,
                occupied,
                reserved,
                maintenance,
                locked,
                occupancyRate,
                peakHours
        );
    }

    public List<VehicleTypeReport> getVehicleTypeReports() {
        return vehicleTypeRepository.findAll().stream()
                .map(vehicleType -> {
                    List<ParkingSlot> slots = slotRepository.findByVehicleTypeId(vehicleType.getVehicleTypeId());
                    long available = slots.stream()
                            .filter(slot -> ParkingSlotStatus.AVAILABLE.name().equals(slot.getStatus()))
                            .count();
                    return new VehicleTypeReport(
                            vehicleType.getVehicleTypeId(),
                            vehicleType.getName(),
                            slots.size(),
                            available,
                            BigDecimal.ZERO
                    );
                })
                .toList();
    }

    private void markCreated(ParkingBuilding building) {
        LocalDateTime now = LocalDateTime.now();
        building.setCreatedAt(now);
        building.setUpdatedAt(now);
        if (building.getStatus() == null || building.getStatus().isBlank()) {
            building.setStatus("ACTIVE");
        }
    }

    private void markCreated(VehicleType vehicleType) {
        LocalDateTime now = LocalDateTime.now();
        vehicleType.setCreatedAt(now);
        vehicleType.setUpdatedAt(now);
        if (vehicleType.getStatus() == null || vehicleType.getStatus().isBlank()) {
            vehicleType.setStatus("ACTIVE");
        }
    }

    private void markCreated(ParkingZone zone) {
        LocalDateTime now = LocalDateTime.now();
        zone.setCreatedAt(now);
        zone.setUpdatedAt(now);
        if (zone.getStatus() == null || zone.getStatus().isBlank()) {
            zone.setStatus("ACTIVE");
        }
    }

    private void markCreated(ParkingSlot slot) {
        LocalDateTime now = LocalDateTime.now();
        slot.setCreatedAt(now);
        slot.setUpdatedAt(now);
    }

    private void markCreated(PricingPolicy policy) {
        LocalDateTime now = LocalDateTime.now();
        policy.setCreatedAt(now);
        policy.setUpdatedAt(now);
        if (policy.getStatus() == null || policy.getStatus().isBlank()) {
            policy.setStatus("ACTIVE");
        }
    }
}
