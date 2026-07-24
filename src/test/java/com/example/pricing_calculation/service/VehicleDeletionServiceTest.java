package com.example.pricing_calculation.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.pricing_calculation.domain.MonthlyParkingPass;
import com.example.pricing_calculation.domain.PaymentModuleParkingSlot;
import com.example.pricing_calculation.domain.Reservation;
import com.example.pricing_calculation.domain.Vehicle;
import com.example.pricing_calculation.repository.MonthlyParkingPassRepository;
import com.example.pricing_calculation.repository.PaymentModuleParkingSessionRepository;
import com.example.pricing_calculation.repository.ReservationRepository;
import com.example.pricing_calculation.repository.VehicleRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

class VehicleDeletionServiceTest {

    @Test
    void refusesToDeleteVehicleWithOpenParkingSession() {
        Fixture fixture = new Fixture();
        Vehicle vehicle = mock(Vehicle.class);
        when(vehicle.getId()).thenReturn(10L);
        when(fixture.sessions.countByVehicleIdAndStatusIn(10L,
                List.of("ACTIVE", "PAYMENT_PENDING", "PENDING_PAYMENT"))).thenReturn(1L);

        assertThrows(BadRequestException.class, () -> fixture.service.delete(vehicle));

        verify(fixture.vehicles, never()).delete(vehicle);
    }

    @Test
    void cancelsActiveItemsButKeepsPaidHistoryAndRevenue() {
        Fixture fixture = new Fixture();
        Vehicle vehicle = mock(Vehicle.class);
        Reservation reservation = mock(Reservation.class);
        MonthlyParkingPass pass = mock(MonthlyParkingPass.class);
        PaymentModuleParkingSlot reservationSlot = mock(PaymentModuleParkingSlot.class);
        PaymentModuleParkingSlot monthlySlot = mock(PaymentModuleParkingSlot.class);

        when(vehicle.getId()).thenReturn(10L);
        when(reservation.getStatus()).thenReturn("APPROVED");
        when(reservation.getReservedSlot()).thenReturn(reservationSlot);
        when(reservationSlot.getStatus()).thenReturn("RESERVED");
        when(pass.getStatus()).thenReturn("ACTIVE");
        when(pass.getPaymentStatus()).thenReturn("PAID");
        when(pass.getReservedSlot()).thenReturn(monthlySlot);
        when(monthlySlot.getStatus()).thenReturn("MONTHLY_RESERVED");
        when(fixture.reservations.findByVehicleIdOrderByStartTimeDesc(10L)).thenReturn(List.of(reservation));
        when(fixture.monthlyPasses.findByVehicleIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(pass));

        fixture.service.delete(vehicle);

        verify(fixture.sessions, never()).deleteAll(anyList());
        verify(reservationSlot).setStatus("AVAILABLE");
        verify(reservation).setStatus("CANCELLED");
        verify(fixture.reservations).saveAll(List.of(reservation));
        verify(monthlySlot).setStatus("AVAILABLE");
        verify(pass).setStatus("CANCELLED");
        verify(pass, never()).setPaymentStatus("CANCELLED");
        verify(fixture.monthlyPasses).saveAll(List.of(pass));
        verify(vehicle).setStatus("DELETED");
        verify(fixture.vehicles).saveAndFlush(vehicle);
        verify(fixture.vehicles, never()).delete(vehicle);
    }

    private static final class Fixture {
        private final VehicleRepository vehicles = mock(VehicleRepository.class);
        private final PaymentModuleParkingSessionRepository sessions =
                mock(PaymentModuleParkingSessionRepository.class);
        private final ReservationRepository reservations = mock(ReservationRepository.class);
        private final MonthlyParkingPassRepository monthlyPasses = mock(MonthlyParkingPassRepository.class);
        private final VehicleDeletionService service = new VehicleDeletionService(
                vehicles, sessions, reservations, monthlyPasses);
    }
}
