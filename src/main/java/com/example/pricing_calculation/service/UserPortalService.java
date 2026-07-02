package com.example.pricing_calculation.service;

import com.example.pricing_calculation.domain.*;
import com.example.pricing_calculation.dto.CurrentParkingSessionResponse;
import com.example.pricing_calculation.dto.CurrentParkingSessionResponse.ServiceUsageView;
import com.example.pricing_calculation.dto.MonthlyParkingPassDtos.MonthlyParkingPassCreateRequest;
import com.example.pricing_calculation.dto.MonthlyParkingPassDtos.MonthlyParkingPassResponse;
import com.example.pricing_calculation.dto.ManagementDtos.VehicleRequest;
import com.example.pricing_calculation.dto.ManagementDtos.VehicleView;
import com.example.pricing_calculation.dto.ParkingSessionResponse;
import com.example.pricing_calculation.dto.PricingQuoteResponse;
import com.example.pricing_calculation.dto.ServiceUsageRequest;
import com.example.pricing_calculation.repository.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserPortalService {
    private final VehicleRepository vehicles; private final PaymentModuleVehicleTypeRepository vehicleTypes;
    private final PaymentModuleParkingSessionRepository sessions; private final PricingService pricing;
    private final MonthlyParkingPassRepository monthlyPasses;
    private final AdditionalServiceRepository services; private final SessionServiceUsageRepository usages;
    public UserPortalService(VehicleRepository vehicles,PaymentModuleVehicleTypeRepository vehicleTypes,
            PaymentModuleParkingSessionRepository sessions,PricingService pricing,
            MonthlyParkingPassRepository monthlyPasses,AdditionalServiceRepository services,SessionServiceUsageRepository usages){this.vehicles=vehicles;this.vehicleTypes=vehicleTypes;this.sessions=sessions;this.pricing=pricing;this.monthlyPasses=monthlyPasses;this.services=services;this.usages=usages;}

    @Transactional(readOnly=true) public List<VehicleView> vehicles(UserAccount user){return vehicles.findByUserIdOrderByPlateNumberAsc(user.getId()).stream().map(VehicleView::from).toList();}
    @Transactional public VehicleView saveVehicle(UserAccount user,Long id,VehicleRequest r){if(r==null||r.vehicleTypeId()==null||r.plateNumber()==null||r.plateNumber().isBlank())throw new BadRequestException("vehicleTypeId and plateNumber are required");Vehicle x=id==null?new Vehicle():ownedVehicle(user,id);vehicles.findByPlateNumberIgnoreCase(r.plateNumber()).filter(v->id==null||!v.getId().equals(id)).ifPresent(v->{throw new BadRequestException("License plate already exists");});x.setUser(user);x.setVehicleType(vehicleTypes.findById(r.vehicleTypeId()).orElseThrow(()->new ResourceNotFoundException("Vehicle type not found: "+r.vehicleTypeId())));x.setPlateNumber(r.plateNumber());x.setBrand(r.brand());x.setColor(r.color());x.setStatus("ACTIVE");return VehicleView.from(vehicles.save(x));}
    @Transactional public void deleteVehicle(UserAccount user,Long id){vehicles.delete(ownedVehicle(user,id));}

    @Transactional(readOnly=true)
    public List<MonthlyParkingPassResponse> monthlyPasses(UserAccount user) {
        return monthlyPasses.findByVehicleUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(MonthlyParkingPassResponse::from)
                .toList();
    }

    @Transactional
    public MonthlyParkingPassResponse registerMonthlyPass(UserAccount user, MonthlyParkingPassCreateRequest request) {
        if (request == null || request.vehicleId() == null) {
            throw new BadRequestException("vehicleId is required");
        }
        Vehicle vehicle = ownedVehicle(user, request.vehicleId());
        int months = request.months() == null ? 1 : request.months();
        if (months < 1 || months > 12) {
            throw new BadRequestException("months must be between 1 and 12");
        }
        LocalDate startDate = request.startDate() == null ? LocalDate.now() : request.startDate();
        LocalDate endDate = startDate.plusMonths(months).minusDays(1);
        boolean hasActive = monthlyPasses.findByVehicleIdOrderByCreatedAtDesc(vehicle.getId()).stream()
                .anyMatch(pass -> pass.isActiveAt(startDate));
        if (hasActive) {
            throw new BadRequestException("Vehicle already has an active monthly pass");
        }

        BigDecimal monthlyRate = pricing.monthlyRateForVehicleType(vehicle.getVehicleType().getId(), startDate.atStartOfDay());
        LocalDateTime now = LocalDateTime.now();
        MonthlyParkingPass pass = new MonthlyParkingPass();
        pass.setUser(user);
        pass.setVehicle(vehicle);
        pass.setVehicleType(vehicle.getVehicleType());
        pass.setMonths(months);
        pass.setMonthlyRate(monthlyRate);
        pass.setTotalAmount(monthlyRate.multiply(BigDecimal.valueOf(months)).setScale(2, java.math.RoundingMode.HALF_UP));
        pass.setStartDate(startDate);
        pass.setEndDate(endDate);
        pass.setStatus(startDate.isAfter(LocalDate.now()) ? "SCHEDULED" : "ACTIVE");
        pass.setNote(request.note());
        pass.setCreatedAt(now);
        pass.setUpdatedAt(now);
        return MonthlyParkingPassResponse.from(monthlyPasses.save(pass));
    }

    @Transactional(readOnly=true) public CurrentParkingSessionResponse current(UserAccount user){PaymentModuleParkingSession s=sessions.findFirstByVehicleUserIdAndStatusInOrderByEntryTimeDesc(user.getId(),List.of("ACTIVE","PAYMENT_PENDING","CHECKED_OUT")).orElseThrow(()->new ResourceNotFoundException("No active parking session"));return currentResponse(s);}
    @Transactional(readOnly=true) public List<ParkingSessionResponse> history(UserAccount user){return sessions.findByVehicleUserIdOrderByEntryTimeDesc(user.getId()).stream().map(ParkingSessionResponse::from).toList();}
    @Transactional public CurrentParkingSessionResponse addService(UserAccount user,Long sessionId,ServiceUsageRequest r){PaymentModuleParkingSession s=ownedSession(user,sessionId);if(!"ACTIVE".equalsIgnoreCase(s.getStatus()))throw new BadRequestException("Additional services can only be added to ACTIVE sessions");if(r==null||r.serviceId()==null)throw new BadRequestException("serviceId is required");AdditionalService service=services.findById(r.serviceId()).orElseThrow(()->new ResourceNotFoundException("Additional service not found: "+r.serviceId()));if(!"ACTIVE".equalsIgnoreCase(service.getStatus()))throw new BadRequestException("Additional service is not active");SessionServiceUsage u=new SessionServiceUsage();u.setSession(s);u.setService(service);u.setQuantity(r.quantity()==null?1:Math.max(1,r.quantity()));u.setUnitPrice(service.getPrice());usages.save(u);return currentResponse(s);}

    private CurrentParkingSessionResponse currentResponse(PaymentModuleParkingSession s){PricingQuoteResponse quote=null;if("ACTIVE".equalsIgnoreCase(s.getStatus())){LocalDateTime now=LocalDateTime.now();if(!now.isAfter(s.getEntryTime()))now=s.getEntryTime().plusSeconds(1);quote=pricing.estimateForVehicle(s.getVehicle().getId(),s.getEntryTime(),now,false,0);}List<ServiceUsageView> list=usages.findBySessionId(s.getId()).stream().map(u->new ServiceUsageView(u.getId(),u.getService().getId(),u.getService().getName(),u.getQuantity(),u.getUnitPrice(),u.getUnitPrice().multiply(BigDecimal.valueOf(u.getQuantity())))).toList();BigDecimal extra=list.stream().map(ServiceUsageView::lineTotal).reduce(BigDecimal.ZERO,BigDecimal::add);BigDecimal base=quote==null?(s.getTotalFee()==null?BigDecimal.ZERO:s.getTotalFee()):quote.totalFee();return new CurrentParkingSessionResponse(ParkingSessionResponse.from(s),quote,list,extra,base.add(extra));}
    private Vehicle ownedVehicle(UserAccount u,Long id){Vehicle v=vehicles.findById(id).orElseThrow(()->new ResourceNotFoundException("Vehicle not found: "+id));if(!v.getUser().getId().equals(u.getId()))throw new ForbiddenException("Vehicle does not belong to current user");return v;}
    private PaymentModuleParkingSession ownedSession(UserAccount u,Long id){PaymentModuleParkingSession s=sessions.findById(id).orElseThrow(()->new ResourceNotFoundException("Session not found: "+id));if(!s.getVehicle().getUser().getId().equals(u.getId()))throw new ForbiddenException("Session does not belong to current user");return s;}
}
