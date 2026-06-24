package com.example.pricing_calculation.service;

import com.example.pricing_calculation.dto.DashboardOverviewResponse;
import com.example.pricing_calculation.repository.ParkingSessionRepository;
import com.example.pricing_calculation.repository.ParkingSlotRepository;
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
    private final ParkingSessionRepository parkingSessionRepository;
    private final ParkingSlotRepository parkingSlotRepository;
    private final PaymentRepository paymentRepository;
    private final TransactionHistoryRepository transactionHistoryRepository;

    public DashboardService(
            ReservationRepository reservationRepository,
            ParkingSessionRepository parkingSessionRepository,
            ParkingSlotRepository parkingSlotRepository,
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
        BigDecimal todayRevenue = paymentRepository.sumCompletedAmountBetween(startOfToday, startOfTomorrow);
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
}
