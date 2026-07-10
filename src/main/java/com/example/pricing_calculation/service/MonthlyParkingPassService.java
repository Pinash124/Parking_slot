package com.example.pricing_calculation.service;

import com.example.pricing_calculation.domain.MonthlyParkingPass;
import com.example.pricing_calculation.domain.PaymentModuleParkingSlot;
import com.example.pricing_calculation.domain.UserAccount;
import com.example.pricing_calculation.domain.Vehicle;
import com.example.pricing_calculation.dto.MonthlyParkingPassDtos.MonthlyParkingPassCreateRequest;
import com.example.pricing_calculation.dto.MonthlyParkingPassDtos.MonthlyParkingPassPaymentInstructionResponse;
import com.example.pricing_calculation.dto.MonthlyParkingPassDtos.MonthlyParkingPassPaymentRequest;
import com.example.pricing_calculation.dto.MonthlyParkingPassDtos.MonthlyParkingPassResponse;
import com.example.pricing_calculation.repository.MonthlyParkingPassRepository;
import com.example.pricing_calculation.repository.PaymentModuleParkingSlotRepository;
import com.example.pricing_calculation.repository.VehicleRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MonthlyParkingPassService {

    private static final Set<String> BLOCKING_STATUSES = Set.of("PENDING_PAYMENT", "ACTIVE", "SCHEDULED");
    private final MonthlyParkingPassRepository passes;
    private final VehicleRepository vehicles;
    private final PaymentModuleParkingSlotRepository slots;
    private final PricingService pricing;
    private final String personalQrImageUrl;

    public MonthlyParkingPassService(MonthlyParkingPassRepository passes, VehicleRepository vehicles,
            PaymentModuleParkingSlotRepository slots, PricingService pricing,
            @Value("${personal-qr.image-url:/payment/vnpay-personal-qr.png}") String personalQrImageUrl) {
        this.passes = passes;
        this.vehicles = vehicles;
        this.slots = slots;
        this.pricing = pricing;
        this.personalQrImageUrl = personalQrImageUrl;
    }

    @Transactional(readOnly = true)
    public List<MonthlyParkingPassResponse> listForUser(UserAccount user) {
        return passes.findByVehicleUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(MonthlyParkingPassResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<MonthlyParkingPassResponse> listAll() {
        return passes.findAllByOrderByCreatedAtDesc().stream()
                .map(MonthlyParkingPassResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public MonthlyParkingPassResponse getForUser(UserAccount user, Long id) {
        MonthlyParkingPass pass = find(id);
        if (pass.getUser() == null || user == null || !pass.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Monthly pass does not belong to current user");
        }
        return MonthlyParkingPassResponse.from(pass);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public MonthlyParkingPassResponse register(UserAccount user, MonthlyParkingPassCreateRequest request) {
        if (request == null || request.vehicleId() == null || request.slotId() == null) {
            throw new BadRequestException("vehicleId and slotId are required");
        }
        Vehicle vehicle = vehicles.findById(request.vehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + request.vehicleId()));
        if (vehicle.getUser() == null || !vehicle.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Vehicle does not belong to current user");
        }
        if (!VehicleTypeClassifier.isCar(vehicle.getVehicleType())) {
            throw new BadRequestException("Only cars can register a monthly parking slot");
        }
        int months = request.months() == null ? 1 : request.months();
        if (months < 1 || months > 12) {
            throw new BadRequestException("months must be between 1 and 12");
        }
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        LocalDate startDate = request.startDate() == null ? today : request.startDate();
        if (startDate.isBefore(today)) {
            throw new BadRequestException("startDate cannot be in the past");
        }
        LocalDate endDate = startDate.plusMonths(months).minusDays(1);
        ensureVehicleHasNoOverlappingPass(vehicle.getId(), startDate, endDate);

        PaymentModuleParkingSlot slot = slots.findByIdForUpdate(request.slotId())
                .orElseThrow(() -> new ResourceNotFoundException("Parking slot not found: " + request.slotId()));
        if (!"AVAILABLE".equalsIgnoreCase(slot.getStatus())) {
            throw new BadRequestException("Selected monthly parking slot is not available");
        }
        if (slot.getZone() == null || slot.getZone().getVehicleType() == null
                || !slot.getZone().getVehicleType().getId().equals(vehicle.getVehicleType().getId())) {
            throw new BadRequestException("Selected slot does not support this vehicle type");
        }
        if (!"CAR_MONTHLY".equalsIgnoreCase(slot.getZone().getZoneType())) {
            throw new BadRequestException("Monthly passes must select a slot in the CAR_MONTHLY zone");
        }
        ensureSlotHasNoOverlappingPass(slot.getId(), startDate, endDate);

        BigDecimal monthlyRate = pricing.monthlyRateForVehicleType(vehicle.getVehicleType().getId(), startDate.atStartOfDay());
        if (monthlyRate == null || monthlyRate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Monthly rate must be configured before selling a monthly pass");
        }
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        MonthlyParkingPass pass = new MonthlyParkingPass();
        pass.setUser(user);
        pass.setVehicle(vehicle);
        pass.setVehicleType(vehicle.getVehicleType());
        pass.setReservedSlot(slot);
        pass.setMonths(months);
        pass.setMonthlyRate(monthlyRate);
        pass.setTotalAmount(monthlyRate.multiply(BigDecimal.valueOf(months)).setScale(2, RoundingMode.HALF_UP));
        pass.setStartDate(startDate);
        pass.setEndDate(endDate);
        pass.setStatus("PENDING_PAYMENT");
        pass.setPaymentStatus("PENDING");
        pass.setNote(request.note());
        pass.setCreatedAt(now);
        pass.setUpdatedAt(now);
        slot.setStatus("MONTHLY_HELD");
        slots.save(slot);
        return MonthlyParkingPassResponse.from(passes.save(pass));
    }

    @Transactional
    public MonthlyParkingPassPaymentInstructionResponse prepareOnlinePayment(UserAccount user, Long id) {
        MonthlyParkingPass pass = findOwnedPendingPass(user, id);
        String referenceCode = buildReferenceCode("MTHQR", pass.getId());
        pass.setPaymentMethod("ONLINE_QR");
        pass.setPaymentReference(referenceCode);
        pass.setUpdatedAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
        MonthlyParkingPass saved = passes.save(pass);
        String qrContent = buildQrContent(saved, referenceCode);
        return new MonthlyParkingPassPaymentInstructionResponse(
                MonthlyParkingPassResponse.from(saved),
                "ONLINE_QR",
                referenceCode,
                saved.getTotalAmount(),
                qrContent,
                null,
                personalQrImageUrl,
                referenceCode,
                saved.getUpdatedAt()
        );
    }

    @Transactional
    public MonthlyParkingPassResponse prepareVnpayPayment(UserAccount user, Long id, String referenceCode) {
        if (referenceCode == null || referenceCode.isBlank()) {
            throw new BadRequestException("referenceCode is required");
        }
        MonthlyParkingPass pass = findOwnedPendingPass(user, id);
        pass.setPaymentMethod("VNPAY");
        pass.setPaymentReference(referenceCode.trim());
        pass.setUpdatedAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
        return MonthlyParkingPassResponse.from(passes.save(pass));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public MonthlyParkingPassResponse confirmPayment(Long id, MonthlyParkingPassPaymentRequest request) {
        MonthlyParkingPass pass = find(id);
        String reference = request == null || request.referenceCode() == null || request.referenceCode().isBlank()
                ? pass.getPaymentReference()
                : request.referenceCode();
        return completePayment(pass, reference, "ONLINE_QR");
    }

    @Transactional(readOnly = true)
    public BigDecimal amountByPaymentReference(String referenceCode) {
        return findByPaymentReference(referenceCode).getTotalAmount();
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public MonthlyParkingPassResponse confirmVnpayPayment(String referenceCode) {
        MonthlyParkingPass pass = findByPaymentReference(referenceCode);
        return completePayment(pass, referenceCode, "VNPAY");
    }

    private MonthlyParkingPassResponse completePayment(MonthlyParkingPass pass, String reference, String paymentMethod) {
        if ("PAID".equalsIgnoreCase(pass.getPaymentStatus())) {
            return MonthlyParkingPassResponse.from(pass);
        }
        if (!"PENDING_PAYMENT".equalsIgnoreCase(pass.getStatus())) {
            throw new BadRequestException("Only a pending monthly pass can be paid");
        }
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        pass.setPaymentStatus("PAID");
        pass.setPaymentMethod(paymentMethod);
        pass.setPaymentReference(reference);
        pass.setPaidAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
        pass.setStatus(pass.getStartDate().isAfter(today) ? "SCHEDULED" : "ACTIVE");
        pass.setUpdatedAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
        PaymentModuleParkingSlot slot = slots.findByIdForUpdate(pass.getReservedSlot().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Monthly parking slot not found"));
        if (!"MONTHLY_HELD".equalsIgnoreCase(slot.getStatus())) {
            throw new BadRequestException("Monthly parking slot is no longer held for this pass");
        }
        slot.setStatus("MONTHLY_RESERVED");
        slots.save(slot);
        return MonthlyParkingPassResponse.from(passes.save(pass));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public MonthlyParkingPassResponse confirmPaymentFromQr(String qrContent, String referenceCode) {
        Long id = parsePassId(qrContent);
        MonthlyParkingPass pass = find(id);
        String reference = referenceCode == null || referenceCode.isBlank()
                ? pass.getPaymentReference()
                : referenceCode;
        return confirmPayment(id, new MonthlyParkingPassPaymentRequest(reference));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public MonthlyParkingPassResponse cancel(Long id) {
        MonthlyParkingPass pass = find(id);
        if ("CANCELLED".equalsIgnoreCase(pass.getStatus())) {
            return MonthlyParkingPassResponse.from(pass);
        }
        PaymentModuleParkingSlot slot = slots.findByIdForUpdate(pass.getReservedSlot().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Monthly parking slot not found"));
        if ("MONTHLY_OCCUPIED".equalsIgnoreCase(slot.getStatus())) {
            throw new BadRequestException("Cannot cancel a monthly pass while its vehicle is parked");
        }
        if ("MONTHLY_HELD".equalsIgnoreCase(slot.getStatus()) || "MONTHLY_RESERVED".equalsIgnoreCase(slot.getStatus())) {
            slot.setStatus("AVAILABLE");
            slots.save(slot);
        }
        pass.setStatus("CANCELLED");
        if (!"PAID".equalsIgnoreCase(pass.getPaymentStatus())) {
            pass.setPaymentStatus("CANCELLED");
        }
        pass.setUpdatedAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
        return MonthlyParkingPassResponse.from(passes.save(pass));
    }

    private MonthlyParkingPass find(Long id) {
        return passes.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Monthly parking pass not found: " + id));
    }

    private MonthlyParkingPass findByPaymentReference(String referenceCode) {
        if (referenceCode == null || referenceCode.isBlank()) {
            throw new BadRequestException("referenceCode is required");
        }
        return passes.findByPaymentReferenceIgnoreCase(referenceCode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Monthly parking pass not found for reference: " + referenceCode));
    }

    private MonthlyParkingPass findOwnedPendingPass(UserAccount user, Long id) {
        MonthlyParkingPass pass = find(id);
        if (pass.getUser() == null || user == null || !pass.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Monthly pass does not belong to current user");
        }
        if (!"PENDING_PAYMENT".equalsIgnoreCase(pass.getStatus()) || !"PENDING".equalsIgnoreCase(pass.getPaymentStatus())) {
            throw new BadRequestException("Only pending monthly passes can generate payment instructions");
        }
        return pass;
    }

    private String buildReferenceCode(String prefix, Long passId) {
        return prefix + "-" + passId + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String buildQrContent(MonthlyParkingPass pass, String referenceCode) {
        return "MONTHLY_PASS"
                + "|passId=" + pass.getId()
                + "|ref=" + referenceCode
                + "|method=ONLINE_QR"
                + "|amount=" + safeAmount(pass.getTotalAmount())
                + "|plate=" + text(pass.getVehicle() == null ? null : pass.getVehicle().getPlateNumber())
                + "|slot=" + text(pass.getReservedSlot() == null ? null : pass.getReservedSlot().getSlotCode());
    }

    private Long parsePassId(String qrContent) {
        if (qrContent == null || qrContent.isBlank()) {
            throw new BadRequestException("qrContent is required");
        }
        for (String part : qrContent.split("\\|")) {
            if (part.startsWith("passId=")) {
                try {
                    return Long.parseLong(part.substring("passId=".length()));
                } catch (NumberFormatException ex) {
                    throw new BadRequestException("Invalid monthly pass QR content");
                }
            }
        }
        throw new BadRequestException("Monthly pass QR content must include passId");
    }

    private String safeAmount(BigDecimal value) {
        return value == null ? "0.00" : value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String text(String value) {
        return value == null || value.isBlank() ? "N/A" : value.trim();
    }

    private void ensureVehicleHasNoOverlappingPass(Long vehicleId, LocalDate start, LocalDate end) {
        boolean overlap = passes.findByVehicleIdOrderByCreatedAtDesc(vehicleId).stream()
                .anyMatch(pass -> blocks(pass) && overlaps(pass, start, end));
        if (overlap) throw new BadRequestException("Vehicle already has an overlapping monthly pass");
    }

    private void ensureSlotHasNoOverlappingPass(Long slotId, LocalDate start, LocalDate end) {
        boolean overlap = passes.findAll().stream()
                .anyMatch(pass -> pass.getReservedSlot() != null && slotId.equals(pass.getReservedSlot().getId())
                        && blocks(pass) && overlaps(pass, start, end));
        if (overlap) throw new BadRequestException("Selected slot is already assigned to another monthly pass");
    }

    private boolean blocks(MonthlyParkingPass pass) {
        return pass.getStatus() != null && BLOCKING_STATUSES.contains(pass.getStatus().toUpperCase());
    }

    private boolean overlaps(MonthlyParkingPass pass, LocalDate start, LocalDate end) {
        return pass.getStartDate() != null && pass.getEndDate() != null
                && !pass.getStartDate().isAfter(end) && !pass.getEndDate().isBefore(start);
    }
}
