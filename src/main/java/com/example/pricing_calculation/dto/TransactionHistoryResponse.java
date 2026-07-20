package com.example.pricing_calculation.dto;

import com.example.pricing_calculation.domain.Floor;
import com.example.pricing_calculation.domain.PaymentModuleParkingSession;
import com.example.pricing_calculation.domain.PaymentModuleParkingSlot;
import com.example.pricing_calculation.domain.Payment;
import com.example.pricing_calculation.domain.Reservation;
import com.example.pricing_calculation.domain.TransactionHistory;
import com.example.pricing_calculation.domain.TransactionType;
import com.example.pricing_calculation.domain.UserAccount;
import com.example.pricing_calculation.domain.Vehicle;
import com.example.pricing_calculation.domain.VehicleTypeEntity;
import com.example.pricing_calculation.domain.Zone;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

public class TransactionHistoryResponse {

    private final Long id;
    private final Long paymentId;
    private final Long sessionId;
    private final Long vehicleId;
    private final String transactionCode;
    private final String gateway;
    private final TransactionType type;
    private final String status;
    private final String reservationCode;
    private final String parkingTicketCode;
    private final String licensePlate;
    private final String vehicleType;
    private final String floorNumber;
    private final String parkingZone;
    private final String slotNumber;
    private final String customerName;
    private final String customerPhone;
    private final String paymentMethod;
    private final BigDecimal amount;
    private final String currency;
    private final Integer durationMinutes;
    private final String gatewayReference;
    private final LocalDateTime occurredAt;
    private final LocalDateTime sessionStartedAt;
    private final LocalDateTime sessionEndedAt;
    private final String note;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private TransactionHistoryResponse(TransactionHistory transaction) {
        Payment payment = transaction.getPayment();
        PaymentModuleParkingSession session = payment == null ? null : payment.getSession();
        Vehicle vehicle = session == null ? null : session.getVehicle();
        UserAccount user = vehicle == null ? null : vehicle.getUser();
        VehicleTypeEntity vehicleTypeEntity = vehicle == null ? null : vehicle.getVehicleType();
        PaymentModuleParkingSlot slot = session == null ? null : session.getSlot();
        Zone zone = slot == null ? null : slot.getZone();
        Floor floor = zone == null ? null : zone.getFloor();
        Reservation reservation = session == null ? null : session.getReservation();

        id = transaction.getId();
        paymentId = payment == null ? null : payment.getId();
        sessionId = session == null ? null : session.getId();
        vehicleId = vehicle == null ? null : vehicle.getId();
        transactionCode = transaction.getReferenceCode();
        gateway = transaction.getGateway();
        type = deriveType(transaction, payment, session);
        status = transaction.getStatus();
        reservationCode = reservation == null || reservation.getId() == null ? null : String.valueOf(reservation.getId());
        parkingTicketCode = session == null ? null : session.getTicketCode();
        licensePlate = vehicle == null ? null : vehicle.getPlateNumber();
        vehicleType = vehicleTypeEntity == null ? null : vehicleTypeEntity.getName();
        floorNumber = floor == null ? null : displayFloor(floor);
        parkingZone = zone == null ? null : zone.getZoneName();
        slotNumber = slot == null ? null : slot.getSlotCode();
        customerName = user == null ? null : user.getFullName();
        customerPhone = user == null ? null : user.getPhone();
        paymentMethod = payment == null ? null : payment.getPaymentMethod();
        amount = payment == null ? BigDecimal.ZERO : payment.getAmount();
        currency = "VND";
        durationMinutes = calculateDurationMinutes(session);
        gatewayReference = transaction.getReferenceCode();
        occurredAt = payment == null ? null : payment.getPaymentTime();
        sessionStartedAt = session == null ? null : session.getEntryTime();
        sessionEndedAt = session == null ? null : session.getExitTime();
        note = buildNote(payment, session);
        createdAt = payment == null ? null : payment.getPaymentTime();
        updatedAt = null;
    }

    public static TransactionHistoryResponse from(TransactionHistory transaction) {
        return new TransactionHistoryResponse(transaction);
    }

    private TransactionType deriveType(TransactionHistory transaction, Payment payment, PaymentModuleParkingSession session) {
        String transactionStatus = upper(transaction.getStatus());
        String paymentStatus = payment == null ? null : upper(payment.getStatus());
        String sessionStatus = session == null ? null : upper(session.getStatus());
        if ("REFUNDED".equals(transactionStatus)) {
            return TransactionType.REFUND_CREATED;
        }
        if ("COMPLETED".equals(transactionStatus) || "COMPLETED".equals(paymentStatus)
                || "SUCCESS".equals(transactionStatus) || "SUCCESS".equals(paymentStatus)) {
            return TransactionType.PAYMENT_COMPLETED;
        }
        if ("PENDING".equals(transactionStatus) || "PENDING".equals(paymentStatus)) {
            return TransactionType.PAYMENT_PENDING;
        }
        if ("CHECKED_OUT".equals(sessionStatus) || "CLOSED".equals(sessionStatus)) {
            return TransactionType.CHECKOUT_COMPLETED;
        }
        if (session != null && session.getReservation() != null) {
            return TransactionType.RESERVATION_CREATED;
        }
        return TransactionType.FEE_CALCULATED;
    }

    private String displayFloor(Floor floor) {
        if (floor.getFloorName() != null && !floor.getFloorName().isBlank()) {
            return floor.getFloorName();
        }
        return floor.getFloorNumber() == null ? null : String.valueOf(floor.getFloorNumber());
    }

    private Integer calculateDurationMinutes(PaymentModuleParkingSession session) {
        if (session == null || session.getEntryTime() == null || session.getExitTime() == null) {
            return null;
        }
        return Math.toIntExact(Duration.between(session.getEntryTime(), session.getExitTime()).toMinutes());
    }

    private String buildNote(Payment payment, PaymentModuleParkingSession session) {
        if (session == null) {
            return null;
        }
        BigDecimal parkingFee = session.getParkingFee() == null ? BigDecimal.ZERO : session.getParkingFee();
        BigDecimal penaltyFee = session.getPenaltyFee() == null ? BigDecimal.ZERO : session.getPenaltyFee();
        BigDecimal totalFee = session.getTotalFee() == null ? payment == null ? BigDecimal.ZERO : payment.getAmount() : session.getTotalFee();
        return "parkingFee=" + parkingFee + ", penaltyFee=" + penaltyFee + ", totalFee=" + totalFee;
    }

    private String upper(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }

    public Long getId() {
        return id;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public String getTransactionCode() {
        return transactionCode;
    }

    public String getGateway() {
        return gateway;
    }

    public TransactionType getType() {
        return type;
    }

    public String getStatus() {
        return status;
    }

    public String getReservationCode() {
        return reservationCode;
    }

    public String getParkingTicketCode() {
        return parkingTicketCode;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public String getFloorNumber() {
        return floorNumber;
    }

    public String getParkingZone() {
        return parkingZone;
    }

    public String getSlotNumber() {
        return slotNumber;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public String getGatewayReference() {
        return gatewayReference;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public LocalDateTime getSessionStartedAt() {
        return sessionStartedAt;
    }

    public LocalDateTime getSessionEndedAt() {
        return sessionEndedAt;
    }

    public String getNote() {
        return note;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
