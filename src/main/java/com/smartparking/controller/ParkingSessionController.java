package com.smartparking.controller;

import com.smartparking.model.enums.ParkingSessionStatus;
import com.smartparking.model.schemas.ParkingSession;
import com.smartparking.service.ParkingSessionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/parking-sessions")
public class ParkingSessionController {

    private final ParkingSessionService service;

    public ParkingSessionController(ParkingSessionService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<ParkingSession>> getAllSessions() {
        return ResponseEntity.ok(service.getAllSessions());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ParkingSession> getSessionById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getSessionById(id));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<ParkingSession>> getSessionsByStatus(@PathVariable ParkingSessionStatus status) {
        return ResponseEntity.ok(service.getSessionsByStatus(status));
    }

    @GetMapping("/vehicle/{vehicleId}")
    public ResponseEntity<List<ParkingSession>> getSessionsByVehicle(@PathVariable Long vehicleId) {
        return ResponseEntity.ok(service.getSessionsByVehicle(vehicleId));
    }

    @PostMapping("/check-in")
    public ResponseEntity<ParkingSession> checkIn(@RequestBody ParkingSession session) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.checkIn(session));
    }

    @PutMapping("/{id}/check-out")
    public ResponseEntity<ParkingSession> checkOut(
            @PathVariable Long id,
            @RequestParam Long exitStaffId,
            @RequestParam Long exitGateId) {
        return ResponseEntity.ok(service.checkOut(id, exitStaffId, exitGateId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ParkingSession> updateSession(
            @PathVariable Long id,
            @RequestBody ParkingSession session) {
        return ResponseEntity.ok(service.updateSession(id, session));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ParkingSession> updateStatus(
            @PathVariable Long id,
            @RequestParam ParkingSessionStatus status) {
        return ResponseEntity.ok(service.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSession(@PathVariable Long id) {
        service.deleteSession(id);
        return ResponseEntity.noContent().build();
    }
}