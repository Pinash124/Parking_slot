package com.smartparking.controller;

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
import com.smartparking.model.schemas.SystemConfig;
import com.smartparking.model.schemas.User;
import com.smartparking.model.schemas.VehicleType;
import com.smartparking.repository.ParkingBuildingRepository;
import com.smartparking.repository.ParkingIncidentRepository;
import com.smartparking.repository.ParkingSlotRepository;
import com.smartparking.repository.ParkingZoneRepository;
import com.smartparking.repository.PricingPolicyRepository;
import com.smartparking.repository.SystemConfigRepository;
import com.smartparking.repository.UserRepository;
import com.smartparking.repository.VehicleTypeRepository;
import com.smartparking.service.ManagerService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/manager")
public class ManagerController {

    private final ManagerService managerService;
    private final ParkingBuildingRepository buildingRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final ParkingZoneRepository zoneRepository;
    private final ParkingSlotRepository slotRepository;
    private final PricingPolicyRepository pricingPolicyRepository;
    private final ParkingIncidentRepository incidentRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final UserRepository userRepository;

    public ManagerController(ManagerService managerService,
                             ParkingBuildingRepository buildingRepository,
                             VehicleTypeRepository vehicleTypeRepository,
                             ParkingZoneRepository zoneRepository,
                             ParkingSlotRepository slotRepository,
                             PricingPolicyRepository pricingPolicyRepository,
                             ParkingIncidentRepository incidentRepository,
                             SystemConfigRepository systemConfigRepository,
                             UserRepository userRepository) {
        this.managerService = managerService;
        this.buildingRepository = buildingRepository;
        this.vehicleTypeRepository = vehicleTypeRepository;
        this.zoneRepository = zoneRepository;
        this.slotRepository = slotRepository;
        this.pricingPolicyRepository = pricingPolicyRepository;
        this.incidentRepository = incidentRepository;
        this.systemConfigRepository = systemConfigRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/buildings")
    public List<ParkingBuilding> getBuildings() {
        return buildingRepository.findAll();
    }

    @PostMapping("/buildings")
    public ResponseEntity<ParkingBuilding> createBuilding(@RequestBody ParkingBuilding building) {
        return ResponseEntity.status(HttpStatus.CREATED).body(managerService.createBuilding(building));
    }

    @PutMapping("/buildings/{id}")
    public ParkingBuilding updateBuilding(@PathVariable Long id, @RequestBody ParkingBuilding building) {
        return managerService.updateBuilding(id, building);
    }

    @DeleteMapping("/buildings/{id}")
    public ResponseEntity<Void> deleteBuilding(@PathVariable Long id) {
        buildingRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/vehicle-types")
    public List<VehicleType> getVehicleTypes() {
        return vehicleTypeRepository.findAll();
    }

    @PostMapping("/vehicle-types")
    public ResponseEntity<VehicleType> createVehicleType(@RequestBody VehicleType vehicleType) {
        return ResponseEntity.status(HttpStatus.CREATED).body(managerService.createVehicleType(vehicleType));
    }

    @PutMapping("/vehicle-types/{id}")
    public VehicleType updateVehicleType(@PathVariable Long id, @RequestBody VehicleType vehicleType) {
        return managerService.updateVehicleType(id, vehicleType);
    }

    @DeleteMapping("/vehicle-types/{id}")
    public ResponseEntity<Void> deleteVehicleType(@PathVariable Long id) {
        vehicleTypeRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/zones")
    public List<ParkingZone> getZones(@Nullable @RequestParam(required = false) Long buildingId,
                                      @Nullable @RequestParam(required = false) Long vehicleTypeId) {
        if (buildingId != null) {
            return zoneRepository.findByBuildingId(buildingId);
        }
        if (vehicleTypeId != null) {
            return zoneRepository.findByVehicleTypeId(vehicleTypeId);
        }
        return zoneRepository.findAll();
    }

    @PostMapping("/zones")
    public ResponseEntity<ParkingZone> createZone(@RequestBody ParkingZone zone) {
        return ResponseEntity.status(HttpStatus.CREATED).body(managerService.createZone(zone));
    }

    @PutMapping("/zones/{id}")
    public ParkingZone updateZone(@PathVariable Long id, @RequestBody ParkingZone zone) {
        return managerService.updateZone(id, zone);
    }

    @DeleteMapping("/zones/{id}")
    public ResponseEntity<Void> deleteZone(@PathVariable Long id) {
        zoneRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/slots")
    public List<ParkingSlot> getSlots(@Nullable @RequestParam(required = false) String status,
                                      @Nullable @RequestParam(required = false) Long vehicleTypeId) {
        if (status != null && !status.isBlank()) {
            return slotRepository.findByStatus(status);
        }
        if (vehicleTypeId != null) {
            return slotRepository.findByVehicleTypeId(vehicleTypeId);
        }
        return slotRepository.findAll();
    }

    @PostMapping("/slots")
    public ResponseEntity<ParkingSlot> createSlot(@RequestBody ParkingSlot slot) {
        return ResponseEntity.status(HttpStatus.CREATED).body(managerService.createSlot(slot));
    }

    @PutMapping("/slots/{id}")
    public ParkingSlot updateSlot(@PathVariable Long id, @RequestBody ParkingSlot slot) {
        return managerService.updateSlot(id, slot);
    }

    @PatchMapping("/slots/{id}/status")
    public ParkingSlot updateSlotStatus(@PathVariable Long id, @Valid @RequestBody SlotStatusUpdateRequest request) {
        return managerService.updateSlotStatus(id, request);
    }

    @DeleteMapping("/slots/{id}")
    public ResponseEntity<Void> deleteSlot(@PathVariable Long id) {
        slotRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/pricing-policies")
    public List<PricingPolicy> getPricingPolicies(@Nullable @RequestParam(required = false) Long vehicleTypeId) {
        if (vehicleTypeId != null) {
            return pricingPolicyRepository.findByVehicleTypeId(vehicleTypeId);
        }
        return pricingPolicyRepository.findAll();
    }

    @PostMapping("/pricing-policies")
    public ResponseEntity<PricingPolicy> createPricingPolicy(@RequestBody PricingPolicy policy) {
        return ResponseEntity.status(HttpStatus.CREATED).body(managerService.createPricingPolicy(policy));
    }

    @PutMapping("/pricing-policies/{id}")
    public PricingPolicy updatePricingPolicy(@PathVariable Long id, @RequestBody PricingPolicy policy) {
        return managerService.updatePricingPolicy(id, policy);
    }

    @DeleteMapping("/pricing-policies/{id}")
    public ResponseEntity<Void> deletePricingPolicy(@PathVariable Long id) {
        pricingPolicyRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/reports/overview")
    public ManagerOverviewReport getOverviewReport(
            @Nullable @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Nullable @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return managerService.getOverviewReport(from, to);
    }

    @GetMapping("/reports/vehicle-types")
    public List<VehicleTypeReport> getVehicleTypeReports() {
        return managerService.getVehicleTypeReports();
    }

    @GetMapping("/incidents")
    public List<ParkingIncident> getIncidents(@Nullable @RequestParam(required = false) String status) {
        if (status != null && !status.isBlank()) {
            return incidentRepository.findByStatus(status);
        }
        return incidentRepository.findAll();
    }

    @PostMapping("/incidents")
    public ResponseEntity<ParkingIncident> createIncident(@RequestBody ParkingIncident incident) {
        return ResponseEntity.status(HttpStatus.CREATED).body(managerService.createIncident(incident));
    }

    @PatchMapping("/incidents/{id}/resolve")
    public ParkingIncident resolveIncident(@PathVariable Long id, @RequestParam Long managerId) {
        return managerService.resolveIncident(id, managerId);
    }

    @GetMapping("/users")
    public List<User> getUsers() {
        return userRepository.findAll();
    }

    @PatchMapping("/users/{id}/role")
    public User updateUserRole(@PathVariable Long id, @Valid @RequestBody UserRoleUpdateRequest request) {
        return managerService.updateUserRole(id, request);
    }

    @PatchMapping("/users/{id}/status")
    public User updateUserStatus(@PathVariable Long id, @Valid @RequestBody UserStatusUpdateRequest request) {
        return managerService.updateUserStatus(id, request);
    }

    @GetMapping("/system-configs")
    public List<SystemConfig> getSystemConfigs() {
        return systemConfigRepository.findAll();
    }

    @PostMapping("/system-configs")
    public SystemConfig saveSystemConfig(@RequestBody SystemConfig config) {
        return managerService.saveSystemConfig(config);
    }
}
