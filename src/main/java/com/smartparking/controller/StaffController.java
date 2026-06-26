package com.smartparking.controller;

import com.smartparking.model.requests.SlotStatusUpdateRequest;
import com.smartparking.model.requests.StaffCheckInRequest;
import com.smartparking.model.requests.StaffCheckOutRequest;
import com.smartparking.model.schemas.ParkingIncident;
import com.smartparking.model.schemas.ParkingSession;
import com.smartparking.model.schemas.ParkingSlot;
import com.smartparking.service.StaffService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/staff")
public class StaffController {

    private final StaffService staffService;

    public StaffController(StaffService staffService) {
        this.staffService = staffService;
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<ParkingSession>> getSessions(
            @Nullable @RequestParam(required = false) String status,
            @Nullable @RequestParam(required = false) Long vehicleId) {
        return ResponseEntity.ok(staffService.getSessions(status, vehicleId));
    }

    @GetMapping("/sessions/{id}")
    public ResponseEntity<ParkingSession> getSessionById(@PathVariable Long id) {
        return ResponseEntity.ok(staffService.getSessionById(id));
    }

    @GetMapping("/sessions/by-ticket")
    public ResponseEntity<ParkingSession> getSessionByTicketCode(@RequestParam String ticketCode) {
        return ResponseEntity.ok(staffService.getSessionByTicketCode(ticketCode));
    }

    @PostMapping("/sessions/check-in")
    public ResponseEntity<ParkingSession> createSession(@Valid @RequestBody StaffCheckInRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(staffService.createSession(request));
    }

    @PostMapping("/sessions/{id}/check-out")
    public ResponseEntity<ParkingSession> checkOut(
            @PathVariable Long id,
            @Valid @RequestBody StaffCheckOutRequest request) {
        return ResponseEntity.ok(staffService.checkOut(id, request));
    }

    @GetMapping("/slots")
    public ResponseEntity<List<ParkingSlot>> getSlots(
            @Nullable @RequestParam(required = false) String status,
            @Nullable @RequestParam(required = false) Long zoneId) {
        return ResponseEntity.ok(staffService.getSlots(status, zoneId));
    }

    @PatchMapping("/slots/{id}/status")
    public ResponseEntity<ParkingSlot> updateSlotStatus(
            @PathVariable Long id,
            @Valid @RequestBody SlotStatusUpdateRequest request) {
        return ResponseEntity.ok(staffService.updateSlotStatus(id, request));
    }

    @GetMapping("/incidents")
    public ResponseEntity<List<ParkingIncident>> getIncidents(
            @Nullable @RequestParam(required = false) String status) {
        return ResponseEntity.ok(staffService.getIncidents(status));
    }

    @PostMapping("/incidents")
    public ResponseEntity<ParkingIncident> createIncident(@RequestBody ParkingIncident incident) {
        return ResponseEntity.status(HttpStatus.CREATED).body(staffService.createIncident(incident));
    }

    @PatchMapping("/incidents/{id}/resolve")
    public ResponseEntity<ParkingIncident> resolveIncident(@PathVariable Long id) {
        return ResponseEntity.ok(staffService.resolveIncident(id));
    }
}
