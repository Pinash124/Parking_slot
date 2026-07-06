package com.example.pricing_calculation.web;

import com.example.pricing_calculation.dto.PaymentCheckoutPrepareRequest;
import com.example.pricing_calculation.dto.PaymentCheckoutResponse;
import com.example.pricing_calculation.dto.PaymentExitValidationRequest;
import com.example.pricing_calculation.dto.PaymentExitValidationResponse;
import com.example.pricing_calculation.service.PaymentCheckoutService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/payment-checkout")
public class PaymentCheckoutController {

    private final PaymentCheckoutService paymentCheckoutService;
    private final com.example.pricing_calculation.service.QrCodeService qrCodeService;
    private final com.example.pricing_calculation.repository.PaymentModuleParkingSessionRepository sessionsRepository;
    private final com.example.pricing_calculation.repository.VehicleRepository vehicleRepository;

    public PaymentCheckoutController(
            PaymentCheckoutService paymentCheckoutService,
            com.example.pricing_calculation.service.QrCodeService qrCodeService,
            com.example.pricing_calculation.repository.PaymentModuleParkingSessionRepository sessionsRepository,
            com.example.pricing_calculation.repository.VehicleRepository vehicleRepository) {
        this.paymentCheckoutService = paymentCheckoutService;
        this.qrCodeService = qrCodeService;
        this.sessionsRepository = sessionsRepository;
        this.vehicleRepository = vehicleRepository;
    }

    @PostMapping("/prepare")
    public PaymentCheckoutResponse prepare(@RequestBody PaymentCheckoutPrepareRequest request) {
        return paymentCheckoutService.prepare(request);
    }

    @GetMapping("/sessions/{sessionId}/status")
    public PaymentCheckoutResponse status(@PathVariable Long sessionId) {
        return paymentCheckoutService.status(sessionId);
    }

    @PostMapping("/validate-exit")
    public PaymentExitValidationResponse validateExit(@RequestBody PaymentExitValidationRequest request) {
        return paymentCheckoutService.validateExit(request);
    }

    @PostMapping(value = "/prepare/scan-qr", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public PaymentCheckoutResponse prepareWithQr(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam(defaultValue = "false") boolean lostTicket,
            @RequestParam(defaultValue = "0") Integer overtimeMinutes) {
        String decodedText = qrCodeService.decodeQrCode(file);
        String plate = plateFromQrOrText(decodedText);
        if (plate.toUpperCase().startsWith("TICKET-")) {
            final String ticketCode = plate;
            com.example.pricing_calculation.domain.PaymentModuleParkingSession session = sessionsRepository.findByTicketCodeIgnoreCase(ticketCode)
                    .orElseThrow(() -> new com.example.pricing_calculation.service.ResourceNotFoundException("Parking session not found for ticket: " + ticketCode));
            plate = session.getVehicle().getPlateNumber();
        }
        
        PaymentCheckoutPrepareRequest request = new PaymentCheckoutPrepareRequest(
                plate, java.time.LocalDateTime.now(), lostTicket, overtimeMinutes
        );
        return paymentCheckoutService.prepare(request);
    }

    @PostMapping(value = "/validate-exit/scan-qr", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public PaymentExitValidationResponse validateExitWithQr(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        String decodedText = qrCodeService.decodeQrCode(file);
        String plate = plateFromQrOrText(decodedText);
        if (plate.toUpperCase().startsWith("TICKET-")) {
            final String ticketCode = plate;
            com.example.pricing_calculation.domain.PaymentModuleParkingSession session = sessionsRepository.findByTicketCodeIgnoreCase(ticketCode)
                    .orElseThrow(() -> new com.example.pricing_calculation.service.ResourceNotFoundException("Parking session not found for ticket: " + ticketCode));
            plate = session.getVehicle().getPlateNumber();
        }
        
        PaymentExitValidationRequest request = new PaymentExitValidationRequest(
                plate, java.time.LocalDateTime.now()
        );
        return paymentCheckoutService.validateExit(request);
    }

    private String plateFromQrOrText(String decodedText) {
        Long vehicleId = qrCodeService.parseVehicleId(decodedText);
        if (vehicleId != null) {
            return vehicleRepository.findById(vehicleId)
                    .orElseThrow(() -> new com.example.pricing_calculation.service.ResourceNotFoundException("Vehicle not found from QR: " + vehicleId))
                    .getPlateNumber();
        }
        String qrPlate = qrCodeService.parseVehiclePlate(decodedText);
        return qrPlate == null ? decodedText.trim() : qrPlate;
    }
}
