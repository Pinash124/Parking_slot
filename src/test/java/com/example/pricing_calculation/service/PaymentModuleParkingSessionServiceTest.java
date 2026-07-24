package com.example.pricing_calculation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.pricing_calculation.domain.PaymentModuleParkingSession;
import com.example.pricing_calculation.domain.PaymentModuleParkingSlot;
import com.example.pricing_calculation.domain.Reservation;
import com.example.pricing_calculation.domain.Vehicle;
import com.example.pricing_calculation.domain.VehicleTypeEntity;
import com.example.pricing_calculation.domain.Zone;
import com.example.pricing_calculation.dto.ParkingSessionResponse;
import com.example.pricing_calculation.dto.SessionCheckInRequest;
import com.example.pricing_calculation.repository.PaymentModuleParkingSessionRepository;
import com.example.pricing_calculation.repository.PaymentModuleParkingSlotRepository;
import com.example.pricing_calculation.repository.PaymentModuleVehicleTypeRepository;
import com.example.pricing_calculation.repository.MonthlyParkingPassRepository;
import com.example.pricing_calculation.config.ParkingRuleProperties;
import com.example.pricing_calculation.domain.MonthlyParkingPass;
import com.example.pricing_calculation.repository.ReservationRepository;
import com.example.pricing_calculation.repository.SessionServiceUsageRepository;
import com.example.pricing_calculation.repository.UserAccountRepository;
import com.example.pricing_calculation.repository.VehicleRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PaymentModuleParkingSessionServiceTest {

    @Test
    void checksInEarlyUsingSlotSelectedOnReservation() {
        PaymentModuleParkingSessionRepository sessions = mock(PaymentModuleParkingSessionRepository.class);
        ReservationRepository reservations = mock(ReservationRepository.class);
        VehicleRepository vehicles = mock(VehicleRepository.class);
        PaymentModuleParkingSlotRepository slots = mock(PaymentModuleParkingSlotRepository.class);
        PricingService pricing = mock(PricingService.class);
        RealtimeEventService realtime = mock(RealtimeEventService.class);
        SessionServiceUsageRepository usages = mock(SessionServiceUsageRepository.class);
        UserAccountRepository users = mock(UserAccountRepository.class);
        PaymentModuleVehicleTypeRepository vehicleTypes = mock(PaymentModuleVehicleTypeRepository.class);
        MonthlyParkingPassRepository monthlyPasses = mock(MonthlyParkingPassRepository.class);
        ParkingRuleProperties rules = new ParkingRuleProperties();
        QrCodeService qrCodeService = mock(QrCodeService.class);
        PaymentModuleParkingSessionService service = new PaymentModuleParkingSessionService(
                sessions, reservations, vehicles, slots, pricing, realtime, usages, users, vehicleTypes,
                monthlyPasses, rules, qrCodeService);

        Vehicle vehicle = mock(Vehicle.class);
        VehicleTypeEntity vehicleType = mock(VehicleTypeEntity.class);
        Zone zone = mock(Zone.class);
        Reservation reservation = mock(Reservation.class);
        PaymentModuleParkingSlot selectedSlot = mock(PaymentModuleParkingSlot.class);
        LocalDateTime earlyArrival = LocalDateTime.of(2026, 7, 5, 9, 40);

        when(vehicles.findByPlateNumberIgnoreCase("61B-33345")).thenReturn(Optional.of(vehicle));
        when(vehicle.getId()).thenReturn(10L);
        when(vehicle.getPlateNumber()).thenReturn("61B-33345");
        when(vehicle.getVehicleType()).thenReturn(vehicleType);
        when(vehicleType.getId()).thenReturn(1L);
        when(vehicleType.getName()).thenReturn("CAR");
        when(monthlyPasses.findByVehicleIdOrderByCreatedAtDesc(10L)).thenReturn(List.of());
        when(reservations.findByVehicleIdAndStatusInOrderByStartTimeAsc(10L, List.of("PENDING", "APPROVED")))
                .thenReturn(List.of(reservation));
        when(reservation.getId()).thenReturn(30L);
        when(reservation.getVehicle()).thenReturn(vehicle);
        when(reservation.getZone()).thenReturn(zone);
        when(reservation.getReservedSlot()).thenReturn(selectedSlot);
        when(reservation.getStatus()).thenReturn("APPROVED");
        when(reservation.getStartTime()).thenReturn(LocalDateTime.of(2026, 7, 5, 10, 0));
        when(zone.getId()).thenReturn(5L);
        when(zone.getVehicleType()).thenReturn(vehicleType);
        when(zone.getZoneType()).thenReturn("CAR_NORMAL");
        when(zone.getZoneName()).thenReturn("CAR ZONE");
        when(selectedSlot.getId()).thenReturn(20L);
        when(selectedSlot.getStatus()).thenReturn("RESERVED");
        when(selectedSlot.getZone()).thenReturn(zone);
        when(selectedSlot.getSlotCode()).thenReturn("A-01");
        when(slots.findByIdForUpdate(20L)).thenReturn(Optional.of(selectedSlot));
        when(sessions.countByVehicleIdAndStatusIn(anyLong(), any())).thenReturn(0L);
        when(sessions.save(any(PaymentModuleParkingSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ParkingSessionResponse response = service.checkIn(
                new SessionCheckInRequest(null, null, 999L, null, earlyArrival, "61B-33345"),
                7L,
                "GATE_IN_01");

        assertEquals(20L, response.slotId());
        assertEquals(30L, response.reservationId());
        assertEquals("ACTIVE", response.status());
        verify(reservation).setStatus("CONFIRMED");
        verify(selectedSlot).setStatus("OCCUPIED");
        verify(slots).findByIdForUpdate(20L);
        verify(slots, never()).findByIdForUpdate(999L);
    }
    @Test
    void explicitReservationQrTooEarlyDoesNotFallbackToRandomSlot() {
        PaymentModuleParkingSessionRepository sessions = mock(PaymentModuleParkingSessionRepository.class);
        ReservationRepository reservations = mock(ReservationRepository.class);
        VehicleRepository vehicles = mock(VehicleRepository.class);
        PaymentModuleParkingSlotRepository slots = mock(PaymentModuleParkingSlotRepository.class);
        PricingService pricing = mock(PricingService.class);
        RealtimeEventService realtime = mock(RealtimeEventService.class);
        SessionServiceUsageRepository usages = mock(SessionServiceUsageRepository.class);
        UserAccountRepository users = mock(UserAccountRepository.class);
        PaymentModuleVehicleTypeRepository vehicleTypes = mock(PaymentModuleVehicleTypeRepository.class);
        MonthlyParkingPassRepository monthlyPasses = mock(MonthlyParkingPassRepository.class);
        ParkingRuleProperties rules = new ParkingRuleProperties();
        QrCodeService qrCodeService = mock(QrCodeService.class);
        PaymentModuleParkingSessionService service = new PaymentModuleParkingSessionService(
                sessions, reservations, vehicles, slots, pricing, realtime, usages, users, vehicleTypes,
                monthlyPasses, rules, qrCodeService);

        Vehicle vehicle = mock(Vehicle.class);
        Reservation reservation = mock(Reservation.class);
        LocalDateTime tooEarly = LocalDateTime.of(2026, 7, 5, 9, 0);

        when(reservations.findById(30L)).thenReturn(Optional.of(reservation));
        when(reservation.getVehicle()).thenReturn(vehicle);
        when(reservation.getStatus()).thenReturn("APPROVED");
        when(reservation.getStartTime()).thenReturn(LocalDateTime.of(2026, 7, 5, 10, 0));
        when(vehicle.getId()).thenReturn(10L);

        assertThrows(BadRequestException.class, () -> service.checkIn(
                new SessionCheckInRequest(30L, null, 999L, null, tooEarly, null),
                7L,
                "GATE_IN_01"));

        verify(slots, never()).findByIdForUpdate(anyLong());
        verify(sessions, never()).save(any(PaymentModuleParkingSession.class));
        verify(reservations, times(2)).findById(30L);
    }

    @Test
    void twoWheelMonthlyPassChecksInWithoutFixedSlot() {
        PaymentModuleParkingSessionRepository sessions = mock(PaymentModuleParkingSessionRepository.class);
        ReservationRepository reservations = mock(ReservationRepository.class);
        VehicleRepository vehicles = mock(VehicleRepository.class);
        PaymentModuleParkingSlotRepository slots = mock(PaymentModuleParkingSlotRepository.class);
        PricingService pricing = mock(PricingService.class);
        RealtimeEventService realtime = mock(RealtimeEventService.class);
        SessionServiceUsageRepository usages = mock(SessionServiceUsageRepository.class);
        UserAccountRepository users = mock(UserAccountRepository.class);
        PaymentModuleVehicleTypeRepository vehicleTypes = mock(PaymentModuleVehicleTypeRepository.class);
        MonthlyParkingPassRepository monthlyPasses = mock(MonthlyParkingPassRepository.class);
        ParkingRuleProperties rules = new ParkingRuleProperties();
        QrCodeService qrCodeService = mock(QrCodeService.class);
        PaymentModuleParkingSessionService service = new PaymentModuleParkingSessionService(
                sessions, reservations, vehicles, slots, pricing, realtime, usages, users, vehicleTypes,
                monthlyPasses, rules, qrCodeService);

        Vehicle vehicle = mock(Vehicle.class);
        VehicleTypeEntity vehicleType = mock(VehicleTypeEntity.class);
        Zone zone = mock(Zone.class);
        PaymentModuleParkingSlot selectedSlot = mock(PaymentModuleParkingSlot.class);
        MonthlyParkingPass monthlyPass = mock(MonthlyParkingPass.class);
        LocalDateTime entryTime = LocalDateTime.of(2026, 7, 20, 8, 30);

        when(vehicles.findByPlateNumberIgnoreCase("59A1-12345")).thenReturn(Optional.of(vehicle));
        when(vehicle.getId()).thenReturn(10L);
        when(vehicle.getPlateNumber()).thenReturn("59A1-12345");
        when(vehicle.getVehicleType()).thenReturn(vehicleType);
        when(vehicleType.getId()).thenReturn(2L);
        when(vehicleType.getName()).thenReturn("Xe 2 banh");
        when(vehicleType.getWheelCount()).thenReturn(2);
        when(monthlyPasses.findByVehicleIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(monthlyPass));
        when(monthlyPass.isActiveAt(entryTime.toLocalDate())).thenReturn(true);
        when(monthlyPass.getReservedSlot()).thenReturn(null);
        when(monthlyPass.getId()).thenReturn(99L);
        when(reservations.findByVehicleIdAndStatusInOrderByStartTimeAsc(10L, List.of("PENDING", "APPROVED")))
                .thenReturn(List.of());
        when(slots.searchAvailableSlots(null, 2L, "AVAILABLE")).thenReturn(List.of(selectedSlot));
        when(selectedSlot.getId()).thenReturn(20L);
        when(selectedSlot.getStatus()).thenReturn("AVAILABLE");
        when(selectedSlot.getZone()).thenReturn(zone);
        when(selectedSlot.getSlotCode()).thenReturn("F1-XE 2 BANH-001");
        when(zone.getId()).thenReturn(5L);
        when(zone.getVehicleType()).thenReturn(vehicleType);
        when(zone.getZoneType()).thenReturn("MOTORBIKE");
        when(zone.getZoneName()).thenReturn("F1 - Xe 2 banh");
        when(slots.findByIdForUpdate(20L)).thenReturn(Optional.of(selectedSlot));
        when(sessions.countByVehicleIdAndStatusIn(10L, List.of("ACTIVE", "PAYMENT_PENDING"))).thenReturn(0L);
        when(sessions.save(any(PaymentModuleParkingSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ParkingSessionResponse response = service.checkIn(
                new SessionCheckInRequest(null, null, null, null, entryTime, "59A1-12345"),
                7L,
                "GATE_IN_01");

        assertEquals(20L, response.slotId());
        assertEquals(99L, response.monthlyPassId());
        assertEquals("ACTIVE", response.status());
        verify(selectedSlot).setStatus("MONTHLY_OCCUPIED");
        verify(slots).findByIdForUpdate(20L);
    }
}
