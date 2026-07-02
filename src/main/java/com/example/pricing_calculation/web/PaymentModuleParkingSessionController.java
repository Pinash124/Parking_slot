package com.example.pricing_calculation.web;

import com.example.pricing_calculation.dto.ParkingSessionResponse;
import com.example.pricing_calculation.dto.SessionCheckInRequest;
import com.example.pricing_calculation.dto.SessionCheckoutRequest;
import com.example.pricing_calculation.service.PaymentModuleParkingSessionService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.example.pricing_calculation.domain.UserAccount;
import com.example.pricing_calculation.domain.UserRole;
import com.example.pricing_calculation.service.PaymentModuleAuthService;
import com.example.pricing_calculation.service.PaymentCheckoutService;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/staff/parking-sessions")
@SecurityRequirement(name="bearerAuth")
@Tag(name="Parking Staff", description="Check-in, tinh phi, thu phi va xac nhan xe ra")
public class PaymentModuleParkingSessionController {

    private final PaymentModuleParkingSessionService parkingSessionService;
    private final PaymentModuleAuthService authService;
    private final PaymentCheckoutService paymentCheckoutService;
    private final com.example.pricing_calculation.service.QrCodeService qrCodeService;
    private final com.example.pricing_calculation.repository.VehicleRepository vehicleRepository;
    private final com.example.pricing_calculation.repository.ReservationRepository reservationRepository;
    private final com.example.pricing_calculation.repository.PaymentModuleVehicleTypeRepository vehicleTypeRepository;
    private final com.example.pricing_calculation.repository.UserAccountRepository userRepository;

    public PaymentModuleParkingSessionController(
            PaymentModuleParkingSessionService parkingSessionService,
            PaymentModuleAuthService authService,
            PaymentCheckoutService paymentCheckoutService,
            com.example.pricing_calculation.service.QrCodeService qrCodeService,
            com.example.pricing_calculation.repository.VehicleRepository vehicleRepository,
            com.example.pricing_calculation.repository.ReservationRepository reservationRepository,
            com.example.pricing_calculation.repository.PaymentModuleVehicleTypeRepository vehicleTypeRepository,
            com.example.pricing_calculation.repository.UserAccountRepository userRepository) {
        this.parkingSessionService = parkingSessionService;
        this.authService = authService;
        this.paymentCheckoutService = paymentCheckoutService;
        this.qrCodeService = qrCodeService;
        this.vehicleRepository = vehicleRepository;
        this.reservationRepository = reservationRepository;
        this.vehicleTypeRepository = vehicleTypeRepository;
        this.userRepository = userRepository;
    }

    private UserAccount staff(String header) {
        return authService.requireAnyRole(header, UserRole.PARKING_STAFF, UserRole.PARKING_MANAGER, UserRole.ADMINISTRATOR);
    }

    @GetMapping("/{id}")
    public ParkingSessionResponse getById(@RequestHeader("Authorization") String header, @PathVariable Long id) {
        staff(header);
        return parkingSessionService.getById(id);
    }

    @PostMapping("/check-in")
    public ResponseEntity<ParkingSessionResponse> checkIn(@RequestHeader("Authorization") String header,
            @RequestParam String entryGateCode, @RequestBody SessionCheckInRequest request) {
        UserAccount staff = staff(header);
        return ResponseEntity.status(HttpStatus.CREATED).body(parkingSessionService.checkIn(request, staff.getId(), entryGateCode));
    }

    @PostMapping("/{id}/checkout")
    public ParkingSessionResponse prepareCheckout(@RequestHeader("Authorization") String header,
            @PathVariable Long id,
            @RequestBody(required = false) SessionCheckoutRequest request) {
        staff(header);
        return parkingSessionService.checkout(id, request);
    }

    @PostMapping("/{id}/complete-exit")
    public ParkingSessionResponse completeExit(@RequestHeader("Authorization") String header,
            @PathVariable Long id, @RequestParam String exitGateCode) {
        UserAccount staff = staff(header);
        return paymentCheckoutService.completeExit(id, staff.getId(), exitGateCode);
    }

    @PostMapping(value = "/check-in/scan-qr", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ParkingSessionResponse> checkInWithQr(
            @RequestHeader("Authorization") String header,
            @RequestParam String entryGateCode,
            @RequestParam Long slotId,
            @RequestParam(required = false) String ticketCode,
            @RequestPart("file") org.springframework.web.multipart.MultipartFile file) {
        UserAccount staff = staff(header);
        String decodedText = qrCodeService.decodeQrCode(file);
        
        Long reservationId = null;
        Long vehicleId = null;

        if (decodedText.matches("^\\d+$")) {
            Long resId = Long.parseLong(decodedText);
            com.example.pricing_calculation.domain.Reservation reservation = reservationRepository.findById(resId)
                    .orElseThrow(() -> new com.example.pricing_calculation.service.ResourceNotFoundException("Reservation not found: " + resId));
            reservationId = reservation.getId();
            vehicleId = reservation.getVehicle().getId();
        } else {
            String plate = decodedText.trim().toUpperCase();
            java.util.Optional<com.example.pricing_calculation.domain.Vehicle> optVehicle = vehicleRepository.findByPlateNumberIgnoreCase(plate);
            
            if (optVehicle.isPresent()) {
                vehicleId = optVehicle.get().getId();
                java.util.List<com.example.pricing_calculation.domain.Reservation> activeReservations = 
                        reservationRepository.findByVehicleIdAndStatusIgnoreCase(vehicleId, "APPROVED");
                if (!activeReservations.isEmpty()) {
                    reservationId = activeReservations.get(0).getId();
                }
            } else {
                com.example.pricing_calculation.domain.Vehicle guestVehicle = new com.example.pricing_calculation.domain.Vehicle();
                guestVehicle.setPlateNumber(plate);
                
                com.example.pricing_calculation.domain.UserAccount owner = userRepository.findByEmailIgnoreCase("customer@example.com")
                        .orElse(staff);
                guestVehicle.setUser(owner);
                
                com.example.pricing_calculation.domain.VehicleTypeEntity defaultType = vehicleTypeRepository.findById(1L)
                        .orElseThrow(() -> new com.example.pricing_calculation.service.ResourceNotFoundException("Default vehicle type not found"));
                guestVehicle.setVehicleType(defaultType);
                guestVehicle.setBrand("Guest");
                guestVehicle.setColor("Unknown");
                guestVehicle.setStatus("ACTIVE");
                
                com.example.pricing_calculation.domain.Vehicle savedGuest = vehicleRepository.save(guestVehicle);
                vehicleId = savedGuest.getId();
            }
        }

        SessionCheckInRequest request = new SessionCheckInRequest(
                reservationId, vehicleId, slotId, ticketCode, java.time.LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(parkingSessionService.checkIn(request, staff.getId(), entryGateCode));
    }
}
