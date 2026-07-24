package com.example.pricing_calculation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.pricing_calculation.domain.MonthlyParkingPass;
import com.example.pricing_calculation.dto.DashboardOverviewResponse;
import com.example.pricing_calculation.repository.MonthlyParkingPassRepository;
import com.example.pricing_calculation.repository.PaymentModuleParkingSessionRepository;
import com.example.pricing_calculation.repository.PaymentModuleParkingSlotRepository;
import com.example.pricing_calculation.repository.PaymentRepository;
import com.example.pricing_calculation.repository.ReservationRepository;
import com.example.pricing_calculation.repository.TransactionHistoryRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class DashboardServiceTest {

    @Test
    void reportsOnlySessionsWhoseParkingSlotsAreStillOccupied() {
        ReservationRepository reservations = mock(ReservationRepository.class);
        PaymentModuleParkingSessionRepository sessions = mock(PaymentModuleParkingSessionRepository.class);
        PaymentModuleParkingSlotRepository slots = mock(PaymentModuleParkingSlotRepository.class);
        PaymentRepository payments = mock(PaymentRepository.class);
        MonthlyParkingPassRepository monthlyPasses = mock(MonthlyParkingPassRepository.class);
        TransactionHistoryRepository transactions = mock(TransactionHistoryRepository.class);
        when(sessions.countCurrentlyParked()).thenReturn(1L);
        when(monthlyPasses.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

        DashboardService service = new DashboardService(
                reservations, sessions, slots, payments, monthlyPasses, transactions);

        assertEquals(1L, service.overview().activeSessions());
    }

    @Test
    void reportsTodayWeekAndSelectedMonthRevenueIncludingPaidMonthlyPasses() {
        ReservationRepository reservations = mock(ReservationRepository.class);
        PaymentModuleParkingSessionRepository sessions = mock(PaymentModuleParkingSessionRepository.class);
        PaymentModuleParkingSlotRepository slots = mock(PaymentModuleParkingSlotRepository.class);
        PaymentRepository payments = mock(PaymentRepository.class);
        MonthlyParkingPassRepository monthlyPasses = mock(MonthlyParkingPassRepository.class);
        TransactionHistoryRepository transactions = mock(TransactionHistoryRepository.class);
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime startOfWeek = LocalDate.now()
                .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                .atStartOfDay();
        LocalDateTime selectedMonthStart = LocalDate.of(2001, 3, 1).atStartOfDay();
        MonthlyParkingPass passToday = mock(MonthlyParkingPass.class);
        MonthlyParkingPass passSelectedMonth = mock(MonthlyParkingPass.class);

        when(payments.sumCompletedAmountBetween(startOfToday, startOfToday.plusDays(1)))
                .thenReturn(new BigDecimal("10000"));
        when(payments.sumCompletedAmountBetween(startOfWeek, startOfWeek.plusWeeks(1)))
                .thenReturn(new BigDecimal("50000"));
        when(payments.sumCompletedAmountBetween(selectedMonthStart, selectedMonthStart.plusMonths(1)))
                .thenReturn(new BigDecimal("100000"));
        when(passToday.getPaymentStatus()).thenReturn("PAID");
        when(passToday.getPaidAt()).thenReturn(startOfToday.plusHours(1));
        when(passToday.getTotalAmount()).thenReturn(new BigDecimal("30000"));
        when(passSelectedMonth.getPaymentStatus()).thenReturn("PAID");
        when(passSelectedMonth.getPaidAt()).thenReturn(selectedMonthStart.plusDays(14));
        when(passSelectedMonth.getTotalAmount()).thenReturn(new BigDecimal("15000"));
        when(monthlyPasses.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(passToday, passSelectedMonth));

        DashboardService service = new DashboardService(
                reservations, sessions, slots, payments, monthlyPasses, transactions);

        DashboardOverviewResponse overview = service.overview(3, 2001);
        assertEquals(new BigDecimal("40000"), overview.todayRevenue());
        assertEquals(new BigDecimal("80000"), overview.weekRevenue());
        assertEquals(new BigDecimal("115000"), overview.monthRevenue());
    }
}
