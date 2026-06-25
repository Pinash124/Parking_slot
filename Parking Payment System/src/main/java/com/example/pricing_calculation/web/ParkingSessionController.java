package com.example.pricing_calculation.web;

import com.example.pricing_calculation.dto.ParkingSessionResponse;
import com.example.pricing_calculation.dto.SessionCheckInRequest;
import com.example.pricing_calculation.dto.SessionCheckoutRequest;
import com.example.pricing_calculation.service.ParkingSessionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/parking-sessions")
public class ParkingSessionController {

    private final ParkingSessionService parkingSessionService;

    public ParkingSessionController(ParkingSessionService parkingSessionService) {
        this.parkingSessionService = parkingSessionService;
    }

    @GetMapping("/{id}")
    public ParkingSessionResponse getById(@PathVariable Long id) {
        return parkingSessionService.getById(id);
    }

    @PostMapping("/check-in")
    public ResponseEntity<ParkingSessionResponse> checkIn(@RequestBody SessionCheckInRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(parkingSessionService.checkIn(request));
    }

    @PostMapping("/{id}/checkout")
    public ParkingSessionResponse checkout(
            @PathVariable Long id,
            @RequestBody(required = false) SessionCheckoutRequest request) {
        return parkingSessionService.checkout(id, request);
    }
}
