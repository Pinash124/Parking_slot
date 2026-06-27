package com.example.pricing_calculation.service;

import com.example.pricing_calculation.domain.*;
import com.example.pricing_calculation.dto.ManagementDtos.*;
import com.example.pricing_calculation.repository.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UnifiedManagementService {
    private static final List<String> SLOT_STATUSES = List.of("AVAILABLE", "OCCUPIED", "RESERVED", "MAINTENANCE", "LOCKED");
    private final BuildingRepository buildings; private final FloorRepository floors;
    private final PaymentModuleVehicleTypeRepository vehicleTypes; private final ZoneRepository zones;
    private final PaymentModuleParkingSlotRepository slots; private final PaymentModulePricingPolicyRepository policies;
    private final AdditionalServiceRepository services;

    public UnifiedManagementService(BuildingRepository buildings, FloorRepository floors,
            PaymentModuleVehicleTypeRepository vehicleTypes, ZoneRepository zones,
            PaymentModuleParkingSlotRepository slots, PaymentModulePricingPolicyRepository policies,
            AdditionalServiceRepository services) {
        this.buildings=buildings; this.floors=floors; this.vehicleTypes=vehicleTypes; this.zones=zones;
        this.slots=slots; this.policies=policies; this.services=services;
    }

    @Transactional(readOnly=true) public List<BuildingView> buildings(){return buildings.findAll().stream().map(BuildingView::from).toList();}
    @Transactional public BuildingView saveBuilding(Long id, BuildingRequest r){require(r!=null && text(r.name()),"name is required");Building x=id==null?new Building():building(id);x.setName(r.name().trim());x.setAddress(r.address());x.setStatus(normal(r.status(),"ACTIVE"));return BuildingView.from(buildings.save(x));}
    @Transactional public void deleteBuilding(Long id){buildings.delete(building(id));}
    @Transactional(readOnly=true) public List<FloorView> floors(Long buildingId){return (buildingId==null?floors.findAll():floors.findByBuildingIdOrderByFloorNumberAsc(buildingId)).stream().map(FloorView::from).toList();}
    @Transactional public FloorView saveFloor(Long id, FloorRequest r){require(r!=null&&r.buildingId()!=null&&text(r.floorName()),"buildingId and floorName are required");Floor x=id==null?new Floor():floor(id);x.setBuilding(building(r.buildingId()));x.setFloorName(r.floorName().trim());x.setFloorNumber(r.floorNumber());return FloorView.from(floors.save(x));}
    @Transactional public void deleteFloor(Long id){floors.delete(floor(id));}
    @Transactional(readOnly=true) public List<VehicleTypeView> vehicleTypes(){return vehicleTypes.findAll().stream().map(VehicleTypeView::from).toList();}
    @Transactional public VehicleTypeView saveVehicleType(Long id, VehicleTypeRequest r){require(r!=null&&text(r.name()),"name is required");VehicleTypeEntity x=id==null?new VehicleTypeEntity():vehicleType(id);x.setName(r.name().trim());x.setDescription(r.description());x.setDefaultHourlyFee(nonNegative(r.defaultHourlyFee()));return VehicleTypeView.from(vehicleTypes.save(x));}
    @Transactional public void deleteVehicleType(Long id){vehicleTypes.delete(vehicleType(id));}
    @Transactional(readOnly=true) public List<ZoneView> zones(Long floorId,Long vehicleTypeId){List<Zone> list=floorId!=null?zones.findByFloorId(floorId):vehicleTypeId!=null?zones.findByVehicleTypeId(vehicleTypeId):zones.findAll();return list.stream().map(ZoneView::from).toList();}
    @Transactional public ZoneView saveZone(Long id, ZoneRequest r){require(r!=null&&r.floorId()!=null&&r.vehicleTypeId()!=null&&text(r.zoneName()),"floorId, vehicleTypeId and zoneName are required");Zone x=id==null?new Zone():zone(id);x.setFloor(floor(r.floorId()));x.setVehicleType(vehicleType(r.vehicleTypeId()));x.setZoneName(r.zoneName().trim());return ZoneView.from(zones.save(x));}
    @Transactional public void deleteZone(Long id){zones.delete(zone(id));}
    @Transactional(readOnly=true) public List<SlotView> slots(Long zoneId,Long vehicleTypeId,String status){return slots.findAll().stream().filter(x->zoneId==null||x.getZone().getId().equals(zoneId)).filter(x->vehicleTypeId==null||x.getZone().getVehicleType().getId().equals(vehicleTypeId)).filter(x->!text(status)||status.equalsIgnoreCase(x.getStatus())).map(SlotView::from).toList();}
    @Transactional public SlotView saveSlot(Long id, SlotRequest r){require(r!=null&&r.zoneId()!=null&&text(r.slotCode()),"zoneId and slotCode are required");PaymentModuleParkingSlot x=id==null?new PaymentModuleParkingSlot():slot(id);x.setZone(zone(r.zoneId()));x.setSlotCode(r.slotCode());x.setStatus(slotStatus(r.status()));return SlotView.from(slots.save(x));}
    @Transactional public SlotView updateSlotStatus(Long id,String status){PaymentModuleParkingSlot x=slot(id);x.setStatus(slotStatus(status));return SlotView.from(slots.save(x));}
    @Transactional public void deleteSlot(Long id){slots.delete(slot(id));}
    @Transactional(readOnly=true) public List<PricingPolicyView> policies(){return policies.findAll().stream().map(PricingPolicyView::from).toList();}
    @Transactional public PricingPolicyView savePolicy(Long id,PricingPolicyRequest r){require(r!=null&&r.vehicleTypeId()!=null&&text(r.policyName()),"vehicleTypeId and policyName are required");PaymentModulePricingPolicy x=id==null?new PaymentModulePricingPolicy():policy(id);x.setVehicleType(vehicleType(r.vehicleTypeId()));x.setPolicyName(r.policyName());x.setHourlyRate(nonNegative(r.hourlyRate()));x.setDailyRate(nonNegative(r.dailyRate()));x.setMonthlyRate(nonNegative(r.monthlyRate()));x.setFixedSurcharge(nonNegative(r.fixedSurcharge()));x.setLostTicketFee(nonNegative(r.lostTicketFee()));x.setOvertimeFee(nonNegative(r.overtimeFee()));x.setEffectiveFrom(r.effectiveFrom());x.setEffectiveTo(r.effectiveTo());x.setStatus(normal(r.status(),"ACTIVE"));return PricingPolicyView.from(policies.save(x));}
    @Transactional public void deletePolicy(Long id){policies.delete(policy(id));}
    @Transactional(readOnly=true) public List<AdditionalServiceView> additionalServices(){return services.findAll().stream().map(AdditionalServiceView::from).toList();}
    @Transactional public AdditionalServiceView saveAdditionalService(Long id,AdditionalServiceRequest r){require(r!=null&&text(r.name()),"name is required");AdditionalService x=id==null?new AdditionalService():additionalService(id);x.setName(r.name().trim());x.setPrice(nonNegative(r.price()));x.setStatus(normal(r.status(),"ACTIVE"));return AdditionalServiceView.from(services.save(x));}
    @Transactional public void deleteAdditionalService(Long id){services.delete(additionalService(id));}

    private Building building(Long id){return buildings.findById(id).orElseThrow(()->new ResourceNotFoundException("Building not found: "+id));}
    private Floor floor(Long id){return floors.findById(id).orElseThrow(()->new ResourceNotFoundException("Floor not found: "+id));}
    private VehicleTypeEntity vehicleType(Long id){return vehicleTypes.findById(id).orElseThrow(()->new ResourceNotFoundException("Vehicle type not found: "+id));}
    private Zone zone(Long id){return zones.findById(id).orElseThrow(()->new ResourceNotFoundException("Zone not found: "+id));}
    private PaymentModuleParkingSlot slot(Long id){return slots.findById(id).orElseThrow(()->new ResourceNotFoundException("Slot not found: "+id));}
    private PaymentModulePricingPolicy policy(Long id){return policies.findById(id).orElseThrow(()->new ResourceNotFoundException("Policy not found: "+id));}
    private AdditionalService additionalService(Long id){return services.findById(id).orElseThrow(()->new ResourceNotFoundException("Additional service not found: "+id));}
    private String slotStatus(String value){String v=normal(value,"AVAILABLE");require(SLOT_STATUSES.contains(v),"status must be one of "+SLOT_STATUSES);return v;}
    private String normal(String v,String d){return text(v)?v.trim().toUpperCase(Locale.ROOT):d;}
    private boolean text(String v){return v!=null&&!v.isBlank();}
    private BigDecimal nonNegative(BigDecimal v){if(v==null)return BigDecimal.ZERO;require(v.signum()>=0,"amount cannot be negative");return v;}
    private void require(boolean ok,String message){if(!ok)throw new BadRequestException(message);}
}
