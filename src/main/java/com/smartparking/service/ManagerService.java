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
import com.smartparking.model.schemas.User;
import com.smartparking.model.schemas.VehicleType;
import com.smartparking.repository.ParkingBuildingRepository;
import com.smartparking.repository.ParkingIncidentRepository;
import com.smartparking.repository.ParkingSessionRepository;
import com.smartparking.repository.ParkingSlotRepository;
import com.smartparking.repository.ParkingZoneRepository;
import com.smartparking.repository.PricingPolicyRepository;
import com.smartparking.repository.UserRepository;
import com.smartparking.repository.VehicleTypeRepository;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.smartparking.service.NullSafety.requireNonNull;

@Service
public class ManagerService {

    private final ParkingBuildingRepository buildingRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final ParkingZoneRepository zoneRepository;
    private final ParkingSlotRepository slotRepository;
    private final PricingPolicyRepository pricingPolicyRepository;
    private final ParkingIncidentRepository incidentRepository;
    private final UserRepository userRepository;
    private final ParkingSessionRepository parkingSessionRepository;

    public ManagerService(ParkingBuildingRepository buildingRepository,
                          VehicleTypeRepository vehicleTypeRepository,
                          ParkingZoneRepository zoneRepository,
                          ParkingSlotRepository slotRepository,
                          PricingPolicyRepository pricingPolicyRepository,
                          ParkingIncidentRepository incidentRepository,
                          UserRepository userRepository,
                          ParkingSessionRepository parkingSessionRepository) {
        this.buildingRepository = buildingRepository;
        this.vehicleTypeRepository = vehicleTypeRepository;
        this.zoneRepository = zoneRepository;
        this.slotRepository = slotRepository;
        this.pricingPolicyRepository = pricingPolicyRepository;
        this.incidentRepository = incidentRepository;
        this.userRepository = userRepository;
        this.parkingSessionRepository = parkingSessionRepository;
    }

    public ParkingBuilding createBuilding(ParkingBuilding building) {
        if (building.getStatus() == null || building.getStatus().isBlank()) {
            building.setStatus("ACTIVE");
        }
        return buildingRepository.save(building);
    }

    public ParkingBuilding updateBuilding(Long id, ParkingBuilding updated) {
        ParkingBuilding building = buildingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Building not found: " + id));
        building.setName(updated.getName());
        building.setAddress(updated.getAddress());
        building.setDescription(updated.getDescription());
        building.setStatus(updated.getStatus());
        return buildingRepository.save(building);
    }

    public VehicleType createVehicleType(VehicleType vehicleType) {
        if (vehicleTypeRepository.existsByName(vehicleType.getName())) {
            throw new IllegalArgumentException("Vehicle type name already exists");
        }
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
        return zoneRepository.save(zone);
    }

    public ParkingZone updateZone(Long id, ParkingZone updated) {
        ParkingZone zone = zoneRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Parking zone not found: " + id));
        zone.setFloorId(updated.getFloorId());
        zone.setVehicleTypeId(updated.getVehicleTypeId());
        zone.setZoneName(updated.getZoneName());
        return zoneRepository.save(zone);
    }

    public ParkingSlot createSlot(ParkingSlot slot) {
        slotRepository.findBySlotCode(slot.getSlotCode()).ifPresent(existing -> {
            throw new IllegalArgumentException("Slot code already exists");
        });
        if (slot.getStatus() == null || slot.getStatus().isBlank()) {
            slot.setStatus(ParkingSlotStatus.AVAILABLE.name());
        }
        return slotRepository.save(slot);
    }

    public ParkingSlot updateSlot(Long id, ParkingSlot updated) {
        ParkingSlot slot = slotRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Parking slot not found: " + id));
        slot.setZoneId(updated.getZoneId());
        slot.setSlotCode(updated.getSlotCode());
        slot.setStatus(updated.getStatus());
        return slotRepository.save(slot);
    }

    public ParkingSlot updateSlotStatus(Long id, SlotStatusUpdateRequest request) {
        ParkingSlot slot = slotRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Parking slot not found: " + id));
        slot.setStatus(request.getStatus().name());
        return slotRepository.save(slot);
    }

    public PricingPolicy createPricingPolicy(PricingPolicy policy) {
        if (policy.getStatus() == null || policy.getStatus().isBlank()) {
            policy.setStatus("ACTIVE");
        }
        return pricingPolicyRepository.save(policy);
    }

    public PricingPolicy updatePricingPolicy(Long id, PricingPolicy updated) {
        PricingPolicy policy = pricingPolicyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pricing policy not found: " + id));
        policy.setVehicleTypeId(updated.getVehicleTypeId());
        policy.setPolicyName(updated.getPolicyName());
        policy.setHourlyRate(updated.getHourlyRate());
        policy.setDailyRate(updated.getDailyRate());
        policy.setLostTicketFee(updated.getLostTicketFee());
        policy.setOvertimeFee(updated.getOvertimeFee());
        policy.setEffectiveFrom(updated.getEffectiveFrom());
        policy.setEffectiveTo(updated.getEffectiveTo());
        policy.setStatus(updated.getStatus());
        return pricingPolicyRepository.save(policy);
    }

    public ParkingIncident createIncident(ParkingIncident incident) {
        incident.setCreatedAt(LocalDateTime.now());
        if (incident.getCreatedBy() == null) {
            incident.setCreatedBy(incident.getReportedBy());
        }
        if (incident.getStatus() == null || incident.getStatus().isBlank()) {
            incident.setStatus("OPEN");
        }
        return incidentRepository.save(incident);
    }

    public ParkingIncident resolveIncident(Long id) {
        ParkingIncident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found: " + id));
        incident.setStatus("RESOLVED");
        incident.setResolvedAt(LocalDateTime.now());
        return incidentRepository.save(incident);
    }

    public User updateUserRole(Long id, @Nullable UserRoleUpdateRequest request) {
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

    public User updateUserStatus(Long id, @Nullable UserStatusUpdateRequest request) {
        if (request == null || request.getStatus() == null || request.getStatus().isBlank()) {
            throw new IllegalArgumentException("Status is required");
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        user.setStatus(request.getStatus().trim().toUpperCase());
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
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
        List<VehicleTypeReport> reports = requireNonNull(vehicleTypeRepository.findAll()).stream()
                .map(vehicleType -> {
                    List<ParkingSlot> slots = requireNonNull(zoneRepository.findByVehicleTypeId(vehicleType.getVehicleTypeId()))
                            .stream()
                            .flatMap(zone -> requireNonNull(slotRepository.findByZoneId(zone.getZoneId())).stream())
                            .toList();
                    long availableCount = slots.stream()
                            .filter(slot -> ParkingSlotStatus.AVAILABLE.name().equals(slot.getStatus()))
                            .count();
                    return new VehicleTypeReport(
                            vehicleType.getVehicleTypeId(),
                            vehicleType.getName(),
                            slots.size(),
                            availableCount,
                            BigDecimal.ZERO
                    );
                })
                .toList();
        return requireNonNull(reports);
    }
}
