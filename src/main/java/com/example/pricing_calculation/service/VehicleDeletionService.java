package com.example.pricing_calculation.service;

import com.example.pricing_calculation.domain.MonthlyParkingPass;
import com.example.pricing_calculation.domain.Reservation;
import com.example.pricing_calculation.domain.Vehicle;
import com.example.pricing_calculation.repository.MonthlyParkingPassRepository;
import com.example.pricing_calculation.repository.PaymentModuleParkingSessionRepository;
import com.example.pricing_calculation.repository.ReservationRepository;
import com.example.pricing_calculation.repository.VehicleRepository;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VehicleDeletionService {
    private static final List<String> OPEN_SESSION_STATUSES =
            List.of("ACTIVE", "PAYMENT_PENDING", "PENDING_PAYMENT");
    private static final Set<String> ACTIVE_RESERVATION_STATUSES = Set.of("PENDING", "APPROVED");
    private static final Set<String> ACTIVE_PASS_STATUSES =
            Set.of("PENDING_PAYMENT", "SCHEDULED", "ACTIVE");

    private final VehicleRepository vehicles;
    private final PaymentModuleParkingSessionRepository sessions;
    private final ReservationRepository reservations;
    private final MonthlyParkingPassRepository monthlyPasses;

    public VehicleDeletionService(
            VehicleRepository vehicles,
            PaymentModuleParkingSessionRepository sessions,
            ReservationRepository reservations,
            MonthlyParkingPassRepository monthlyPasses) {
        this.vehicles = vehicles;
        this.sessions = sessions;
        this.reservations = reservations;
        this.monthlyPasses = monthlyPasses;
    }

    @Transactional
    public void delete(Vehicle vehicle) {
        Long vehicleId = vehicle.getId();
        if (sessions.countByVehicleIdAndStatusIn(vehicleId, OPEN_SESSION_STATUSES) > 0) {
            throw new BadRequestException(
                    "Xe đang ở trong bãi hoặc đang chờ thanh toán nên chưa thể xóa.");
        }

        List<Reservation> vehicleReservations = reservations.findByVehicleIdOrderByStartTimeDesc(vehicleId);
        vehicleReservations.forEach(this::cancelReservation);
        reservations.saveAll(vehicleReservations);
        reservations.flush();

        List<MonthlyParkingPass> vehiclePasses =
                monthlyPasses.findByVehicleIdOrderByCreatedAtDesc(vehicleId);
        vehiclePasses.forEach(this::cancelMonthlyPass);
        monthlyPasses.saveAll(vehiclePasses);
        monthlyPasses.flush();

        // Keep the vehicle row as an internal tombstone so historical sessions,
        // payments and monthly-pass revenue retain their original relationships.
        vehicle.setStatus("DELETED");
        vehicles.saveAndFlush(vehicle);
    }

    private void cancelReservation(Reservation reservation) {
        String status = normalized(reservation.getStatus());
        if (!ACTIVE_RESERVATION_STATUSES.contains(status)) {
            return;
        }
        if (reservation.getReservedSlot() != null
                && "RESERVED".equalsIgnoreCase(reservation.getReservedSlot().getStatus())) {
            reservation.getReservedSlot().setStatus("AVAILABLE");
        }
        reservation.setStatus("CANCELLED");
    }

    private void cancelMonthlyPass(MonthlyParkingPass pass) {
        String status = normalized(pass.getStatus());
        if (!ACTIVE_PASS_STATUSES.contains(status)) {
            return;
        }
        if (pass.getReservedSlot() != null
                && "MONTHLY_OCCUPIED".equalsIgnoreCase(pass.getReservedSlot().getStatus())) {
            throw new BadRequestException("Không thể xóa xe đang chiếm ô vé tháng.");
        }
        String slotStatus = pass.getReservedSlot() == null ? "" : normalized(pass.getReservedSlot().getStatus());
        if (pass.getReservedSlot() != null
                && Set.of("MONTHLY_HELD", "MONTHLY_RESERVED").contains(slotStatus)) {
            pass.getReservedSlot().setStatus("AVAILABLE");
        }
        pass.setStatus("CANCELLED");
        if (!"PAID".equalsIgnoreCase(pass.getPaymentStatus())) {
            pass.setPaymentStatus("CANCELLED");
        }
    }

    private String normalized(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }
}
