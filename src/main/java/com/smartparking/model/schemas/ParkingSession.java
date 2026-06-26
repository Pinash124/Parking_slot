package com.smartparking.model.schemas;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "parking_sessions")
@Getter
@Setter
public class ParkingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    private Long sessionId;

    @Column(name = "reservation_id")
    private Long reservationId;

    @Column(name = "vehicle_id")
    private Long vehicleId;

    @Column(name = "slot_id")
    private Long slotId;

    @Column(name = "entry_staff_id")
    private Long entryStaffId;

    @Column(name = "exit_staff_id")
    private Long exitStaffId;

    @Column(name = "entry_gate_id")
    private Long entryGateId;

    @Column(name = "exit_gate_id")
    private Long exitGateId;

    @Column(name = "ticket_code")
    private String ticketCode;

    @Column(name = "entry_time")
    private LocalDateTime entryTime;

    @Column(name = "exit_time")
    private LocalDateTime exitTime;

    @Column(name = "parking_fee")
    private BigDecimal parkingFee;

    @Column(name = "penalty_fee")
    private BigDecimal penaltyFee;

    @Column(name = "total_fee")
    private BigDecimal totalFee;

    @Column(name = "status")
    private String status;
}