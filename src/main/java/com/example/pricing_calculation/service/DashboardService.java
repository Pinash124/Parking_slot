package com.example.pricing_calculation.service;

import com.example.pricing_calculation.dto.DashboardOverviewResponse;
import com.example.pricing_calculation.domain.MonthlyParkingPass;
import com.example.pricing_calculation.repository.MonthlyParkingPassRepository;
import com.example.pricing_calculation.repository.PaymentModuleParkingSessionRepository;
import com.example.pricing_calculation.repository.PaymentModuleParkingSlotRepository;
import com.example.pricing_calculation.repository.PaymentRepository;
import com.example.pricing_calculation.repository.ReservationRepository;
import com.example.pricing_calculation.repository.TransactionHistoryRepository;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.time.temporal.TemporalAdjusters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final ReservationRepository reservationRepository;
    private final PaymentModuleParkingSessionRepository parkingSessionRepository;
    private final PaymentModuleParkingSlotRepository parkingSlotRepository;
    private final PaymentRepository paymentRepository;
    private final MonthlyParkingPassRepository monthlyPassRepository;
    private final TransactionHistoryRepository transactionHistoryRepository;

    public DashboardService(
            ReservationRepository reservationRepository,
            PaymentModuleParkingSessionRepository parkingSessionRepository,
            PaymentModuleParkingSlotRepository parkingSlotRepository,
            PaymentRepository paymentRepository,
            MonthlyParkingPassRepository monthlyPassRepository,
            TransactionHistoryRepository transactionHistoryRepository) {
        this.reservationRepository = reservationRepository;
        this.parkingSessionRepository = parkingSessionRepository;
        this.parkingSlotRepository = parkingSlotRepository;
        this.paymentRepository = paymentRepository;
        this.monthlyPassRepository = monthlyPassRepository;
        this.transactionHistoryRepository = transactionHistoryRepository;
    }

    @Transactional(readOnly = true)
    public DashboardOverviewResponse overview() {
        return overview(null, null);
    }

    @Transactional(readOnly = true)
    public DashboardOverviewResponse overview(Integer month, Integer year) {
        LocalDate today = LocalDate.now();
        int selectedMonth = month == null ? today.getMonthValue() : month;
        int selectedYear = year == null ? today.getYear() : year;
        if (selectedMonth < 1 || selectedMonth > 12) {
            throw new IllegalArgumentException("Month must be between 1 and 12");
        }

        LocalDateTime startOfToday = today.atStartOfDay();
        LocalDateTime startOfTomorrow = startOfToday.plusDays(1);
        LocalDateTime startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
        LocalDateTime startOfNextWeek = startOfWeek.plusWeeks(1);
        LocalDateTime startOfMonth = LocalDate.of(selectedYear, selectedMonth, 1).atStartOfDay();
        LocalDateTime startOfNextMonth = startOfMonth.plusMonths(1);
        BigDecimal sessionRevenueToday = paymentRepository.sumCompletedAmountBetween(startOfToday, startOfTomorrow);
        BigDecimal sessionRevenueWeek = paymentRepository.sumCompletedAmountBetween(startOfWeek, startOfNextWeek);
        BigDecimal sessionRevenueMonth = paymentRepository.sumCompletedAmountBetween(startOfMonth, startOfNextMonth);
        List<MonthlyParkingPass> paidMonthlyPasses = monthlyPassRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(pass -> "PAID".equalsIgnoreCase(pass.getPaymentStatus()))
                .toList();
        BigDecimal monthlyRevenueToday = paidMonthlyPasses.stream()
                .filter(pass -> inRange(revenueTime(pass), startOfToday, startOfTomorrow))
                .map(this::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal monthlyRevenueWeek = paidMonthlyPasses.stream()
                .filter(pass -> inRange(revenueTime(pass), startOfWeek, startOfNextWeek))
                .map(this::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal monthlyRevenueMonth = paidMonthlyPasses.stream()
                .filter(pass -> inRange(revenueTime(pass), startOfMonth, startOfNextMonth))
                .map(this::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal todayRevenue = safe(sessionRevenueToday).add(monthlyRevenueToday);
        BigDecimal weekRevenue = safe(sessionRevenueWeek).add(monthlyRevenueWeek);
        BigDecimal monthRevenue = safe(sessionRevenueMonth).add(monthlyRevenueMonth);
        return new DashboardOverviewResponse(
                reservationRepository.count(),
                reservationRepository.countByStatusIgnoreCase("PENDING"),
                reservationRepository.countByStatusIgnoreCase("APPROVED"),
                parkingSessionRepository.countCurrentlyParked(),
                parkingSlotRepository.countByStatusIgnoreCase("AVAILABLE"),
                parkingSlotRepository.countByStatusIgnoreCase("OCCUPIED"),
                parkingSlotRepository.countByStatusIgnoreCase("RESERVED"),
                parkingSlotRepository.countByStatusIgnoreCase("MONTHLY_HELD"),
                parkingSlotRepository.countByStatusIgnoreCase("MONTHLY_RESERVED"),
                parkingSlotRepository.countByStatusIgnoreCase("MONTHLY_OCCUPIED"),
                paymentRepository.countByStatusIgnoreCase("PENDING"),
                paymentRepository.countByStatusIgnoreCase("COMPLETED"),
                todayRevenue,
                weekRevenue,
                monthRevenue,
                transactionHistoryRepository.count()
        );
    }

    private boolean inRange(LocalDateTime value, LocalDateTime from, LocalDateTime to) {
        return value != null && !value.isBefore(from) && value.isBefore(to);
    }

    private LocalDateTime revenueTime(MonthlyParkingPass pass) {
        return pass.getPaidAt() != null ? pass.getPaidAt() : pass.getUpdatedAt();
    }

    private BigDecimal amount(MonthlyParkingPass pass) {
        return pass.getTotalAmount() == null ? BigDecimal.ZERO : pass.getTotalAmount();
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
