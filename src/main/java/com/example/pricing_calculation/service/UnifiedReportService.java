package com.example.pricing_calculation.service;

import com.example.pricing_calculation.domain.*;
import com.example.pricing_calculation.dto.ManagerReportResponse;
import com.example.pricing_calculation.dto.ManagerReportResponse.VehicleTypeSummary;
import com.example.pricing_calculation.repository.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UnifiedReportService {
    private final PaymentModuleParkingSessionRepository sessions;
    private final PaymentModuleParkingSlotRepository slots;
    private final PaymentRepository payments;
    private final PaymentModuleVehicleTypeRepository types;
    private final MonthlyParkingPassRepository monthlyPasses;

    public UnifiedReportService(PaymentModuleParkingSessionRepository sessions,
            PaymentModuleParkingSlotRepository slots, PaymentRepository payments,
            PaymentModuleVehicleTypeRepository types,
            MonthlyParkingPassRepository monthlyPasses) {
        this.sessions = sessions;
        this.slots = slots;
        this.payments = payments;
        this.types = types;
        this.monthlyPasses = monthlyPasses;
    }

    @Transactional(readOnly = true)
    public ManagerReportResponse report(LocalDate from, LocalDate to) {
        LocalDateTime start = (from == null ? LocalDate.now().minusDays(30) : from).atStartOfDay();
        LocalDateTime end = (to == null ? LocalDate.now() : to).plusDays(1).atStartOfDay();
        List<PaymentModuleParkingSession> slist = sessions.findAll();
        List<PaymentModuleParkingSession> range = slist.stream().filter(
                s -> s.getEntryTime() != null && !s.getEntryTime().isBefore(start) && s.getEntryTime().isBefore(end))
                .toList();
        long exits = slist.stream().filter(
                s -> s.getExitTime() != null && !s.getExitTime().isBefore(start) && s.getExitTime().isBefore(end))
                .count();
        long current = slist.stream().filter(s -> List.of("ACTIVE", "PAYMENT_PENDING").contains(up(s.getStatus())))
                .count();
        List<PaymentModuleParkingSlot> slotList = slots.findAll();
        Map<Integer, Long> peak = range.stream()
                .collect(Collectors.groupingBy(s -> s.getEntryTime().getHour(), TreeMap::new, Collectors.counting()));
        List<Payment> completedPayments = payments.findAll().stream()
                .filter(p -> isCompletedPayment(p) && inRange(p.getPaymentTime(), start, end))
                .toList();
        List<MonthlyParkingPass> completedMonthlyPasses = monthlyPasses.findAllByOrderByCreatedAtDesc().stream()
                .filter(pass -> "PAID".equalsIgnoreCase(pass.getPaymentStatus()))
                .filter(pass -> inRange(revenueTime(pass), start, end))
                .toList();
        Map<String, BigDecimal> revenueByDay = new LinkedHashMap<>();
        for (LocalDate day = start.toLocalDate(); day.isBefore(end.toLocalDate()); day = day.plusDays(1)) {
            revenueByDay.put(day.toString(), BigDecimal.ZERO);
        }
        completedPayments.forEach(p -> {
            String dayKey = p.getPaymentTime().toLocalDate().toString();
            revenueByDay.merge(dayKey, amount(p), BigDecimal::add);
        });
        completedMonthlyPasses.forEach(pass -> {
            String dayKey = revenueTime(pass).toLocalDate().toString();
            revenueByDay.merge(dayKey, amount(pass), BigDecimal::add);
        });
        List<VehicleTypeSummary> byType = types.findAll().stream().map(t -> {
            long en = range.stream().filter(s -> Objects.equals(vehicleTypeId(s), t.getId())).count();
            long ex = slist.stream()
                    .filter(s -> s.getExitTime() != null && !s.getExitTime().isBefore(start)
                            && s.getExitTime().isBefore(end)
                            && Objects.equals(vehicleTypeId(s), t.getId()))
                    .count();
            long parked = slist.stream().filter(s -> List.of("ACTIVE", "PAYMENT_PENDING").contains(up(s.getStatus()))
                    && Objects.equals(vehicleTypeId(s), t.getId())).count();
            List<PaymentModuleParkingSlot> ts = slotList.stream()
                    .filter(s -> s.getZone() != null && s.getZone().getVehicleType() != null
                            && Objects.equals(s.getZone().getVehicleType().getId(), t.getId()))
                    .toList();
            BigDecimal rev = completedPayments.stream()
                    .filter(p -> Objects.equals(vehicleTypeId(p), t.getId()))
                    .map(this::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal monthlyRev = completedMonthlyPasses.stream()
                    .filter(pass -> Objects.equals(vehicleTypeId(pass), t.getId()))
                    .map(this::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
            return new VehicleTypeSummary(t.getId(), t.getName(), en, ex, parked, ts.size(), count(ts, "AVAILABLE"),
                    rev.add(monthlyRev));
        }).toList();
        long occupied = count(slotList, "OCCUPIED");
        BigDecimal sessionRevenue = Optional.ofNullable(payments.sumCompletedAmountBetween(start, end)).orElse(BigDecimal.ZERO);
        BigDecimal monthlyRevenue = completedMonthlyPasses.stream()
                .map(this::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalRevenue = sessionRevenue.add(monthlyRevenue);
        return new ManagerReportResponse(start, end, range.size(), exits, current,
                totalRevenue, slotList.size(), count(slotList, "AVAILABLE"), occupied,
                count(slotList, "RESERVED"), count(slotList, "MAINTENANCE"), count(slotList, "LOCKED"),
                slotList.isEmpty() ? 0 : occupied * 100.0 / slotList.size(), peak, revenueByDay, byType);
    }

    private long count(List<PaymentModuleParkingSlot> list, String status) {
        return list.stream().filter(s -> status.equalsIgnoreCase(s.getStatus())).count();
    }

    private String up(String s) {
        return s == null ? "" : s.toUpperCase();
    }

    private boolean inRange(LocalDateTime time, LocalDateTime start, LocalDateTime end) {
        return time != null && !time.isBefore(start) && time.isBefore(end);
    }

    private boolean isCompletedPayment(Payment payment) {
        return payment != null && List.of("COMPLETED", "SUCCESS").contains(up(payment.getStatus()));
    }

    private Long vehicleTypeId(PaymentModuleParkingSession session) {
        if (session == null || session.getVehicle() == null || session.getVehicle().getVehicleType() == null) {
            return null;
        }
        return session.getVehicle().getVehicleType().getId();
    }

    private Long vehicleTypeId(Payment payment) {
        return payment == null ? null : vehicleTypeId(payment.getSession());
    }

    private Long vehicleTypeId(MonthlyParkingPass pass) {
        if (pass == null) {
            return null;
        }
        if (pass.getVehicleType() != null) {
            return pass.getVehicleType().getId();
        }
        if (pass.getVehicle() != null && pass.getVehicle().getVehicleType() != null) {
            return pass.getVehicle().getVehicleType().getId();
        }
        return null;
    }

    private BigDecimal amount(Payment payment) {
        return payment == null || payment.getAmount() == null ? BigDecimal.ZERO : payment.getAmount();
    }

    private BigDecimal amount(MonthlyParkingPass pass) {
        return pass == null || pass.getTotalAmount() == null ? BigDecimal.ZERO : pass.getTotalAmount();
    }

    private LocalDateTime revenueTime(MonthlyParkingPass pass) {
        return pass.getPaidAt() != null ? pass.getPaidAt() : pass.getUpdatedAt();
    }
}
