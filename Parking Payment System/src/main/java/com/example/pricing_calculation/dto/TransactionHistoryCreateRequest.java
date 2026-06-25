package com.example.pricing_calculation.dto;

import com.example.pricing_calculation.domain.PaymentMethod;
import com.example.pricing_calculation.domain.TransactionStatus;
import com.example.pricing_calculation.domain.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionHistoryCreateRequest {

    private Long paymentId;
    private String gateway;
    private String transactionCode;
    private TransactionType type;
    private TransactionStatus status;
    private String reservationCode;
    private String parkingTicketCode;
    private String licensePlate;
    private String vehicleType;
    private String floorNumber;
    private String parkingZone;
    private String slotNumber;
    private String customerName;
    private String customerPhone;
    private PaymentMethod paymentMethod;
    private BigDecimal amount;
    private String currency;
    private Integer durationMinutes;
    private String gatewayReference;
    private LocalDateTime occurredAt;
    private LocalDateTime sessionStartedAt;
    private LocalDateTime sessionEndedAt;
    private String note;

    public String getTransactionCode() {
        return transactionCode;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public void setTransactionCode(String transactionCode) {
        this.transactionCode = transactionCode;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public String getReservationCode() {
        return reservationCode;
    }

    public void setReservationCode(String reservationCode) {
        this.reservationCode = reservationCode;
    }

    public String getParkingTicketCode() {
        return parkingTicketCode;
    }

    public void setParkingTicketCode(String parkingTicketCode) {
        this.parkingTicketCode = parkingTicketCode;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public String getFloorNumber() {
        return floorNumber;
    }

    public void setFloorNumber(String floorNumber) {
        this.floorNumber = floorNumber;
    }

    public String getParkingZone() {
        return parkingZone;
    }

    public void setParkingZone(String parkingZone) {
        this.parkingZone = parkingZone;
    }

    public String getSlotNumber() {
        return slotNumber;
    }

    public void setSlotNumber(String slotNumber) {
        this.slotNumber = slotNumber;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getGatewayReference() {
        return gatewayReference;
    }

    public void setGatewayReference(String gatewayReference) {
        this.gatewayReference = gatewayReference;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }

    public LocalDateTime getSessionStartedAt() {
        return sessionStartedAt;
    }

    public void setSessionStartedAt(LocalDateTime sessionStartedAt) {
        this.sessionStartedAt = sessionStartedAt;
    }

    public LocalDateTime getSessionEndedAt() {
        return sessionEndedAt;
    }

    public void setSessionEndedAt(LocalDateTime sessionEndedAt) {
        this.sessionEndedAt = sessionEndedAt;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
