package com.example.pricing_calculation.service;

import com.example.pricing_calculation.domain.PaymentModuleParkingSession;
import com.example.pricing_calculation.domain.PaymentModuleParkingSlot;
import com.example.pricing_calculation.domain.Reservation;
import com.example.pricing_calculation.domain.Vehicle;
import com.example.pricing_calculation.domain.UserAccount;
import com.example.pricing_calculation.domain.VehicleTypeEntity;
import com.example.pricing_calculation.domain.MonthlyParkingPass;
import com.example.pricing_calculation.config.ParkingRuleProperties;
import com.example.pricing_calculation.dto.FloorOccupancyResponse;
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
import com.example.pricing_calculation.repository.MonthlyParkingPassRepository;
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
    private final MonthlyParkingPassRepository monthlyPassRepository;
    private final ParkingRuleProperties rules;
    private final QrCodeService qrCodeService;

    public PaymentModuleParkingSessionService(
            PaymentModuleParkingSessionRepository parkingSessionRepository,
            ReservationRepository reservationRepository,
            VehicleRepository vehicleRepository,
            PaymentModuleParkingSlotRepository parkingSlotRepository,
            PricingService pricingService,
            RealtimeEventService realtimeEventService,
            SessionServiceUsageRepository serviceUsageRepository,
            UserAccountRepository userAccountRepository,
            PaymentModuleVehicleTypeRepository vehicleTypeRepository,
            MonthlyParkingPassRepository monthlyPassRepository,
            ParkingRuleProperties rules,
            QrCodeService qrCodeService) {
        this.parkingSessionRepository = parkingSessionRepository;
        this.reservationRepository = reservationRepository;
        this.vehicleRepository = vehicleRepository;
        this.parkingSlotRepository = parkingSlotRepository;
        this.pricingService = pricingService;
        this.realtimeEventService = realtimeEventService;
        this.serviceUsageRepository = serviceUsageRepository;
        this.userAccountRepository = userAccountRepository;
        this.vehicleTypeRepository = vehicleTypeRepository;
        this.monthlyPassRepository = monthlyPassRepository;
        this.rules = rules;
        this.qrCodeService = qrCodeService;
    }

    @Transactional
    public ParkingSessionResponse checkIn(SessionCheckInRequest request) {
        return checkIn(request, null, null);
    }

    @Transactional
    public ParkingSessionResponse checkIn(SessionCheckInRequest request, Long staffId, String entryGateCode) {
        if (request == null) {
            throw new BadRequestException("Check-in request is required");
        }
        LocalDateTime entryTime = request.entryTime() == null
                ? LocalDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"))
                : request.entryTime();

        Vehicle vehicle = null;
        if (request.reservationId() != null && request.vehicleId() == null) {
            Reservation reservation = reservationRepository.findById(request.reservationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + request.reservationId()));
            if (reservation.getVehicle() == null) {
                throw new BadRequestException("Reservation vehicle is required for check-in");
            }
            vehicle = reservation.getVehicle();
        } else if (request.vehicleId() != null) {
            vehicle = vehicleRepository.findById(request.vehicleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + request.vehicleId()));
            if (request.vehicleTypeId() != null
                    && vehicle.getVehicleType() != null
                    && !request.vehicleTypeId().equals(vehicle.getVehicleType().getId())) {
                throw new BadRequestException("Selected vehicle type does not match registered vehicle");
            }
        } else if (request.licensePlate() != null && !request.licensePlate().isBlank()) {
            String plate = request.licensePlate().trim().toUpperCase();
            vehicle = findVehicleByNormalizedPlate(plate);
            if (vehicle == null) {
                Vehicle guestVehicle = new Vehicle();
                guestVehicle.setPlateNumber(plate);
                
                UserAccount owner = userAccountRepository.findByEmailIgnoreCase("customer@example.com").orElse(null);
                if (owner == null && staffId != null) {
                    owner = userAccountRepository.findById(staffId).orElse(null);
                }
                guestVehicle.setUser(owner);
                
                Long requestedVehicleTypeId = request.vehicleTypeId() == null ? 1L : request.vehicleTypeId();
                VehicleTypeEntity requestedType = vehicleTypeRepository.findById(requestedVehicleTypeId)
                        .orElseThrow(() -> new ResourceNotFoundException("Vehicle type not found: " + requestedVehicleTypeId));
                guestVehicle.setVehicleType(requestedType);
                guestVehicle.setBrand("Guest");
                guestVehicle.setColor("Unknown");
                guestVehicle.setStatus("ACTIVE");
                
                vehicle = vehicleRepository.save(guestVehicle);
                vehicle.setQrCode(qrCodeService.buildVehicleQrContent(vehicle));
                vehicle = vehicleRepository.save(vehicle);
            } else if (request.vehicleTypeId() != null
                    && vehicle.getVehicleType() != null
                    && !request.vehicleTypeId().equals(vehicle.getVehicleType().getId())) {
                throw new BadRequestException("Selected vehicle type does not match registered vehicle");
            }
        } else {
            throw new BadRequestException("vehicleId or licensePlate is required");
        }

        ReservationResolution reservationResolution = resolveReservation(request, vehicle, entryTime);
        Reservation reservation = reservationResolution.reservation();
        MonthlyParkingPass monthlyPass = reservation == null && !reservationResolution.invalidated()
                ? findActiveMonthlyPass(vehicle.getId(), entryTime)
                : null;
        boolean flexibleTwoWheelWalkIn = reservation == null
                && monthlyPass == null
                && VehicleTypeClassifier.isTwoWheel(vehicle.getVehicleType());
        Long slotId;
        if (monthlyPass != null && monthlyPass.getReservedSlot() != null) {
            slotId = monthlyPass.getReservedSlot().getId();
        } else if (reservation != null) {
            if (reservation.getReservedSlot() != null) {
                slotId = reservation.getReservedSlot().getId();
            } else if (request.slotId() != null) {
                slotId = request.slotId();
            } else {
                throw new BadRequestException("Reservation does not have a selected parking slot");
            }
        } else if (flexibleTwoWheelWalkIn) {
            slotId = firstAvailableSlotIdForWalkIn(vehicle.getVehicleType().getId(), entryTime);
        } else {
            slotId = reservationResolution.invalidated() ? null : request.slotId();
        }
        if (slotId == null && !flexibleTwoWheelWalkIn) {
            slotId = firstAvailableSlotIdForWalkIn(vehicle.getVehicleType().getId(), entryTime);
        }
        PaymentModuleParkingSlot slot = null;
        if (slotId != null) {
            Long selectedSlotId = slotId;
            slot = parkingSlotRepository.findByIdForUpdate(selectedSlotId)
                    .orElseThrow(() -> new ResourceNotFoundException("Parking slot not found: " + selectedSlotId));
            boolean assignedReservationSlot = reservation != null
                    && ((reservation.getReservedSlot() != null && reservation.getReservedSlot().getId().equals(slot.getId()))
                    || (reservation.getReservedSlot() == null && request.slotId() != null && request.slotId().equals(slot.getId())));
            boolean assignedMonthlySlot = monthlyPass != null
                    && monthlyPass.getReservedSlot() != null
                    && monthlyPass.getReservedSlot().getId().equals(slot.getId());
            if ("RESERVED".equalsIgnoreCase(slot.getStatus()) && !assignedReservationSlot) {
                throw new BadRequestException("Parking slot is reserved for another reservation");
            }
            if (("MONTHLY_HELD".equalsIgnoreCase(slot.getStatus()) || "MONTHLY_RESERVED".equalsIgnoreCase(slot.getStatus()))
                    && !assignedMonthlySlot) {
                throw new BadRequestException("Parking slot is assigned to another monthly pass");
            }
            if (monthlyPass == null && "CAR_MONTHLY".equalsIgnoreCase(slot.getZone().getZoneType())) {
                throw new BadRequestException("CAR_MONTHLY slots are reserved for paid monthly passes");
            }
            if (slot.getStatus() != null
                    && !slot.getStatus().equalsIgnoreCase("AVAILABLE")
                    && !slot.getStatus().equalsIgnoreCase("RESERVED")
                    && !slot.getStatus().equalsIgnoreCase("MONTHLY_RESERVED")) {
                throw new BadRequestException("Parking slot is not available");
            }
            if (!slot.getZone().getVehicleType().getId().equals(vehicle.getVehicleType().getId())) {
                throw new BadRequestException("Vehicle type is not allowed in selected slot zone");
            }
            if (reservation == null && monthlyPass == null) {
                ensureWalkInCapacity(slot, entryTime);
            }
        }
        if (parkingSessionRepository.countByVehicleIdAndStatusIn(vehicle.getId(), java.util.List.of("ACTIVE", "PAYMENT_PENDING")) > 0) {
            throw new BadRequestException("Vehicle already has an active parking session");
        }
        if (reservation != null) {
            if (!reservation.getVehicle().getId().equals(vehicle.getId())) {
                throw new BadRequestException("Reservation vehicle does not match check-in vehicle");
            }
            if (slot == null) {
                throw new BadRequestException("Reservation check-in requires a selected parking slot");
            }
            if (!reservation.getZone().getId().equals(slot.getZone().getId())) {
                throw new BadRequestException("Selected slot is outside the reserved zone");
            }
            if (reservation.getReservedSlot() != null && !reservation.getReservedSlot().getId().equals(slot.getId())) {
                throw new BadRequestException("Check-in must use the slot selected in the reservation");
            }
            if (reservation.getReservedSlot() == null) {
                reservation.setReservedSlot(slot);
            }
            if (!"PENDING".equalsIgnoreCase(reservation.getStatus())
                    && !"APPROVED".equalsIgnoreCase(reservation.getStatus())) {
                throw new BadRequestException("Reservation cannot be checked in from status " + reservation.getStatus());
            }
            reservation.setStatus("CONFIRMED");
        }

        PaymentModuleParkingSession session = new PaymentModuleParkingSession();
        session.setReservation(reservation);
        session.setMonthlyPass(monthlyPass);
        session.setVehicle(vehicle);
        session.setSlot(slot);
        session.setTicketCode(request.ticketCode() == null || request.ticketCode().isBlank()
                ? generateTicketCode()
                : request.ticketCode());
        session.setEntryTime(entryTime);
        session.setStatus("ACTIVE");
        session.setEntryStaffId(staffId);
        session.setEntryGateCode(entryGateCode);
        if (slot != null) {
            slot.setStatus(monthlyPass == null ? "OCCUPIED" : "MONTHLY_OCCUPIED");
        }

        PaymentModuleParkingSession saved = parkingSessionRepository.save(session);
        if (slot != null) {
            parkingSlotRepository.save(slot);
        }
        ParkingSessionResponse response = ParkingSessionResponse.from(saved);
        realtimeEventService.publish("/topic/parking-sessions", "SESSION_STARTED", "Parking session started", response);
        if (slot != null) {
            realtimeEventService.publish("/topic/parking-slots", "SLOT_OCCUPIED", "Parking slot occupied", response);
        }
        return response;
    }

    private ReservationResolution resolveReservation(SessionCheckInRequest request, Vehicle vehicle, LocalDateTime entryTime) {
        if (request.reservationId() != null) {
            Reservation reservation = reservationRepository.findById(request.reservationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + request.reservationId()));
            if (reservation.getVehicle() == null || !vehicle.getId().equals(reservation.getVehicle().getId())) {
                throw new BadRequestException("Reservation vehicle does not match check-in vehicle");
            }
            if (!"PENDING".equalsIgnoreCase(reservation.getStatus())
                    && !"APPROVED".equalsIgnoreCase(reservation.getStatus())) {
                throw new BadRequestException("Reservation cannot be checked in from status " + reservation.getStatus());
            }
            return evaluateReservationWindow(reservation, entryTime);
        }
        for (Reservation reservation : reservationRepository.findByVehicleIdAndStatusInOrderByStartTimeAsc(
                vehicle.getId(), java.util.List.of("PENDING", "APPROVED"))) {
            ReservationResolution result = evaluateReservationWindow(reservation, entryTime);
            if (result.reservation() != null || result.invalidated()) {
                return result;
            }
        }
        return new ReservationResolution(null, false);
    }

    private ReservationResolution evaluateReservationWindow(Reservation reservation, LocalDateTime entryTime) {
        if (reservation.getStartTime() == null) {
            throw new BadRequestException("Reservation startTime is required");
        }
        LocalDateTime earliest = reservation.getStartTime().minusMinutes(rules.getReservationEarlyMinutes());
        LocalDateTime latest = reservation.getStartTime().plusMinutes(rules.getReservationLateMinutes());
        if (entryTime.isBefore(earliest)) {
            throw new BadRequestException("Reservation check-in is too early; selected slot remains reserved");
        }
        if (entryTime.isAfter(latest)) {
            cancelReservationForArrival(reservation);
            throw new BadRequestException("Reservation check-in window has expired");
        }
        return new ReservationResolution(reservation, false);
    }

    private void cancelReservationForArrival(Reservation reservation) {
        reservation.setStatus("CANCELLED");
        if (reservation.getReservedSlot() != null
                && "RESERVED".equalsIgnoreCase(reservation.getReservedSlot().getStatus())) {
            reservation.getReservedSlot().setStatus("AVAILABLE");
            parkingSlotRepository.save(reservation.getReservedSlot());
        }
        reservationRepository.save(reservation);
    }

    private Long firstAvailableSlotIdForReservation(Long vehicleTypeId, Long zoneId) {
        return firstAvailableSlotId(vehicleTypeId, zoneId, null, false);
    }

    private Long firstAvailableSlotIdForWalkIn(Long vehicleTypeId, LocalDateTime entryTime) {
        return firstAvailableSlotId(vehicleTypeId, null, entryTime, true);
    }

    private Long firstAvailableSlotId(Long vehicleTypeId, Long zoneId, LocalDateTime entryTime, boolean protectReservations) {
        java.util.List<PaymentModuleParkingSlot> available = parkingSlotRepository.searchAvailableSlots(
                zoneId, vehicleTypeId, "AVAILABLE").stream()
                .filter(slot -> VehicleTypeClassifier.isCar(slot.getZone().getVehicleType())
                        ? "CAR_NORMAL".equalsIgnoreCase(slot.getZone().getZoneType())
                        : "MOTORBIKE".equalsIgnoreCase(slot.getZone().getZoneType()))
                .filter(slot -> !protectReservations || hasWalkInCapacity(slot, entryTime))
                .sorted(java.util.Comparator
                        .comparing((PaymentModuleParkingSlot slot) ->
                                slot.getZone() == null
                                        || slot.getZone().getFloor() == null
                                        || slot.getZone().getFloor().getFloorNumber() == null
                                        ? Integer.MAX_VALUE
                                        : slot.getZone().getFloor().getFloorNumber())
                        .thenComparing(slot -> slot.getZone() == null || slot.getZone().getZoneName() == null
                                ? ""
                                : slot.getZone().getZoneName())
                        .thenComparing(slot -> slot.getSlotCode() == null ? "" : slot.getSlotCode()))
                .toList();
        if (available.isEmpty()) {
            throw new BadRequestException("Parking area is full for this vehicle type");
        }
        return available.get(0).getId();
    }

    private void ensureWalkInCapacity(PaymentModuleParkingSlot slot, LocalDateTime entryTime) {
        if (!hasWalkInCapacity(slot, entryTime)) {
            throw new BadRequestException("No walk-in capacity available because upcoming reservations are holding the remaining normal car slots");
        }
    }

    private boolean hasWalkInCapacity(PaymentModuleParkingSlot slot, LocalDateTime entryTime) {
        if (slot == null || slot.getZone() == null || slot.getZone().getVehicleType() == null) {
            return true;
        }
        if (!VehicleTypeClassifier.isCar(slot.getZone().getVehicleType())
                || !"CAR_NORMAL".equalsIgnoreCase(slot.getZone().getZoneType())) {
            return true;
        }
        long availableSlots = parkingSlotRepository.countByZoneIdAndStatusIgnoreCase(slot.getZone().getId(), "AVAILABLE");
        long protectedReservations = reservationRepository.countPendingArrivalsNeedingCapacity(
                slot.getZone().getId(),
                entryTime.minusMinutes(rules.getReservationLateMinutes()),
                entryTime.plusMinutes(rules.getReservationEarlyMinutes()),
                entryTime);
        return availableSlots > protectedReservations;
    }

    private MonthlyParkingPass findActiveMonthlyPass(Long vehicleId, LocalDateTime entryTime) {
        return monthlyPassRepository.findByVehicleIdOrderByCreatedAtDesc(vehicleId).stream()
                .filter(pass -> pass.isActiveAt(entryTime.toLocalDate()))
                .findFirst()
                .orElse(null);
    }

    private record ReservationResolution(Reservation reservation, boolean invalidated) { }

    @Transactional(readOnly = true)
    public java.util.List<FloorOccupancyResponse> floorOccupancy() {
        java.util.Map<Long, FloorOccupancyAccumulator> floors = new java.util.LinkedHashMap<>();

        for (PaymentModuleParkingSlot slot : parkingSlotRepository.findAll()) {
            if (slot == null || slot.getZone() == null || slot.getZone().getFloor() == null) {
                continue;
            }
            if ("MAINTENANCE".equalsIgnoreCase(slot.getStatus()) || "LOCKED".equalsIgnoreCase(slot.getStatus())) {
                continue;
            }
            var floor = slot.getZone().getFloor();
            FloorOccupancyAccumulator acc = floors.computeIfAbsent(
                    floor.getId(),
                    id -> new FloorOccupancyAccumulator(floor.getId(), floor.getFloorName(), floor.getFloorNumber())
            );
            if (VehicleTypeClassifier.isCar(slot.getZone().getVehicleType())
                    && "CAR_NORMAL".equalsIgnoreCase(slot.getZone().getZoneType())) {
                acc.carTotal++;
            } else if (VehicleTypeClassifier.isTwoWheel(slot.getZone().getVehicleType())
                    || "MOTORBIKE".equalsIgnoreCase(slot.getZone().getZoneType())) {
                acc.twoWheelTotal++;
            }
        }

        java.util.List<PaymentModuleParkingSession> active = new java.util.ArrayList<>();
        active.addAll(parkingSessionRepository.findByStatusIgnoreCaseOrderByEntryTimeDesc("ACTIVE"));
        active.addAll(parkingSessionRepository.findByStatusIgnoreCaseOrderByEntryTimeDesc("PAYMENT_PENDING"));
        for (PaymentModuleParkingSession session : active) {
            if (session == null || session.getSlot() == null
                    || session.getSlot().getZone() == null
                    || session.getSlot().getZone().getFloor() == null
                    || session.getVehicle() == null) {
                continue;
            }
            var floor = session.getSlot().getZone().getFloor();
            FloorOccupancyAccumulator acc = floors.computeIfAbsent(
                    floor.getId(),
                    id -> new FloorOccupancyAccumulator(floor.getId(), floor.getFloorName(), floor.getFloorNumber())
            );
            if (VehicleTypeClassifier.isTwoWheel(session.getVehicle().getVehicleType())) {
                acc.twoWheelUsed++;
            } else if (VehicleTypeClassifier.isCar(session.getVehicle().getVehicleType())
                    && "CAR_NORMAL".equalsIgnoreCase(session.getSlot().getZone().getZoneType())) {
                acc.carUsed++;
            }
        }

        return floors.values().stream()
                .sorted(java.util.Comparator
                        .comparing((FloorOccupancyAccumulator acc) -> acc.floorNumber == null ? Integer.MAX_VALUE : acc.floorNumber)
                        .thenComparing(acc -> acc.floorName == null ? "" : acc.floorName))
                .map(FloorOccupancyAccumulator::toResponse)
                .toList();
    }

    private static class FloorOccupancyAccumulator {
        private final Long floorId;
        private final String floorName;
        private final Integer floorNumber;
        private long carUsed;
        private long carTotal;
        private long twoWheelUsed;
        private long twoWheelTotal;

        private FloorOccupancyAccumulator(Long floorId, String floorName, Integer floorNumber) {
            this.floorId = floorId;
            this.floorName = floorName;
            this.floorNumber = floorNumber;
        }

        private FloorOccupancyResponse toResponse() {
            return new FloorOccupancyResponse(floorId, floorName, floorNumber, carUsed, carTotal, twoWheelUsed, twoWheelTotal);
        }
    }

    @Transactional(readOnly = true)
    public ParkingSessionResponse lookupForGate(String query) {
        if (query == null || query.isBlank()) {
            throw new BadRequestException("Lookup query is required");
        }
        String term = query.trim().toUpperCase();
        java.util.Optional<PaymentModuleParkingSession> matched = java.util.Optional.empty();
        if (term.matches("^\\d+$")) {
            matched = parkingSessionRepository.findById(Long.parseLong(term));
        }
        if (matched.isEmpty()) {
            matched = parkingSessionRepository.findFirstByTicketCodeIgnoreCaseOrderByEntryTimeDesc(term);
        }
        if (matched.isEmpty()) {
            Vehicle vehicle = findVehicleByNormalizedPlate(term);
            if (vehicle != null) {
                matched = parkingSessionRepository.findFirstByVehicleIdOrderByEntryTimeDesc(vehicle.getId());
            }
        }
        return matched.map(ParkingSessionResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Parking session not found for query: " + term));
    }
    @Transactional(readOnly = true)
    public ParkingSessionResponse getById(Long id) {
        return ParkingSessionResponse.from(findSession(id));
    }

    @Transactional(readOnly = true)
    public java.util.List<ParkingSessionResponse> list(String status) {
        java.util.List<PaymentModuleParkingSession> sessions = status == null || status.isBlank()
                ? parkingSessionRepository.findAllByOrderByEntryTimeDesc()
                : parkingSessionRepository.findByStatusIgnoreCaseOrderByEntryTimeDesc(status.trim());
        return sessions.stream().map(ParkingSessionResponse::from).toList();
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
        if (session.getSlot() != null) {
            session.getSlot().setStatus(session.getMonthlyPass() == null ? "AVAILABLE" : "MONTHLY_RESERVED");
            parkingSlotRepository.save(session.getSlot());
        }
        ParkingSessionResponse response = ParkingSessionResponse.from(parkingSessionRepository.save(session));
        realtimeEventService.publish("/topic/parking-sessions", "EXIT_COMPLETED", "Paid vehicle exited", response);
        if (session.getSlot() != null) {
            realtimeEventService.publish("/topic/parking-slots", "SLOT_AVAILABLE", "Parking slot available", response);
        }
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

    private Vehicle findVehicleByNormalizedPlate(String plate) {
        if (plate == null || plate.isBlank()) {
            return null;
        }
        String target = plate.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        java.util.Optional<Vehicle> opt = vehicleRepository.findByPlateNumberIgnoreCase(plate);
        if (opt.isPresent()) {
            return opt.get();
        }
        return vehicleRepository.findByNormalizedPlate(target).orElse(null);
    }

    private String generateTicketCode() {
        return "T-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
