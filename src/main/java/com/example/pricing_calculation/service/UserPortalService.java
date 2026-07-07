package com.example.pricing_calculation.service;

import com.example.pricing_calculation.domain.*;
import com.example.pricing_calculation.dto.CurrentParkingSessionResponse;
import com.example.pricing_calculation.dto.CurrentParkingSessionResponse.ServiceUsageView;
import com.example.pricing_calculation.dto.MonthlyParkingPassDtos.MonthlyParkingPassCreateRequest;
import com.example.pricing_calculation.dto.MonthlyParkingPassDtos.MonthlyParkingPassPaymentInstructionResponse;
import com.example.pricing_calculation.dto.MonthlyParkingPassDtos.MonthlyParkingPassResponse;
import com.example.pricing_calculation.dto.ManagementDtos.VehicleRequest;
import com.example.pricing_calculation.dto.ManagementDtos.VehicleView;
import com.example.pricing_calculation.dto.ParkingSessionResponse;
import com.example.pricing_calculation.dto.PricingQuoteResponse;
import com.example.pricing_calculation.dto.ServiceUsageRequest;
import com.example.pricing_calculation.repository.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserPortalService {
    private final VehicleRepository vehicles; private final PaymentModuleVehicleTypeRepository vehicleTypes;
    private final PaymentModuleParkingSessionRepository sessions; private final PricingService pricing;
    private final MonthlyParkingPassService monthlyPassService;
    private final AdditionalServiceRepository services; private final SessionServiceUsageRepository usages;
    private final PaymentModulePricingPolicyRepository pricingPolicies;
    private final QrCodeService qrCodeService;

    public UserPortalService(VehicleRepository vehicles,PaymentModuleVehicleTypeRepository vehicleTypes,
            PaymentModuleParkingSessionRepository sessions,PricingService pricing,
            MonthlyParkingPassService monthlyPassService,AdditionalServiceRepository services,
            SessionServiceUsageRepository usages, PaymentModulePricingPolicyRepository pricingPolicies,
            QrCodeService qrCodeService){
        this.vehicles=vehicles;
        this.vehicleTypes=vehicleTypes;
        this.sessions=sessions;
        this.pricing=pricing;
        this.monthlyPassService=monthlyPassService;
        this.services=services;
        this.usages=usages;
        this.pricingPolicies=pricingPolicies;
        this.qrCodeService=qrCodeService;
    }

    @Transactional(readOnly=true)
    public List<com.example.pricing_calculation.dto.ManagementDtos.PricingPolicyView> pricingPolicies() {
        return pricingPolicies.findAll().stream()
                .map(com.example.pricing_calculation.dto.ManagementDtos.PricingPolicyView::from)
                .toList();
    }

    @Transactional(readOnly=true)
    public List<com.example.pricing_calculation.dto.ManagementDtos.VehicleTypeView> vehicleTypes() {
        return vehicleTypes.findAll().stream()
                .map(com.example.pricing_calculation.dto.ManagementDtos.VehicleTypeView::from)
                .toList();
    }

    @Transactional(readOnly=true)
    public List<VehicleView> vehicles(UserAccount user) {
        return vehicles.findByUserIdOrderByPlateNumberAsc(user.getId()).stream()
                .filter(v -> !"DELETED".equalsIgnoreCase(v.getStatus()))
                .map(VehicleView::from)
                .toList();
    }

    @Transactional
    public VehicleView saveVehicle(UserAccount user, Long id, VehicleRequest r) {
        if (r == null || r.vehicleTypeId() == null || r.plateNumber() == null || r.plateNumber().isBlank()) {
            throw new BadRequestException("vehicleTypeId and plateNumber are required");
        }
        
        String cleanPlate = r.plateNumber().trim().toUpperCase();
        Vehicle existing = vehicles.findByPlateNumberIgnoreCase(cleanPlate).orElse(null);
        Vehicle x;
        
        if (id == null) {
            if (existing != null) {
                if ("DELETED".equalsIgnoreCase(existing.getStatus())) {
                    x = existing;
                } else {
                    throw new BadRequestException("License plate already exists");
                }
            } else {
                x = new Vehicle();
            }
        } else {
            x = ownedVehicle(user, id);
            if (existing != null && !existing.getId().equals(id)) {
                throw new BadRequestException("License plate already exists");
            }
        }
        
        x.setUser(user);
        x.setVehicleType(vehicleTypes.findById(r.vehicleTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle type not found: " + r.vehicleTypeId())));
        x.setPlateNumber(cleanPlate);
        x.setBrand(r.brand());
        x.setColor(r.color());
        x.setStatus("ACTIVE");
        
        Vehicle saved = vehicles.save(x);
        saved.setQrCode(qrCodeService.buildVehicleQrContent(saved));
        return VehicleView.from(vehicles.save(saved));
    }

    @Transactional
    public void deleteVehicle(UserAccount user, Long id) {
        Vehicle vehicle = ownedVehicle(user, id);
        vehicle.setStatus("DELETED");
        vehicles.save(vehicle);
    }

    @Transactional(readOnly=true)
    public List<MonthlyParkingPassResponse> monthlyPasses(UserAccount user) {
        return monthlyPassService.listForUser(user);
    }

    @Transactional
    public MonthlyParkingPassResponse registerMonthlyPass(UserAccount user, MonthlyParkingPassCreateRequest request) {
        return monthlyPassService.register(user, request);
    }

    @Transactional
    public MonthlyParkingPassPaymentInstructionResponse prepareMonthlyPassOnlinePayment(
            UserAccount user,
            Long id) {
        return monthlyPassService.prepareOnlinePayment(user, id);
    }

    @Transactional(readOnly=true)
    public List<CurrentParkingSessionResponse> current(UserAccount user) {
        List<String> statuses = List.of("ACTIVE");
        return sessions.findByVehicleUserIdOrderByEntryTimeDesc(user.getId()).stream()
                .filter(s -> statuses.contains(s.getStatus()))
                .map(this::currentResponse)
                .toList();
    }
    @Transactional(readOnly=true) public List<ParkingSessionResponse> history(UserAccount user){return sessions.findByVehicleUserIdOrderByEntryTimeDesc(user.getId()).stream().map(ParkingSessionResponse::from).toList();}
    @Transactional public CurrentParkingSessionResponse addService(UserAccount user,Long sessionId,ServiceUsageRequest r){PaymentModuleParkingSession s=ownedSession(user,sessionId);if(!"ACTIVE".equalsIgnoreCase(s.getStatus()))throw new BadRequestException("Additional services can only be added to ACTIVE sessions");if(r==null||r.serviceId()==null)throw new BadRequestException("serviceId is required");AdditionalService service=services.findById(r.serviceId()).orElseThrow(()->new ResourceNotFoundException("Additional service not found: "+r.serviceId()));if(!"ACTIVE".equalsIgnoreCase(service.getStatus()))throw new BadRequestException("Additional service is not active");SessionServiceUsage u=new SessionServiceUsage();u.setSession(s);u.setService(service);u.setQuantity(r.quantity()==null?1:Math.max(1,r.quantity()));u.setUnitPrice(service.getPrice());usages.save(u);return currentResponse(s);}

    private CurrentParkingSessionResponse currentResponse(PaymentModuleParkingSession s){PricingQuoteResponse quote=null;if("ACTIVE".equalsIgnoreCase(s.getStatus())){LocalDateTime now=LocalDateTime.now();if(!now.isAfter(s.getEntryTime()))now=s.getEntryTime().plusSeconds(1);quote=pricing.estimateForVehicle(s.getVehicle().getId(),s.getEntryTime(),now,false,0);}List<ServiceUsageView> list=usages.findBySessionId(s.getId()).stream().map(u->new ServiceUsageView(u.getId(),u.getService().getId(),u.getService().getName(),u.getQuantity(),u.getUnitPrice(),u.getUnitPrice().multiply(BigDecimal.valueOf(u.getQuantity())))).toList();BigDecimal extra=list.stream().map(ServiceUsageView::lineTotal).reduce(BigDecimal.ZERO,BigDecimal::add);BigDecimal base=quote==null?(s.getTotalFee()==null?BigDecimal.ZERO:s.getTotalFee()):quote.totalFee();return new CurrentParkingSessionResponse(ParkingSessionResponse.from(s),quote,list,extra,base.add(extra));}
    private Vehicle ownedVehicle(UserAccount u, Long id) {
        Vehicle v = vehicles.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + id));
        if ("DELETED".equalsIgnoreCase(v.getStatus())) {
            throw new ResourceNotFoundException("Vehicle not found: " + id);
        }
        if (!v.getUser().getId().equals(u.getId())) {
            throw new ForbiddenException("Vehicle does not belong to current user");
        }
        return v;
    }
    private PaymentModuleParkingSession ownedSession(UserAccount u,Long id){PaymentModuleParkingSession s=sessions.findById(id).orElseThrow(()->new ResourceNotFoundException("Session not found: "+id));if(!s.getVehicle().getUser().getId().equals(u.getId()))throw new ForbiddenException("Session does not belong to current user");return s;}
}
