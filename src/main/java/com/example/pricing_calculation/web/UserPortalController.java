package com.example.pricing_calculation.web;

import com.example.pricing_calculation.domain.UserAccount;
import com.example.pricing_calculation.domain.UserRole;
import com.example.pricing_calculation.dto.*;
import com.example.pricing_calculation.dto.ManagementDtos.BuildingView;
import com.example.pricing_calculation.dto.ManagementDtos.FloorView;
import com.example.pricing_calculation.dto.ManagementDtos.VehicleRequest;
import com.example.pricing_calculation.dto.ManagementDtos.VehicleView;
import com.example.pricing_calculation.dto.ManagementDtos.ZoneView;
import com.example.pricing_calculation.service.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/user") @SecurityRequirement(name="bearerAuth")
@Tag(name="Parking User",description="Phuong tien, dat cho, luot gui hien tai, lich su va dich vu bo sung")
public class UserPortalController {
    private final PaymentModuleAuthService auth;private final UserPortalService portal;private final ReservationService reservations;private final UnifiedManagementService catalog;
    public UserPortalController(PaymentModuleAuthService auth,UserPortalService portal,ReservationService reservations,UnifiedManagementService catalog){this.auth=auth;this.portal=portal;this.reservations=reservations;this.catalog=catalog;}
    private UserAccount user(String h){return auth.requireAnyRole(h,UserRole.PARKING_USER);}
    @GetMapping("/vehicles") public List<VehicleView> vehicles(@RequestHeader("Authorization")String h){return portal.vehicles(user(h));}
    @GetMapping("/buildings") public List<BuildingView> buildings(@RequestHeader("Authorization")String h){user(h);return catalog.buildings();}
    @GetMapping("/floors") public List<FloorView> floors(@RequestHeader("Authorization")String h,@RequestParam(required=false)Long buildingId){user(h);return catalog.floors(buildingId);}
    @GetMapping("/zones") public List<ZoneView> zones(@RequestHeader("Authorization")String h,@RequestParam(required=false)Long floorId,@RequestParam(defaultValue="RESERVATION")String purpose){user(h);return catalog.zonesForPurpose(floorId,purpose);}
    @PostMapping("/vehicles") public ResponseEntity<VehicleView> createVehicle(@RequestHeader("Authorization")String h,@RequestBody VehicleRequest r){return ResponseEntity.status(HttpStatus.CREATED).body(portal.saveVehicle(user(h),null,r));}
    @PutMapping("/vehicles/{id}") public VehicleView updateVehicle(@RequestHeader("Authorization")String h,@PathVariable Long id,@RequestBody VehicleRequest r){return portal.saveVehicle(user(h),id,r);}
    @DeleteMapping("/vehicles/{id}") public void deleteVehicle(@RequestHeader("Authorization")String h,@PathVariable Long id){portal.deleteVehicle(user(h),id);}
    @GetMapping("/monthly-passes") public List<MonthlyParkingPassDtos.MonthlyParkingPassResponse> monthlyPasses(@RequestHeader("Authorization")String h){return portal.monthlyPasses(user(h));}
    @PostMapping("/monthly-passes") public ResponseEntity<MonthlyParkingPassDtos.MonthlyParkingPassResponse> registerMonthlyPass(@RequestHeader("Authorization")String h,@RequestBody MonthlyParkingPassDtos.MonthlyParkingPassCreateRequest r){return ResponseEntity.status(HttpStatus.CREATED).body(portal.registerMonthlyPass(user(h),r));}
    @PostMapping("/monthly-passes/{id}/payment/online-qr") public MonthlyParkingPassDtos.MonthlyParkingPassPaymentInstructionResponse prepareMonthlyPassOnlinePayment(@RequestHeader("Authorization")String h,@PathVariable Long id){return portal.prepareMonthlyPassOnlinePayment(user(h),id);}
    @PostMapping("/reservations") public ReservationResponse reserve(@RequestHeader("Authorization")String h,@RequestBody ReservationCreateRequest r){UserAccount u=user(h);return reservations.create(new ReservationCreateRequest(u.getId(),r.vehicleId(),r.zoneId(),r.startTime(),r.endTime()));}
    @GetMapping("/reservations") public PageResponse<ReservationResponse> myReservations(@RequestHeader("Authorization")String h,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){return reservations.search(user(h).getId(),null,null,null,null,null,page,size);}
    @PatchMapping("/reservations/{id}/cancel") public ReservationResponse cancelReservation(@RequestHeader("Authorization")String h,@PathVariable Long id){return reservations.cancelForUser(id,user(h).getId());}
    @GetMapping("/parking-sessions/current") public List<CurrentParkingSessionResponse> current(@RequestHeader("Authorization")String h){return portal.current(user(h));}
    @GetMapping("/parking-sessions/history") public List<ParkingSessionResponse> history(@RequestHeader("Authorization")String h){return portal.history(user(h));}
    @PostMapping("/parking-sessions/{id}/additional-services") public CurrentParkingSessionResponse addService(@RequestHeader("Authorization")String h,@PathVariable Long id,@RequestBody ServiceUsageRequest r){return portal.addService(user(h),id,r);}
    @GetMapping("/pricing-policies") public List<com.example.pricing_calculation.dto.ManagementDtos.PricingPolicyView> pricingPolicies(@RequestHeader("Authorization")String h){return portal.pricingPolicies();}
    @GetMapping("/vehicle-types") public List<com.example.pricing_calculation.dto.ManagementDtos.VehicleTypeView> vehicleTypes(@RequestHeader("Authorization")String h){return portal.vehicleTypes();}
}
