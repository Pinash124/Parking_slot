package com.example.pricing_calculation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.pricing_calculation.domain.PaymentModuleParkingSlot;
import com.example.pricing_calculation.domain.VehicleTypeEntity;
import com.example.pricing_calculation.domain.Zone;
import com.example.pricing_calculation.repository.BuildingRepository;
import com.example.pricing_calculation.repository.PaymentModuleParkingSlotRepository;
import com.example.pricing_calculation.repository.PaymentModulePricingPolicyRepository;
import com.example.pricing_calculation.repository.PaymentModuleVehicleTypeRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

class ParkingInfoServiceTest {

    @Test
    void completelySeparatesReservationAndMonthlyCatalogs() {
        BuildingRepository buildings = mock(BuildingRepository.class);
        PaymentModuleVehicleTypeRepository vehicleTypes = mock(PaymentModuleVehicleTypeRepository.class);
        PaymentModulePricingPolicyRepository policies = mock(PaymentModulePricingPolicyRepository.class);
        PaymentModuleParkingSlotRepository slots = mock(PaymentModuleParkingSlotRepository.class);
        ParkingInfoService service = new ParkingInfoService(buildings, vehicleTypes, policies, slots,
                "24/7", "SmartParking", "Address");

        PaymentModuleParkingSlot normalSlot = slot(11L, "F1-CAR-011", "CAR_NORMAL");
        PaymentModuleParkingSlot monthlySlot = slot(1L, "F1-CAR-001", "CAR_MONTHLY");
        when(slots.searchAvailableSlots(null, 1L, "AVAILABLE"))
                .thenReturn(List.of(normalSlot, monthlySlot));

        var reservation = service.availableSlots(null, 1L, "RESERVATION");
        var monthly = service.availableSlots(null, 1L, "MONTHLY");

        assertEquals(List.of("F1-CAR-011"), reservation.stream().map(x -> x.slotCode()).toList());
        assertEquals(List.of("F1-CAR-001"), monthly.stream().map(x -> x.slotCode()).toList());
    }

    private PaymentModuleParkingSlot slot(Long id, String code, String zoneType) {
        PaymentModuleParkingSlot slot = mock(PaymentModuleParkingSlot.class);
        Zone zone = mock(Zone.class);
        VehicleTypeEntity type = mock(VehicleTypeEntity.class);
        when(slot.getId()).thenReturn(id);
        when(slot.getSlotCode()).thenReturn(code);
        when(slot.getStatus()).thenReturn("AVAILABLE");
        when(slot.getZone()).thenReturn(zone);
        when(zone.getId()).thenReturn(zoneType.equals("CAR_MONTHLY") ? 2L : 1L);
        when(zone.getZoneName()).thenReturn(zoneType.equals("CAR_MONTHLY") ? "F1-CAR-MONTHLY" : "F1-CAR-NORMAL");
        when(zone.getZoneType()).thenReturn(zoneType);
        when(zone.getVehicleType()).thenReturn(type);
        when(type.getId()).thenReturn(1L);
        when(type.getName()).thenReturn("CAR");
        return slot;
    }
}
