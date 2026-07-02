package com.example.pricing_calculation.service;

import com.example.pricing_calculation.domain.Payment;
import com.example.pricing_calculation.dto.DashboardOverviewResponse;
import com.example.pricing_calculation.dto.RevenueTrendResponse;
import com.example.pricing_calculation.dto.RevenueTrendResponse.RevenueTrendPoint;
import com.example.pricing_calculation.repository.PaymentModuleParkingSessionRepository;
import com.example.pricing_calculation.repository.PaymentModuleParkingSlotRepository;
import com.example.pricing_calculation.repository.PaymentRepository;
import com.example.pricing_calculation.repository.ReservationRepository;
import com.example.pricing_calculation.repository.TransactionHistoryRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private static final DateTimeFormatter DAILY_LABEL = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter MONTHLY_LABEL = DateTimeFormatter.ofPattern("MM/yyyy");

    private final ReservationRepository reservationRepository;
    private final PaymentModuleParkingSessionRepository parkingSessionRepository;
    private final PaymentModuleParkingSlotRepository parkingSlotRepository;
    private final PaymentRepository paymentRepository;
    private final TransactionHistoryRepository transactionHistoryRepository;

    public DashboardService(
            ReservationRepository reservationRepository,
            PaymentModuleParkingSessionRepository parkingSessionRepository,
            PaymentModuleParkingSlotRepository parkingSlotRepository,
            PaymentRepository paymentRepository,
            TransactionHistoryRepository transactionHistoryRepository) {
        this.reservationRepository = reservationRepository;
        this.parkingSessionRepository = parkingSessionRepository;
        this.parkingSlotRepository = parkingSlotRepository;
        this.paymentRepository = paymentRepository;
        this.transactionHistoryRepository = transactionHistoryRepository;
    }

    @Transactional(readOnly = true)
    public DashboardOverviewResponse overview() {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime startOfTomorrow = startOfToday.plusDays(1);
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime startOfNextMonth = startOfMonth.plusMonths(1);
        BigDecimal todayRevenue = paymentRepository.sumCompletedAmountBetween(startOfToday, startOfTomorrow);
        BigDecimal monthRevenue = paymentRepository.sumCompletedAmountBetween(startOfMonth, startOfNextMonth);
        return new DashboardOverviewResponse(
                reservationRepository.count(),
                reservationRepository.countByStatusIgnoreCase("PENDING"),
                reservationRepository.countByStatusIgnoreCase("APPROVED"),
                parkingSessionRepository.countByStatusIgnoreCase("ACTIVE"),
                parkingSlotRepository.countByStatusIgnoreCase("AVAILABLE"),
                parkingSlotRepository.countByStatusIgnoreCase("OCCUPIED"),
                parkingSlotRepository.countByStatusIgnoreCase("RESERVED"),
                paymentRepository.countByStatusIgnoreCase("PENDING"),
                paymentRepository.countByStatusIgnoreCase("COMPLETED"),
                todayRevenue == null ? BigDecimal.ZERO : todayRevenue,
                monthRevenue == null ? BigDecimal.ZERO : monthRevenue,
                transactionHistoryRepository.count()
        );
    }

    @Transactional(readOnly = true)
    public RevenueTrendResponse dailyRevenue(int days) {
        int safeDays = Math.max(1, Math.min(days, 90));
        LocalDate endDate = LocalDate.now().plusDays(1);
        LocalDate startDate = endDate.minusDays(safeDays);
        return buildDailyRevenue(startDate, endDate);
    }

    @Transactional(readOnly = true)
    public RevenueTrendResponse monthlyRevenue(int months) {
        int safeMonths = Math.max(1, Math.min(months, 12));
        YearMonth endMonth = YearMonth.from(LocalDate.now());
        YearMonth startMonth = endMonth.minusMonths(safeMonths - 1L);
        return buildMonthlyRevenue(startMonth, endMonth);
    }

    private RevenueTrendResponse buildDailyRevenue(LocalDate startDate, LocalDate endExclusive) {
        LocalDateTime from = startDate.atStartOfDay();
        LocalDateTime to = endExclusive.atStartOfDay();
        Map<LocalDate, RevenueBucket> buckets = new HashMap<>();
        for (Payment payment : paymentRepository.findCompletedPaymentsBetween(from, to)) {
            if (payment.getPaymentTime() == null || payment.getAmount() == null) {
                continue;
            }
            LocalDate day = payment.getPaymentTime().toLocalDate();
            buckets.computeIfAbsent(day, ignored -> new RevenueBucket())
                    .add(payment.getAmount());
        }

        List<RevenueTrendPoint> points = new ArrayList<>();
        for (LocalDate day = startDate; day.isBefore(endExclusive); day = day.plusDays(1)) {
            RevenueBucket bucket = buckets.getOrDefault(day, RevenueBucket.EMPTY);
            points.add(new RevenueTrendPoint(
                    day.toString(),
                    day.format(DAILY_LABEL),
                    bucket.revenue(),
                    bucket.payments()));
        }
        return new RevenueTrendResponse("DAILY", startDate, endExclusive.minusDays(1), points);
    }

    private RevenueTrendResponse buildMonthlyRevenue(YearMonth startMonth, YearMonth endMonth) {
        LocalDateTime from = startMonth.atDay(1).atStartOfDay();
        LocalDateTime to = endMonth.plusMonths(1).atDay(1).atStartOfDay();
        Map<YearMonth, RevenueBucket> buckets = new HashMap<>();
        for (Payment payment : paymentRepository.findCompletedPaymentsBetween(from, to)) {
            if (payment.getPaymentTime() == null || payment.getAmount() == null) {
                continue;
            }
            YearMonth month = YearMonth.from(payment.getPaymentTime());
            buckets.computeIfAbsent(month, ignored -> new RevenueBucket())
                    .add(payment.getAmount());
        }

        List<RevenueTrendPoint> points = new ArrayList<>();
        for (YearMonth month = startMonth; !month.isAfter(endMonth); month = month.plusMonths(1)) {
            RevenueBucket bucket = buckets.getOrDefault(month, RevenueBucket.EMPTY);
            points.add(new RevenueTrendPoint(
                    month.toString(),
                    month.format(MONTHLY_LABEL),
                    bucket.revenue(),
                    bucket.payments()));
        }
        return new RevenueTrendResponse("MONTHLY", startMonth.atDay(1), endMonth.atEndOfMonth(), points);
    }

    private static final class RevenueBucket {
        private static final RevenueBucket EMPTY = new RevenueBucket();
        private BigDecimal revenue = BigDecimal.ZERO;
        private long payments;

        private void add(BigDecimal amount) {
            revenue = revenue.add(amount);
            payments++;
        }

        private BigDecimal revenue() {
            return revenue;
        }

        private long payments() {
            return payments;
        }
    }
}
