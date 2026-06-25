package com.example.pricing_calculation.web;

import com.example.pricing_calculation.dto.AvailableSlotResponse;
import com.example.pricing_calculation.dto.ParkingFacilityInfoResponse;
import com.example.pricing_calculation.service.ParkingInfoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/parking-info")
@Tag(name = "Parking Info", description = "Thong tin bai xe, quy dinh va slot trong cho user")
public class ParkingInfoController {

    private final ParkingInfoService parkingInfoService;

    public ParkingInfoController(ParkingInfoService parkingInfoService) {
        this.parkingInfoService = parkingInfoService;
    }

    @GetMapping
    public ParkingFacilityInfoResponse info() {
        return parkingInfoService.getFacilityInfo();
    }

    @GetMapping("/available-slots")
    public List<AvailableSlotResponse> availableSlots(
            @RequestParam(required = false) Long zoneId,
            @RequestParam(required = false) Long vehicleTypeId) {
        return parkingInfoService.availableSlots(zoneId, vehicleTypeId);
    }
}
