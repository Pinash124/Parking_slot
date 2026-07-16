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
import java.time.LocalDate;
import java.time.LocalDateTime;
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
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime startOfTomorrow = startOfToday.plusDays(1);
        BigDecimal sessionRevenue = paymentRepository.sumCompletedAmountBetween(startOfToday, startOfTomorrow);
        BigDecimal monthlyRevenue = monthlyPassRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(pass -> "PAID".equalsIgnoreCase(pass.getPaymentStatus()))
                .filter(pass -> inRange(revenueTime(pass), startOfToday, startOfTomorrow))
                .map(this::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal todayRevenue = safe(sessionRevenue).add(monthlyRevenue);
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
