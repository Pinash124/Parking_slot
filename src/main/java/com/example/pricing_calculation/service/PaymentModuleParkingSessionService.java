package com.example.pricing_calculation.service;

import com.example.pricing_calculation.domain.PaymentModuleParkingSession;
import com.example.pricing_calculation.domain.PaymentModuleParkingSlot;
import com.example.pricing_calculation.domain.Reservation;
import com.example.pricing_calculation.domain.Vehicle;
import com.example.pricing_calculation.domain.UserAccount;
import com.example.pricing_calculation.domain.VehicleTypeEntity;
import com.example.pricing_calculation.dto.ParkingSessionResponse;
import com.example.pricing_calculation.dto.PricingQuoteResponse;
import com.example.pricing_calculation.dto.SessionCheckInRequest;
import com.example.pricing_calculation.dto.SessionCheckoutRequest;
import com.example.pricing_calculation.repository.PaymentModuleParkingSessionRepository;
import com.example.pricing_calculation.repository.PaymentModuleParkingSlotRepository;
import com.example.pricing_calculation.repository.ReservationRepository;
import com.example.pricing_calculation.repository.VehicleRepository;
import com.example.pricing_calculation.repository.SessionServiceUsageRepository;
import com.example.pricing_calculation.repository.UserAccountRepository;
import com.example.pricing_calculation.repository.PaymentModuleVehicleTypeRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentModuleParkingSessionService {

    private final PaymentModuleParkingSessionRepository parkingSessionRepository;
    private final ReservationRepository reservationRepository;
    private final VehicleRepository vehicleRepository;
    private final PaymentModuleParkingSlotRepository parkingSlotRepository;
    private final PricingService pricingService;
    private final RealtimeEventService realtimeEventService;
    private final SessionServiceUsageRepository serviceUsageRepository;
    private final UserAccountRepository userAccountRepository;
    private final PaymentModuleVehicleTypeRepository vehicleTypeRepository;

    public PaymentModuleParkingSessionService(
            PaymentModuleParkingSessionRepository parkingSessionRepository,
            ReservationRepository reservationRepository,
            VehicleRepository vehicleRepository,
            PaymentModuleParkingSlotRepository parkingSlotRepository,
            PricingService pricingService,
            RealtimeEventService realtimeEventService,
            SessionServiceUsageRepository serviceUsageRepository,
            UserAccountRepository userAccountRepository,
            PaymentModuleVehicleTypeRepository vehicleTypeRepository) {
        this.parkingSessionRepository = parkingSessionRepository;
        this.reservationRepository = reservationRepository;
        this.vehicleRepository = vehicleRepository;
        this.parkingSlotRepository = parkingSlotRepository;
        this.pricingService = pricingService;
        this.realtimeEventService = realtimeEventService;
        this.serviceUsageRepository = serviceUsageRepository;
        this.userAccountRepository = userAccountRepository;
        this.vehicleTypeRepository = vehicleTypeRepository;
    }

    @Transactional
    public ParkingSessionResponse checkIn(SessionCheckInRequest request) {
        return checkIn(request, null, null);
    }

    @Transactional
    public ParkingSessionResponse checkIn(SessionCheckInRequest request, Long staffId, String entryGateCode) {
        if (request == null || request.slotId() == null) {
            throw new BadRequestException("slotId is required");
        }
        Vehicle vehicle = null;
        if (request.vehicleId() != null) {
            vehicle = vehicleRepository.findById(request.vehicleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + request.vehicleId()));
        } else if (request.licensePlate() != null && !request.licensePlate().isBlank()) {
            String plate = request.licensePlate().trim().toUpperCase();
            vehicle = vehicleRepository.findByPlateNumberIgnoreCase(plate).orElse(null);
            if (vehicle == null) {
                Vehicle guestVehicle = new Vehicle();
                guestVehicle.setPlateNumber(plate);
                
                UserAccount owner = userAccountRepository.findByEmailIgnoreCase("customer@example.com").orElse(null);
                if (owner == null && staffId != null) {
                    owner = userAccountRepository.findById(staffId).orElse(null);
                }
                guestVehicle.setUser(owner);
                
                VehicleTypeEntity defaultType = vehicleTypeRepository.findById(1L)
                        .orElseThrow(() -> new ResourceNotFoundException("Default vehicle type not found"));
                guestVehicle.setVehicleType(defaultType);
                guestVehicle.setBrand("Guest");
                guestVehicle.setColor("Unknown");
                guestVehicle.setStatus("ACTIVE");
                
                vehicle = vehicleRepository.save(guestVehicle);
            }
        } else {
            throw new BadRequestException("vehicleId or licensePlate is required");
        }

        PaymentModuleParkingSlot slot = parkingSlotRepository.findByIdForUpdate(request.slotId())
                .orElseThrow(() -> new ResourceNotFoundException("Parking slot not found: " + request.slotId()));
        if (slot.getStatus() != null
                && !slot.getStatus().equalsIgnoreCase("AVAILABLE")
                && !slot.getStatus().equalsIgnoreCase("RESERVED")) {
            throw new BadRequestException("Parking slot is not available");
        }
        if (!slot.getZone().getVehicleType().getId().equals(vehicle.getVehicleType().getId())) {
            throw new BadRequestException("Vehicle type is not allowed in selected slot zone");
        }
        if (parkingSessionRepository.countByVehicleIdAndStatusIn(vehicle.getId(), java.util.List.of("ACTIVE", "PAYMENT_PENDING")) > 0) {
            throw new BadRequestException("Vehicle already has an active parking session");
        }
        Reservation reservation = null;
        if (request.reservationId() != null) {
            reservation = reservationRepository.findById(request.reservationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + request.reservationId()));
            if (!reservation.getVehicle().getId().equals(vehicle.getId())) {
                throw new BadRequestException("Reservation vehicle does not match check-in vehicle");
            }
            if (!reservation.getZone().getId().equals(slot.getZone().getId())) {
                throw new BadRequestException("Selected slot is outside the reserved zone");
            }
            if (reservation.getReservedSlot() != null && !reservation.getReservedSlot().getId().equals(slot.getId())) {
                throw new BadRequestException("Check-in must use the slot assigned to the reservation");
            }
            if ("CANCELLED".equalsIgnoreCase(reservation.getStatus())) {
                throw new BadRequestException("Reservation is cancelled");
            }
            reservation.setStatus("CONFIRMED");
        }

        PaymentModuleParkingSession session = new PaymentModuleParkingSession();
        session.setReservation(reservation);
        session.setVehicle(vehicle);
        session.setSlot(slot);
        session.setTicketCode(request.ticketCode() == null || request.ticketCode().isBlank()
                ? generateTicketCode()
                : request.ticketCode());
        session.setEntryTime(request.entryTime() == null ? LocalDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")) : request.entryTime());
        session.setStatus("ACTIVE");
        session.setEntryStaffId(staffId);
        session.setEntryGateCode(entryGateCode);
        slot.setStatus("OCCUPIED");

        PaymentModuleParkingSession saved = parkingSessionRepository.save(session);
        parkingSlotRepository.save(slot);
        ParkingSessionResponse response = ParkingSessionResponse.from(saved);
        realtimeEventService.publish("/topic/parking-sessions", "SESSION_STARTED", "Parking session started", response);
        realtimeEventService.publish("/topic/parking-slots", "SLOT_OCCUPIED", "Parking slot occupied", response);
        return response;
    }

    @Transactional(readOnly = true)
    public ParkingSessionResponse getById(Long id) {
        return ParkingSessionResponse.from(findSession(id));
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.SERIALIZABLE)
    public ParkingSessionResponse checkout(Long id, SessionCheckoutRequest request) {
        PaymentModuleParkingSession session = findSessionForUpdate(id);
        if (!"ACTIVE".equalsIgnoreCase(session.getStatus())) {
            throw new BadRequestException("Only ACTIVE sessions can be checked out");
        }
        LocalDateTime exitTime = request == null || request.exitTime() == null ? LocalDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")) : request.exitTime();
        PricingQuoteResponse quote = pricingService.estimateForVehicle(
                session.getVehicle().getId(),
                session.getEntryTime(),
                exitTime,
                request != null && request.lostTicket(),
                request == null ? 0 : request.overtimeMinutes()
        );
        session.setExitTime(exitTime);
        session.setParkingFee(quote.parkingFee());
        session.setPenaltyFee(quote.penaltyFee());
        BigDecimal additionalTotal = serviceUsageRepository.findBySessionId(session.getId()).stream()
                .map(usage -> usage.getUnitPrice().multiply(BigDecimal.valueOf(usage.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        session.setTotalFee(quote.totalFee().add(additionalTotal));
        session.setStatus("PAYMENT_PENDING");
        PaymentModuleParkingSession saved = parkingSessionRepository.save(session);
        ParkingSessionResponse response = ParkingSessionResponse.from(saved);
        realtimeEventService.publish("/topic/parking-sessions", "PAYMENT_REQUIRED", "Parking fee calculated; payment required", response);
        return response;
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.SERIALIZABLE)
    public ParkingSessionResponse completePaidExit(Long id, Long staffId, String exitGateCode) {
        PaymentModuleParkingSession session = findSessionForUpdate(id);
        session.setExitStaffId(staffId);
        session.setExitGateCode(exitGateCode);
        session.setStatus("COMPLETED");
        session.getSlot().setStatus("AVAILABLE");
        parkingSlotRepository.save(session.getSlot());
        ParkingSessionResponse response = ParkingSessionResponse.from(parkingSessionRepository.save(session));
        realtimeEventService.publish("/topic/parking-sessions", "EXIT_COMPLETED", "Paid vehicle exited", response);
        realtimeEventService.publish("/topic/parking-slots", "SLOT_AVAILABLE", "Parking slot available", response);
        return response;
    }

    private PaymentModuleParkingSession findSession(Long id) {
        return parkingSessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Parking session not found: " + id));
    }

    private PaymentModuleParkingSession findSessionForUpdate(Long id) {
        return parkingSessionRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Parking session not found: " + id));
    }

    private String generateTicketCode() {
        return "TICKET-" + LocalDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
