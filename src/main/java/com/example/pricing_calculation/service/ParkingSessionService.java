package com.example.pricing_calculation.service;

import com.example.pricing_calculation.domain.ParkingSession;
import com.example.pricing_calculation.domain.ParkingSlot;
import com.example.pricing_calculation.domain.Reservation;
import com.example.pricing_calculation.domain.Vehicle;
import com.example.pricing_calculation.dto.ParkingSessionResponse;
import com.example.pricing_calculation.dto.PricingQuoteResponse;
import com.example.pricing_calculation.dto.SessionCheckInRequest;
import com.example.pricing_calculation.dto.SessionCheckoutRequest;
import com.example.pricing_calculation.repository.ParkingSessionRepository;
import com.example.pricing_calculation.repository.ParkingSlotRepository;
import com.example.pricing_calculation.repository.ReservationRepository;
import com.example.pricing_calculation.repository.VehicleRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ParkingSessionService {

    private final ParkingSessionRepository parkingSessionRepository;
    private final ReservationRepository reservationRepository;
    private final VehicleRepository vehicleRepository;
    private final ParkingSlotRepository parkingSlotRepository;
    private final PricingService pricingService;
    private final RealtimeEventService realtimeEventService;

    public ParkingSessionService(
            ParkingSessionRepository parkingSessionRepository,
            ReservationRepository reservationRepository,
            VehicleRepository vehicleRepository,
            ParkingSlotRepository parkingSlotRepository,
            PricingService pricingService,
            RealtimeEventService realtimeEventService) {
        this.parkingSessionRepository = parkingSessionRepository;
        this.reservationRepository = reservationRepository;
        this.vehicleRepository = vehicleRepository;
        this.parkingSlotRepository = parkingSlotRepository;
        this.pricingService = pricingService;
        this.realtimeEventService = realtimeEventService;
    }

    @Transactional
    public ParkingSessionResponse checkIn(SessionCheckInRequest request) {
        if (request == null || request.vehicleId() == null || request.slotId() == null) {
            throw new BadRequestException("vehicleId and slotId are required");
        }
        Vehicle vehicle = vehicleRepository.findById(request.vehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + request.vehicleId()));
        ParkingSlot slot = parkingSlotRepository.findById(request.slotId())
                .orElseThrow(() -> new ResourceNotFoundException("Parking slot not found: " + request.slotId()));
        if (slot.getStatus() != null
                && !slot.getStatus().equalsIgnoreCase("AVAILABLE")
                && !slot.getStatus().equalsIgnoreCase("RESERVED")) {
            throw new BadRequestException("Parking slot is not available");
        }
        Reservation reservation = null;
        if (request.reservationId() != null) {
            reservation = reservationRepository.findById(request.reservationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + request.reservationId()));
            if (!reservation.getVehicle().getId().equals(vehicle.getId())) {
                throw new BadRequestException("Reservation vehicle does not match check-in vehicle");
            }
            reservation.setStatus("CONFIRMED");
        }

        ParkingSession session = new ParkingSession();
        session.setReservation(reservation);
        session.setVehicle(vehicle);
        session.setSlot(slot);
        session.setTicketCode(request.ticketCode() == null || request.ticketCode().isBlank()
                ? generateTicketCode()
                : request.ticketCode());
        session.setEntryTime(request.entryTime() == null ? LocalDateTime.now() : request.entryTime());
        session.setStatus("ACTIVE");
        slot.setStatus("OCCUPIED");

        ParkingSession saved = parkingSessionRepository.save(session);
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

    @Transactional
    public ParkingSessionResponse checkout(Long id, SessionCheckoutRequest request) {
        ParkingSession session = findSession(id);
        if (!"ACTIVE".equalsIgnoreCase(session.getStatus())) {
            throw new BadRequestException("Only ACTIVE sessions can be checked out");
        }
        LocalDateTime exitTime = request == null || request.exitTime() == null ? LocalDateTime.now() : request.exitTime();
        PricingQuoteResponse quote = pricingService.estimate(
                session.getVehicle().getVehicleType().getId(),
                session.getEntryTime(),
                exitTime,
                request != null && request.lostTicket(),
                request == null ? 0 : request.overtimeMinutes()
        );
        session.setExitTime(exitTime);
        session.setParkingFee(quote.parkingFee());
        session.setPenaltyFee(quote.penaltyFee());
        session.setTotalFee(quote.totalFee());
        session.setStatus("CHECKED_OUT");
        session.getSlot().setStatus("AVAILABLE");
        ParkingSession saved = parkingSessionRepository.save(session);
        parkingSlotRepository.save(session.getSlot());
        ParkingSessionResponse response = ParkingSessionResponse.from(saved);
        realtimeEventService.publish("/topic/parking-sessions", "CHECKOUT_COMPLETED", "Parking session checked out", response);
        realtimeEventService.publish("/topic/parking-slots", "SLOT_AVAILABLE", "Parking slot available", response);
        return response;
    }

    private ParkingSession findSession(Long id) {
        return parkingSessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Parking session not found: " + id));
    }

    private String generateTicketCode() {
        return "TICKET-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
