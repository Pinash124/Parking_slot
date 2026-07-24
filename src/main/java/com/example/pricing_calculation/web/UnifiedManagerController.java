package com.example.pricing_calculation.web;

import static com.example.pricing_calculation.dto.ManagementDtos.*;
import com.example.pricing_calculation.domain.UserRole;
import com.example.pricing_calculation.repository.VehicleRepository;
import com.example.pricing_calculation.service.PaymentModuleAuthService;
import com.example.pricing_calculation.service.MonthlyParkingPassService;
import com.example.pricing_calculation.service.UnifiedManagementService;
import com.example.pricing_calculation.dto.MonthlyParkingPassDtos;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/manager")
@SecurityRequirement(name="bearerAuth")
@Tag(name="Parking Manager")
public class UnifiedManagerController {
    private final UnifiedManagementService service; private final PaymentModuleAuthService auth; private final VehicleRepository vehicleRepo; private final MonthlyParkingPassService monthlyPasses;
    public UnifiedManagerController(UnifiedManagementService service,PaymentModuleAuthService auth,VehicleRepository vehicleRepo,MonthlyParkingPassService monthlyPasses){this.service=service;this.auth=auth;this.vehicleRepo=vehicleRepo;this.monthlyPasses=monthlyPasses;}
    private void manager(String h){auth.requireAnyRole(h,UserRole.PARKING_MANAGER,UserRole.ADMINISTRATOR);}
    private void monthlyPassAdmin(String h){auth.requireAnyRole(h,UserRole.ADMINISTRATOR);}

    @GetMapping("/buildings") public List<BuildingView> buildings(@RequestHeader("Authorization")String h){manager(h);return service.buildings();}
    @PostMapping("/buildings") public ResponseEntity<BuildingView> createBuilding(@RequestHeader("Authorization")String h,@RequestBody BuildingRequest r){manager(h);return ResponseEntity.status(HttpStatus.CREATED).body(service.saveBuilding(null,r));}
    @PutMapping("/buildings/{id}") public BuildingView updateBuilding(@RequestHeader("Authorization")String h,@PathVariable Long id,@RequestBody BuildingRequest r){manager(h);return service.saveBuilding(id,r);}
    @DeleteMapping("/buildings/{id}") public void deleteBuilding(@RequestHeader("Authorization")String h,@PathVariable Long id){manager(h);service.deleteBuilding(id);}

    @GetMapping("/floors") public List<FloorView> floors(@RequestHeader("Authorization")String h,@RequestParam(required=false)Long buildingId){manager(h);return service.floors(buildingId);}
    @PostMapping("/floors") public FloorView createFloor(@RequestHeader("Authorization")String h,@RequestBody FloorRequest r){manager(h);return service.saveFloor(null,r);}
    @PutMapping("/floors/{id}") public FloorView updateFloor(@RequestHeader("Authorization")String h,@PathVariable Long id,@RequestBody FloorRequest r){manager(h);return service.saveFloor(id,r);}
    @DeleteMapping("/floors/{id}") public void deleteFloor(@RequestHeader("Authorization")String h,@PathVariable Long id){manager(h);service.deleteFloor(id);}

    @GetMapping("/vehicle-types") public List<VehicleTypeView> vehicleTypes(@RequestHeader("Authorization")String h){manager(h);return service.vehicleTypes();}
    @PostMapping("/vehicle-types") public VehicleTypeView createVehicleType(@RequestHeader("Authorization")String h,@RequestBody VehicleTypeRequest r){manager(h);return service.saveVehicleType(null,r);}
    @PutMapping("/vehicle-types/{id}") public VehicleTypeView updateVehicleType(@RequestHeader("Authorization")String h,@PathVariable Long id,@RequestBody VehicleTypeRequest r){manager(h);return service.saveVehicleType(id,r);}
    @DeleteMapping("/vehicle-types/{id}") public void deleteVehicleType(@RequestHeader("Authorization")String h,@PathVariable Long id){manager(h);service.deleteVehicleType(id);}

    @GetMapping("/zones") public List<ZoneView> zones(@RequestHeader("Authorization")String h,@RequestParam(required=false)Long floorId,@RequestParam(required=false)Long vehicleTypeId){manager(h);return service.zones(floorId,vehicleTypeId);}
    @PostMapping("/zones") public ZoneView createZone(@RequestHeader("Authorization")String h,@RequestBody ZoneRequest r){manager(h);return service.saveZone(null,r);}
    @PutMapping("/zones/{id}") public ZoneView updateZone(@RequestHeader("Authorization")String h,@PathVariable Long id,@RequestBody ZoneRequest r){manager(h);return service.saveZone(id,r);}
    @DeleteMapping("/zones/{id}") public void deleteZone(@RequestHeader("Authorization")String h,@PathVariable Long id){manager(h);service.deleteZone(id);}
    @PostMapping("/zones/rebalance-car") public List<ZoneView> rebalanceCarZones(@RequestHeader("Authorization")String h,@RequestParam Long floorId){manager(h);return service.rebalanceCarSlots(floorId);}

    @GetMapping("/slots") public List<SlotView> slots(@RequestHeader("Authorization")String h,@RequestParam(required=false)Long zoneId,@RequestParam(required=false)Long vehicleTypeId,@RequestParam(required=false)String status){manager(h);return service.slots(zoneId,vehicleTypeId,status);}
    @PostMapping("/slots") public SlotView createSlot(@RequestHeader("Authorization")String h,@RequestBody SlotRequest r){manager(h);return service.saveSlot(null,r);}
    @PutMapping("/slots/{id}") public SlotView updateSlot(@RequestHeader("Authorization")String h,@PathVariable Long id,@RequestBody SlotRequest r){manager(h);return service.saveSlot(id,r);}
    @PatchMapping("/slots/{id}/status") public SlotView updateSlotStatus(@RequestHeader("Authorization")String h,@PathVariable Long id,@RequestParam String status){manager(h);return service.updateSlotStatus(id,status);}
    @DeleteMapping("/slots/{id}") public void deleteSlot(@RequestHeader("Authorization")String h,@PathVariable Long id){manager(h);service.deleteSlot(id);}

    @GetMapping("/pricing-policies") public List<PricingPolicyView> policies(@RequestHeader("Authorization")String h){manager(h);return service.policies();}
    @PostMapping("/pricing-policies") public PricingPolicyView createPolicy(@RequestHeader("Authorization")String h,@RequestBody PricingPolicyRequest r){manager(h);return service.savePolicy(null,r);}
    @PutMapping("/pricing-policies/{id}") public PricingPolicyView updatePolicy(@RequestHeader("Authorization")String h,@PathVariable Long id,@RequestBody PricingPolicyRequest r){manager(h);return service.savePolicy(id,r);}
    @DeleteMapping("/pricing-policies/{id}") public void deletePolicy(@RequestHeader("Authorization")String h,@PathVariable Long id){manager(h);service.deletePolicy(id);}
    @GetMapping("/pricing-rules") public PricingRuleSettingsView pricingRules(@RequestHeader("Authorization")String h){manager(h);return service.pricingRules();}
    @PutMapping("/pricing-rules") public PricingRuleSettingsView savePricingRules(@RequestHeader("Authorization")String h,@RequestBody PricingRuleSettingsRequest r){manager(h);return service.savePricingRules(r);}

    @GetMapping("/additional-services") public List<AdditionalServiceView> additionalServices(@RequestHeader("Authorization")String h){manager(h);return service.additionalServices();}
    @PostMapping("/additional-services") public AdditionalServiceView createAdditionalService(@RequestHeader("Authorization")String h,@RequestBody AdditionalServiceRequest r){manager(h);return service.saveAdditionalService(null,r);}
    @PutMapping("/additional-services/{id}") public AdditionalServiceView updateAdditionalService(@RequestHeader("Authorization")String h,@PathVariable Long id,@RequestBody AdditionalServiceRequest r){manager(h);return service.saveAdditionalService(id,r);}
    @DeleteMapping("/additional-services/{id}") public void deleteAdditionalService(@RequestHeader("Authorization")String h,@PathVariable Long id){manager(h);service.deleteAdditionalService(id);}

    @GetMapping("/vehicles") public List<VehicleView> allVehicles(@RequestHeader("Authorization")String h){
        manager(h);
        return vehicleRepo.findAll().stream()
                .filter(vehicle -> !"DELETED".equalsIgnoreCase(vehicle.getStatus()))
                .map(VehicleView::from)
                .toList();
    }

    @GetMapping("/monthly-passes") public List<MonthlyParkingPassDtos.MonthlyParkingPassResponse> monthlyPasses(@RequestHeader("Authorization")String h){monthlyPassAdmin(h);return monthlyPasses.listAll();}
    @PostMapping("/monthly-passes/{id}/confirm-payment") public MonthlyParkingPassDtos.MonthlyParkingPassResponse confirmMonthlyPassPayment(@RequestHeader("Authorization")String h,@PathVariable Long id,@RequestBody(required=false) MonthlyParkingPassDtos.MonthlyParkingPassPaymentRequest r){monthlyPassAdmin(h);return monthlyPasses.confirmPayment(id,r);}
    @PostMapping("/monthly-passes/confirm-payment/scan") public MonthlyParkingPassDtos.MonthlyParkingPassResponse confirmMonthlyPassPaymentByQr(@RequestHeader("Authorization")String h,@RequestBody MonthlyParkingPassDtos.MonthlyParkingPassQrConfirmRequest r){monthlyPassAdmin(h);return monthlyPasses.confirmPaymentFromQr(r.qrContent(),r.referenceCode());}
    @PostMapping("/monthly-passes/{id}/cancel") public MonthlyParkingPassDtos.MonthlyParkingPassResponse cancelMonthlyPass(@RequestHeader("Authorization")String h,@PathVariable Long id){monthlyPassAdmin(h);return monthlyPasses.cancel(id);}
}
