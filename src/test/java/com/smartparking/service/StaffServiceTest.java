package com.smartparking.service;

import com.smartparking.model.enums.ParkingSessionStatus;
import com.smartparking.model.enums.ParkingSlotStatus;
import com.smartparking.model.requests.StaffCheckInRequest;
import com.smartparking.model.requests.StaffCheckOutRequest;
import com.smartparking.model.schemas.ParkingSession;
import com.smartparking.model.schemas.ParkingSlot;
import com.smartparking.repository.ParkingIncidentRepository;
import com.smartparking.repository.ParkingSessionRepository;
import com.smartparking.repository.ParkingSlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffServiceTest {

    @Mock
    private ParkingSessionRepository parkingSessionRepository;

    @Mock
    private ParkingSlotRepository parkingSlotRepository;

    @Mock
    private ParkingIncidentRepository parkingIncidentRepository;

    private StaffService staffService;

    @BeforeEach
    void setUp() {
        staffService = new StaffService(
                parkingSessionRepository,
                parkingSlotRepository,
                parkingIncidentRepository
        );
    }

    @Test
    void checkInOccupiesAvailableSlotAndCreatesActiveSession() {
        ParkingSlot slot = slot(5L, ParkingSlotStatus.AVAILABLE);
        StaffCheckInRequest request = checkInRequest(11L, 5L);

        when(parkingSlotRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(slot));
        when(parkingSessionRepository.existsBySlotIdAndStatusIn(any(), anyCollection())).thenReturn(false);
        when(parkingSessionRepository.existsByVehicleIdAndStatusIn(any(), anyCollection())).thenReturn(false);
        when(parkingSessionRepository.save(any(ParkingSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ParkingSession session = staffService.createSession(request);

        assertEquals(ParkingSlotStatus.OCCUPIED.name(), slot.getStatus());
        assertEquals(ParkingSessionStatus.ACTIVE.name(), session.getStatus());
        assertEquals(11L, session.getVehicleId());
        assertEquals(5L, session.getSlotId());
        verify(parkingSlotRepository).save(slot);
    }

    @Test
    void checkInRejectsReservedSlotWithoutReservation() {
        ParkingSlot slot = slot(5L, ParkingSlotStatus.RESERVED);
        StaffCheckInRequest request = checkInRequest(11L, 5L);
        when(parkingSlotRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(slot));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> staffService.createSession(request)
        );

        assertEquals("Reserved slot requires a matching reservation", error.getMessage());
        verify(parkingSessionRepository, never()).save(any());
    }

    @Test
    void checkInRejectsSlotThatAlreadyHasActiveVehicle() {
        ParkingSlot slot = slot(5L, ParkingSlotStatus.AVAILABLE);
        StaffCheckInRequest request = checkInRequest(11L, 5L);
        when(parkingSlotRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(slot));
        when(parkingSessionRepository.existsBySlotIdAndStatusIn(any(), anyCollection())).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> staffService.createSession(request));

        verify(parkingSessionRepository, never()).save(any());
        verify(parkingSlotRepository, never()).save(any());
    }

    @Test
    void checkOutRejectsCompletedSession() {
        ParkingSession session = new ParkingSession();
        session.setSessionId(7L);
        session.setStatus(ParkingSessionStatus.COMPLETED.name());
        session.setExitTime(LocalDateTime.now());
        when(parkingSessionRepository.findById(7L)).thenReturn(Optional.of(session));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> staffService.checkOut(7L, checkOutRequest())
        );

        assertEquals("Parking session has already been checked out", error.getMessage());
        verify(parkingSlotRepository, never()).save(any());
    }

    @Test
    void checkOutReleasesSlotAndCompletesSession() {
        ParkingSession session = new ParkingSession();
        session.setSessionId(7L);
        session.setSlotId(5L);
        session.setStatus(ParkingSessionStatus.ACTIVE.name());
        ParkingSlot slot = slot(5L, ParkingSlotStatus.OCCUPIED);

        when(parkingSessionRepository.findById(7L)).thenReturn(Optional.of(session));
        when(parkingSlotRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(slot));
        when(parkingSessionRepository.save(any(ParkingSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ParkingSession result = staffService.checkOut(7L, checkOutRequest());

        assertEquals(ParkingSessionStatus.COMPLETED.name(), result.getStatus());
        assertEquals(ParkingSlotStatus.AVAILABLE.name(), slot.getStatus());
        assertEquals(new BigDecimal("15.00"), result.getTotalFee());
        verify(parkingSlotRepository).save(slot);
    }

    private StaffCheckInRequest checkInRequest(Long vehicleId, Long slotId) {
        StaffCheckInRequest request = new StaffCheckInRequest();
        request.setVehicleId(vehicleId);
        request.setSlotId(slotId);
        request.setEntryStaffId(2L);
        request.setEntryGateId(3L);
        return request;
    }

    private StaffCheckOutRequest checkOutRequest() {
        StaffCheckOutRequest request = new StaffCheckOutRequest();
        request.setExitStaffId(2L);
        request.setExitGateId(4L);
        request.setParkingFee(new BigDecimal("10.00"));
        request.setPenaltyFee(new BigDecimal("5.00"));
        return request;
    }

    private ParkingSlot slot(Long id, ParkingSlotStatus status) {
        ParkingSlot slot = new ParkingSlot();
        slot.setSlotId(id);
        slot.setStatus(status.name());
        return slot;
    }
}
