package com.example.pricing_calculation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.pricing_calculation.domain.MonthlyParkingPass;
import com.example.pricing_calculation.domain.PaymentModuleParkingSlot;
import com.example.pricing_calculation.domain.UserAccount;
import com.example.pricing_calculation.domain.Vehicle;
import com.example.pricing_calculation.domain.VehicleTypeEntity;
import com.example.pricing_calculation.domain.Zone;
import com.example.pricing_calculation.dto.MonthlyParkingPassDtos.MonthlyParkingPassCreateRequest;
import com.example.pricing_calculation.dto.MonthlyParkingPassDtos.MonthlyParkingPassPaymentRequest;
import com.example.pricing_calculation.repository.MonthlyParkingPassRepository;
import com.example.pricing_calculation.repository.PaymentModuleParkingSlotRepository;
import com.example.pricing_calculation.repository.VehicleRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MonthlyParkingPassServiceTest {

    @Test
    void rejectsMonthlyParkingForMotorbikes() {
        MonthlyParkingPassRepository passes = mock(MonthlyParkingPassRepository.class);
        VehicleRepository vehicles = mock(VehicleRepository.class);
        PaymentModuleParkingSlotRepository slots = mock(PaymentModuleParkingSlotRepository.class);
        PricingService pricing = mock(PricingService.class);
        MonthlyParkingPassService service = new MonthlyParkingPassService(passes, vehicles, slots, pricing);
        UserAccount user = mock(UserAccount.class);
        Vehicle vehicle = mock(Vehicle.class);
        VehicleTypeEntity type = mock(VehicleTypeEntity.class);
        when(user.getId()).thenReturn(1L);
        when(vehicle.getUser()).thenReturn(user);
        when(vehicle.getVehicleType()).thenReturn(type);
        when(type.getWheelCount()).thenReturn(2);
        when(vehicles.findById(10L)).thenReturn(Optional.of(vehicle));

        BadRequestException error = assertThrows(BadRequestException.class, () -> service.register(user,
                new MonthlyParkingPassCreateRequest(10L, 20L, LocalDate.now().plusDays(1), 1, null)));

        assertEquals("Only cars can register a monthly parking slot", error.getMessage());
    }

    @Test
    void returnsTheSelectedSlotAndWaitsForPrepayment() {
        MonthlyParkingPassRepository passes = mock(MonthlyParkingPassRepository.class);
        VehicleRepository vehicles = mock(VehicleRepository.class);
        PaymentModuleParkingSlotRepository slots = mock(PaymentModuleParkingSlotRepository.class);
        PricingService pricing = mock(PricingService.class);
        MonthlyParkingPassService service = new MonthlyParkingPassService(passes, vehicles, slots, pricing);

        UserAccount user = mock(UserAccount.class);
        Vehicle vehicle = mock(Vehicle.class);
        VehicleTypeEntity type = mock(VehicleTypeEntity.class);
        Zone zone = mock(Zone.class);
        PaymentModuleParkingSlot slot = mock(PaymentModuleParkingSlot.class);
        when(user.getId()).thenReturn(1L);
        when(vehicle.getId()).thenReturn(10L);
        when(vehicle.getUser()).thenReturn(user);
        when(vehicle.getVehicleType()).thenReturn(type);
        when(vehicle.getPlateNumber()).thenReturn("59A-12345");
        when(type.getId()).thenReturn(2L);
        when(type.getName()).thenReturn("CAR");
        when(slot.getId()).thenReturn(20L);
        when(slot.getSlotCode()).thenReturn("CAR-A01");
        when(slot.getStatus()).thenReturn("AVAILABLE");
        when(slot.getZone()).thenReturn(zone);
        when(zone.getVehicleType()).thenReturn(type);
        when(zone.getZoneType()).thenReturn("CAR_MONTHLY");
        when(vehicles.findById(10L)).thenReturn(Optional.of(vehicle));
        when(slots.findByIdForUpdate(20L)).thenReturn(Optional.of(slot));
        when(passes.findByVehicleIdOrderByCreatedAtDesc(10L)).thenReturn(List.of());
        when(passes.findAll()).thenReturn(List.of());
        when(pricing.monthlyRateForVehicleType(any(), any())).thenReturn(new BigDecimal("500000"));
        when(passes.save(any(MonthlyParkingPass.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.register(user,
                new MonthlyParkingPassCreateRequest(10L, 20L, LocalDate.now().plusDays(1), 1, null));

        assertEquals(20L, response.slotId());
        assertEquals("CAR-A01", response.slotCode());
        assertEquals("PENDING_PAYMENT", response.status());
        assertEquals("PENDING", response.paymentStatus());
        verify(slot).setStatus("MONTHLY_HELD");
    }

    @Test
    void rejectsCashPaymentForMonthlyPasses() {
        MonthlyParkingPassRepository passes = mock(MonthlyParkingPassRepository.class);
        VehicleRepository vehicles = mock(VehicleRepository.class);
        PaymentModuleParkingSlotRepository slots = mock(PaymentModuleParkingSlotRepository.class);
        PricingService pricing = mock(PricingService.class);
        MonthlyParkingPassService service = new MonthlyParkingPassService(passes, vehicles, slots, pricing);
        MonthlyParkingPass pass = mock(MonthlyParkingPass.class);
        when(pass.getPaymentStatus()).thenReturn("PENDING");
        when(pass.getStatus()).thenReturn("PENDING_PAYMENT");
        when(passes.findById(99L)).thenReturn(Optional.of(pass));

        BadRequestException error = assertThrows(BadRequestException.class,
                () -> service.confirmPayment(99L, new MonthlyParkingPassPaymentRequest("CASH", "CASH-001")));

        assertEquals("Monthly pass only supports ONLINE_QR transfer payment", error.getMessage());
    }
}
