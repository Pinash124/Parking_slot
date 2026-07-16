package com.example.pricing_calculation.service;

import com.example.pricing_calculation.domain.PaymentModuleParkingSlot;
import com.example.pricing_calculation.domain.Reservation;
import com.example.pricing_calculation.domain.UserAccount;
import com.example.pricing_calculation.domain.Vehicle;
import com.example.pricing_calculation.domain.Zone;
import com.example.pricing_calculation.dto.PageResponse;
import com.example.pricing_calculation.dto.ReservationCreateRequest;
import com.example.pricing_calculation.dto.ReservationResponse;
import com.example.pricing_calculation.repository.MonthlyParkingPassRepository;
import com.example.pricing_calculation.repository.PaymentModuleParkingSlotRepository;
import com.example.pricing_calculation.repository.ReservationRepository;
import com.example.pricing_calculation.repository.UserAccountRepository;
import com.example.pricing_calculation.repository.VehicleRepository;
import com.example.pricing_calculation.repository.ZoneRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final UserAccountRepository userAccountRepository;
    private final VehicleRepository vehicleRepository;
    private final ZoneRepository zoneRepository;
    private final PaymentModuleParkingSlotRepository parkingSlotRepository;
    private final MonthlyParkingPassRepository monthlyParkingPassRepository;
    private final RealtimeEventService realtimeEventService;

    public ReservationService(
            ReservationRepository reservationRepository,
            UserAccountRepository userAccountRepository,
            VehicleRepository vehicleRepository,
            ZoneRepository zoneRepository,
            PaymentModuleParkingSlotRepository parkingSlotRepository,
            MonthlyParkingPassRepository monthlyParkingPassRepository,
            RealtimeEventService realtimeEventService) {
        this.reservationRepository = reservationRepository;
        this.userAccountRepository = userAccountRepository;
        this.vehicleRepository = vehicleRepository;
        this.zoneRepository = zoneRepository;
        this.parkingSlotRepository = parkingSlotRepository;
        this.monthlyParkingPassRepository = monthlyParkingPassRepository;
        this.realtimeEventService = realtimeEventService;
    }

    @Transactional
    public ReservationResponse create(ReservationCreateRequest request) {
        validateCreateRequest(request);
        UserAccount user = userAccountRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.userId()));
        Vehicle vehicle = vehicleRepository.findById(request.vehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + request.vehicleId()));
        Zone zone = zoneRepository.findById(request.zoneId())
                .orElseThrow(() -> new ResourceNotFoundException("Zone not found: " + request.zoneId()));
        PaymentModuleParkingSlot slot = parkingSlotRepository.findByIdForUpdate(request.slotId())
                .orElseThrow(() -> new ResourceNotFoundException("Parking slot not found: " + request.slotId()));
        if (!vehicle.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("Vehicle does not belong to selected user");
        }
        if (monthlyParkingPassRepository.existsByVehicleIdAndStatusIn(vehicle.getId(),
                List.of("ACTIVE", "SCHEDULED", "PENDING_PAYMENT"))) {
            throw new BadRequestException("Vehicle already has a monthly pass and cannot make reservations");
        }
        if (!VehicleTypeClassifier.isCar(vehicle.getVehicleType())) {
            throw new BadRequestException("Only cars can reserve a parking slot");
        }
        if (!"CAR_NORMAL".equalsIgnoreCase(zone.getZoneType())) {
            throw new BadRequestException("Reservations must use a CAR_NORMAL zone");
        }
        if (!zone.getVehicleType().getId().equals(vehicle.getVehicleType().getId())) {
            throw new BadRequestException("Selected zone does not support this vehicle type");
        }
        if (slot.getZone() == null || !slot.getZone().getId().equals(zone.getId())) {
            throw new BadRequestException("Selected slot is outside the selected reservation zone");
        }
        if (!"AVAILABLE".equalsIgnoreCase(slot.getStatus())) {
            throw new BadRequestException("Selected parking slot is not available for reservation");
        }
        ensureZoneCapacity(zone.getId(), request.startTime(), request.endTime());

        Reservation reservation = new Reservation();
        reservation.setUser(user);
        reservation.setVehicle(vehicle);
        reservation.setZone(zone);
        reservation.setReservedSlot(slot);
        reservation.setStartTime(request.startTime());
        reservation.setEndTime(request.endTime());
        reservation.setStatus("APPROVED");
        slot.setStatus("RESERVED");
        parkingSlotRepository.save(slot);
        Reservation saved = reservationRepository.save(reservation);
        ReservationResponse response = ReservationResponse.from(saved);
        realtimeEventService.publish(
                "/topic/reservations",
                "RESERVATION_CREATED",
                "Reservation created",
                response);
        return response;
    }

    @Transactional(readOnly = true)
    public ReservationResponse getById(Long id) {
        return ReservationResponse.from(findReservation(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<ReservationResponse> search(
            Long userId,
            Long vehicleId,
            Long zoneId,
            String status,
            LocalDateTime from,
            LocalDateTime to,
            int page,
            int size) {
        PageRequest pageRequest = PageRequest.of(
                Math.max(0, page),
                Math.max(1, Math.min(size, 100)),
                Sort.by(Sort.Direction.DESC, "startTime"));
        Specification<Reservation> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (userId != null) {
                predicates.add(criteriaBuilder.equal(root.get("user").get("id"), userId));
            }
            if (vehicleId != null) {
                predicates.add(criteriaBuilder.equal(root.get("vehicle").get("id"), vehicleId));
            }
            if (zoneId != null) {
                predicates.add(criteriaBuilder.equal(root.get("zone").get("id"), zoneId));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(
                        criteriaBuilder.equal(criteriaBuilder.upper(root.get("status")), status.trim().toUpperCase()));
            }
            if (from != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("startTime"), from));
            }
            if (to != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("endTime"), to));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        return PageResponse
                .from(reservationRepository.findAll(specification, pageRequest).map(ReservationResponse::from));
    }

    @Transactional
    public ReservationResponse approve(Long id) {
        Reservation reservation = findReservation(id);
        ensureZoneCapacity(reservation.getZone().getId(), reservation.getStartTime(), reservation.getEndTime());
        reservation.setStatus("APPROVED");
        ReservationResponse response = ReservationResponse.from(reservationRepository.save(reservation));
        realtimeEventService.publish("/topic/reservations", "RESERVATION_APPROVED", "Reservation approved", response);
        return response;
    }

    @Transactional
    public ReservationResponse cancel(Long id) {
        return cancelReservation(findReservation(id));
    }

    @Transactional
    public ReservationResponse cancelForUser(Long id, Long userId) {
        Reservation reservation = findReservation(id);
        if (reservation.getUser() == null || !reservation.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Reservation does not belong to current user");
        }
        return cancelReservation(reservation);
    }

    private ReservationResponse cancelReservation(Reservation reservation) {
        reservation.setStatus("CANCELLED");
        if (reservation.getReservedSlot() != null
                && "RESERVED".equalsIgnoreCase(reservation.getReservedSlot().getStatus())) {
            reservation.getReservedSlot().setStatus("AVAILABLE");
            parkingSlotRepository.save(reservation.getReservedSlot());
        }
        ReservationResponse response = ReservationResponse.from(reservationRepository.save(reservation));
        realtimeEventService.publish("/topic/reservations", "RESERVATION_CANCELLED", "Reservation cancelled", response);
        return response;
    }

    private Reservation findReservation(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + id));
    }

    private void validateCreateRequest(ReservationCreateRequest request) {
        if (request == null) {
            throw new BadRequestException("Reservation request is required");
        }
        if (request.userId() == null || request.vehicleId() == null || request.zoneId() == null || request.slotId() == null) {
            throw new BadRequestException("userId, vehicleId, zoneId and slotId are required");
        }
        if (request.startTime() == null || request.endTime() == null) {
            throw new BadRequestException("startTime and endTime are required");
        }
        if (!request.endTime().isAfter(request.startTime())) {
            throw new BadRequestException("endTime must be after startTime");
        }
    }

    private void ensureZoneCapacity(Long zoneId, LocalDateTime startTime, LocalDateTime endTime) {
        long totalSlots = parkingSlotRepository.countByZoneId(zoneId);
        if (totalSlots <= 0) {
            throw new BadRequestException("Zone does not have parking slots");
        }
        long overlappingReservations = reservationRepository.countActiveOverlaps(zoneId, startTime, endTime);
        if (overlappingReservations >= totalSlots) {
            throw new BadRequestException("No reservation capacity available for the selected zone and time");
        }
    }
}
